package com.batit.phototranslator.ui.main

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import fr.opensagres.poi.xwpf.converter.core.FileURIResolver
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<Language>
    private lateinit var secondaryLanguages: MutableList<Language>

    private lateinit var snackBar: Snackbar

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
                Language("en")
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
                kotlin.runCatching {
                    val data = result.data!!.data!!
                    var parsedText: String = ""
                    val path = FileUtils.getPath(requireContext(), data)
                    val file = File(path)
                    when (data.getMimeType(requireContext())) {
                        "pdf" -> {
                            PDFBoxResourceLoader.init(requireContext())
                            val inputStream: InputStream =
                                requireContext().contentResolver.openInputStream(data)!!

                            val document = PDDocument.load(inputStream)
                            try {
                                val pdfStripper = PDFTextStripper()
                                pdfStripper.startPage = 0
                                pdfStripper.endPage = document.numberOfPages
                                parsedText = pdfStripper.getText(document)

                            } catch (e: IOException) {
                                e.printStackTrace()
                            } finally {
                                try {
                                    document?.close()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }

                        }
                        "txt" -> {
                            val reader = FileReader(file.path)
                            parsedText = reader.readText()
                        }
                        "doc", "docx" -> {
                            val `in`: InputStream = FileInputStream(File(path))
                            val document = XWPFDocument(`in`)
                            val options = XHTMLOptions.create()
                                .URIResolver(FileURIResolver(File("word/media")))

                            val out: OutputStream = ByteArrayOutputStream()
                            XHTMLConverter.getInstance().convert(document, out, options)
                            val html = out.toString()
                            println(html)
                        }
                        else -> {}
                    }
                    Log.e("logs", parsedText)
                    if (parsedText.isNotBlank()) {
                        findNavController().navigate(
                            MainFragmentDirections.actionHomeToTranslateTextFragment(
                                parsedText
                            )
                        )
                    }
                }.exceptionOrNull()?.printStackTrace()

            }
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
//        builderSingle.setIcon(R.drawable.ic_launcher)
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
}