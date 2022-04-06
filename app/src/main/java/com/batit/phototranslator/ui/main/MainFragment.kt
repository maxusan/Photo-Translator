package com.batit.phototranslator.ui.main

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.batit.phototranslator.R
import com.batit.phototranslator.core.FileUtils
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.data.LanguageProvider
import com.batit.phototranslator.core.db.PhotoItem
import com.batit.phototranslator.core.util.checkPermissions
import com.batit.phototranslator.core.util.getMimeType
import com.batit.phototranslator.databinding.FragmentMainBinding
import com.batit.phototranslator.ui.MainViewModel
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<Language>
    private lateinit var secondaryLanguages: MutableList<Language>

    private lateinit var snackBar: Snackbar
    private var readingSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        secondaryLanguages = viewModel.getLanguages().toMutableList()
        secondarySpinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item, secondaryLanguages
        )
        secondarySpinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.appBarMain.secondarySpinner.adapter = secondarySpinnerAdapter
        binding.appBarMain.secondarySpinner.setSelection(
            secondarySpinnerAdapter.getPosition(
                requireActivity().intent.getSerializableExtra("second") as Language
            )
        )
        binding.appBarMain.secondarySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.setSecondaryLanguage(secondaryLanguages[p2])
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

            }
        binding.appBarMain.toolbar.setNavigationOnClickListener {
            viewModel.openDrawer()
        }
        binding.translateText.setOnClickListener {
            findNavController().navigate(
                MainFragmentDirections.actionHomeToTranslateTextFragment(
                    null
                )
            )
        }
        binding.record.setOnClickListener {
            requireContext().checkPermissions(android.Manifest.permission.RECORD_AUDIO) {
                if (it) {
                    startSpeechRecognition()
                }
            }
        }
        binding.document.setOnClickListener {
            pickDocument()
        }
        binding.capture.setOnClickListener {
            ImagePicker.with(this)
                .cameraOnly()
                .crop()
                .createIntent {
                    startForProfileImageResult.launch(it)
                }
        }

        binding.importButton.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()
                .crop()
                .createIntent {
                    startForProfileImageResult.launch(it)
                }
        }
    }


    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val fileUri = data?.data!!
                    saveImage(fileUri)
                    kotlin.runCatching {
                        viewModel.setPrimaryLanguage(Language.getDefaultLanguage())
                        findNavController().navigate(
                            MainFragmentDirections.actionHomeToTranslateFragment2(
                                fileUri
                            )
                        )
                    }
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(requireContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT)
                        .show()
                }
                else -> {
                    Toast.makeText(requireContext(), "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun saveImage(fileUri: Uri) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm")
        val date = dateFormat.format(Calendar.getInstance().time);
        val filename: String = fileUri.toString().substring(fileUri.toString().lastIndexOf("/") + 1)
        viewModel.insertPhoto(
            PhotoItem(
                id = UUID.randomUUID().toString(),
                photoName = filename,
                photoUri = fileUri.toString(),
                dateAdded = date
            )
        )
    }

    private val startForDocumentResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            if (resultCode == Activity.RESULT_OK) {
                readingSnackbar =
                    Snackbar.make(requireView(), "Document reading...", Snackbar.LENGTH_INDEFINITE)
                kotlin.runCatching {
                    readingSnackbar?.show()
                    lifecycleScope.launch(Dispatchers.IO) {
                        val data = result.data!!.data!!
                        var parsedText: String = ""
                        val path = FileUtils.getPath(requireContext(), data)
                        val file = File(path)
                        when (data.getMimeType(requireContext())) {
                            "pdf" -> {
                                parsedText = showPagesDialog(data, parsedText, file)
                            }
                            "txt" -> {
                                val reader = FileReader(file.path)
                                parsedText = reader.readText()
                            }
                            else -> {}
                        }
                        if (parsedText.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                kotlin.runCatching {
                                    viewModel.setPrimaryLanguage(Language.getDefaultLanguage())
                                    if (parsedText.length > 1500) {
                                        val builder = AlertDialog.Builder(requireContext())
                                        builder.setMessage("Warning. The number of characters in this document is more than 1500. Translation and analysis of the text may take longer than usual.")
                                            .setCancelable(true)
                                            .setPositiveButton("ok") { dialog, id ->
                                                dialog.dismiss()
                                                findNavController().navigate(
                                                    MainFragmentDirections.actionHomeToTranslateTextFragment(
                                                        parsedText
                                                    )
                                                )
                                            }.setNegativeButton("Cancel") { dialog, id ->
                                                dialog.dismiss()
                                            }
                                        val alert = builder.create()
                                        alert.show()
                                    } else {
                                        findNavController().navigate(
                                            MainFragmentDirections.actionHomeToTranslateTextFragment(
                                                parsedText
                                            )
                                        )
                                    }
                                }.exceptionOrNull()?.printStackTrace()

                            }
                        }
                        withContext(Dispatchers.Main) {
                            readingSnackbar?.dismiss()
                        }
                    }
                }.exceptionOrNull()?.let {
                    it.printStackTrace()
                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    private suspend fun showPagesDialog(
        data: Uri,
        parsedText: String,
        file: File
    ): String {
        var parsedText1 = parsedText
        PDFBoxResourceLoader.init(requireContext())
        val inputStream: InputStream =
            requireContext().contentResolver.openInputStream(data)!!

        val document = PDDocument.load(inputStream)
        val builderSingle: AlertDialog.Builder =
            AlertDialog.Builder(requireContext())

        builderSingle.setCancelable(false)
        builderSingle.setTitle("Select page")

        val pagesList = mutableListOf<String>()
        for (i in 0..document.numberOfPages) {
            val temp = i + 1
            pagesList.add("Page $temp")
        }

        val arrayAdapter =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.select_dialog_singlechoice
            )
        arrayAdapter.addAll(pagesList)

        builderSingle.setNegativeButton("cancel",
            DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
        builderSingle.setAdapter(arrayAdapter,
            DialogInterface.OnClickListener { dialog, which ->
                try {
                    val pdfStripper = PDFTextStripper()
                    pdfStripper.startPage = which
                    pdfStripper.endPage = which
                    parsedText1 = pdfStripper.getText(document)
                    showTextPreview(pdfToBitmap(file, which)!!) {
                        if (it) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                findNavController().navigate(
                                    MainFragmentDirections.actionHomeToTranslateTextFragment(
                                        parsedText1
                                    )
                                )
                            }
                        }
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        document?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            })
        withContext(Dispatchers.Main) {
            builderSingle.show()
        }
        return parsedText1
    }


    private fun pickDocument() {
        requireContext().checkPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) {
            if (it) {
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        try {
                            val intent =
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.addCategory("android.intent.category.DEFAULT")
                            intent.data =
                                Uri.parse(String.format("package:%s", requireContext().packageName))
                            startActivityForResult(intent, 2296)
                        } catch (e: Exception) {
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            startActivityForResult(intent, 2296)
                        }
                    } else {
                        startPdfLauncher()
                    }
                } else {
                    requireContext().checkPermissions(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) {
                        if (it) {
                            startPdfLauncher()
                        }
                    }
                }
            }
        }


    }

    private fun startPdfLauncher() {
        val mimeTypes = arrayOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/pdf",
        )
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startForDocumentResult.launch(intent)

    }

    fun getErrorText(errorCode: Int): String? {
        val message: String = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        return message
    }

    private fun startSpeechRecognition() {
        val builderSingle: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builderSingle.setTitle("Select language")

        val arrayAdapter =
            ArrayAdapter<Language>(requireContext(), android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(LanguageProvider.getLanguages())

        builderSingle.setNegativeButton("cancel",
            DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })

        builderSingle.setAdapter(arrayAdapter,
            DialogInterface.OnClickListener { dialog, which ->
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                viewModel.setPrimaryLanguage(arrayAdapter.getItem(which)!!)

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, arrayAdapter.getItem(which)!!.code)
                snackBar = Snackbar.make(
                    binding.root,
                    "Listening: ${arrayAdapter.getItem(which)!!.displayName}",
                    Snackbar.LENGTH_INDEFINITE
                )

                intent.putExtra(
                    RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    requireContext().packageName
                )

                val recognizer = SpeechRecognizer
                    .createSpeechRecognizer(requireContext())

                val listener: RecognitionListener = object : RecognitionListener {
                    override fun onResults(results: Bundle) {
                        snackBar.dismiss()
                        val voiceResults = results
                            .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (voiceResults == null) {
                            println("No voice results")
                        } else {
                            var text: String = ""
                            voiceResults.forEach {
                                text += it + "\n"
                            }

                            kotlin.runCatching {
                                findNavController().navigate(
                                    MainFragmentDirections.actionHomeToTranslateTextFragment(
                                        text
                                    )
                                )
                            }
                        }
                    }

                    override fun onReadyForSpeech(params: Bundle) {
                        println("Ready for speech")
                        snackBar.show()
                    }

                    override fun onError(error: Int) {
                        kotlin.runCatching {
                            snackBar.dismiss()
                            Toast.makeText(
                                requireContext(),
                                "Error listening for speech: ${getErrorText(error)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.exceptionOrNull()?.printStackTrace()

                    }

                    override fun onBeginningOfSpeech() {
//                Toast.makeText(requireContext(), "Listening", Toast.LENGTH_SHORT).show()

                    }

                    override fun onBufferReceived(buffer: ByteArray) {
                    }

                    override fun onEndOfSpeech() {
                    }

                    override fun onEvent(eventType: Int, params: Bundle) {
                    }

                    override fun onPartialResults(partialResults: Bundle) {
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                    }
                }
                recognizer.setRecognitionListener(listener)
                recognizer.startListening(intent)
            })

        builderSingle.show()

    }

    private fun pdfToBitmap(pdfFile: File, pageNum: Int): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val renderer =
                PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
            val pageCount = renderer.pageCount
            if (pageCount > 0) {
                val page = renderer.openPage(pageNum)
                bitmap = Bitmap.createBitmap(getResources().getDisplayMetrics().densityDpi * page.getWidth() / 72,
                    getResources().getDisplayMetrics().densityDpi * page.getHeight() / 72,
                    Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()
                renderer.close()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return bitmap
    }

    private fun showTextPreview(bitmap: Bitmap, callback: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setPositiveButton(
            "Translate"
        ) { dialog, which ->callback(true) }.setNegativeButton(
            "Cancel"
        ) { dialog, which -> callback(false)}
        val dialog = builder.create()
        val inflater = layoutInflater
        val dialogLayout: View = inflater.inflate(R.layout.pdf_preview_layout, null)
        dialog.setView(dialogLayout)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.show()
        dialog.findViewById<ImageView>(R.id.pdf_preview)?.setImageBitmap(bitmap)
    }

    override fun onPause() {
        super.onPause()
        readingSnackbar?.dismiss()
    }
}