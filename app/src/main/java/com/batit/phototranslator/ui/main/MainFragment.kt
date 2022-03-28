package com.batit.phototranslator.ui.main

import android.app.Activity
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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.callback.FilePickerCallback
import com.batit.phototranslator.R
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.util.checkPermissions
import com.batit.phototranslator.core.util.getMimeType
import com.batit.phototranslator.core.util.getRealPathFromURI
import com.batit.phototranslator.databinding.FragmentMainBinding
import com.batit.phototranslator.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import fr.opensagres.poi.xwpf.converter.core.FileURIResolver
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.*


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<Language>
    private lateinit var secondaryLanguages: MutableList<Language>
    private val storage = SimpleStorage(this)
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
    }

    private val startForDocumentResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            if (resultCode == Activity.RESULT_OK) {
                kotlin.runCatching {
                    val data = result.data!!.data!!
                    var parsedText: String = ""
                    val path = requireContext().getRealPathFromURI(data)
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
//                            val reader = FileReader(file.path)
//                            parsedText = reader.readText()
                        }
                        "doc", "docx" -> {

                            val file = File(path)
                            val instr: InputStream = FileInputStream(file.path)
                            val document = XWPFDocument(instr)
                            val options: XHTMLOptions = XHTMLOptions.create()
                                .URIResolver(FileURIResolver(file))

                            val out: OutputStream = ByteArrayOutputStream()


                            XHTMLConverter.getInstance().convert(document, out, options)
                            parsedText = out.toString()
//                            println(html)
                        }
                        else -> {}
                    }
                    Log.e("logs", parsedText)

                }.exceptionOrNull()?.printStackTrace()
            }
        }


    private fun pickDocument() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
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
//                FilePickerBuilder.instance.setMaxCount(1).pickFile(this)
                startPdfLauncher()
            }
        } else {
            requireContext().checkPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) {
                if (it) {
//                    FilePickerBuilder.instance.setMaxCount(1).pickFile(this)
                    startPdfLauncher()
                }
            }
        }

    }
//    private val activityResultLauncher =
//        registerForActivityResult(
//            ActivityResultContracts.RequestPermission()){isGranted ->
//            // Handle Permission granted/rejected
//            if (isGranted) {
//                startPdfLauncher()
//            }
//        }

    private fun startPdfLauncher() {
        val mimeTypes = arrayOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  // .doc & .docx
//            "application/vnd.ms-powerpoint",
//            "application/vnd.openxmlformats-officedocument.presentationml.presentation",  // .ppt & .pptx
//            "application/vnd.ms-excel",
//            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",  // .xls & .xlsx
            "text/plain",
            "application/pdf",
//            "application/zip"
        )

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        startForDocumentResult.launch(intent)

    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_CALLING_PACKAGE,
            requireContext().packageName
        )

        val recognizer = SpeechRecognizer
            .createSpeechRecognizer(requireContext())
        val snackBar = Snackbar.make(binding.root, "Listening", Snackbar.LENGTH_INDEFINITE)
        val listener: RecognitionListener = object : RecognitionListener {
            override fun onResults(results: Bundle) {
                snackBar.dismiss()
                val voiceResults = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (voiceResults == null) {
                    println("No voice results")
                } else {
                    val text = voiceResults.toString()
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
            }

            override fun onError(error: Int) {
                snackBar.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Error listening for speech: $error",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onBeginningOfSpeech() {
//                Toast.makeText(requireContext(), "Listening", Toast.LENGTH_SHORT).show()
                snackBar.show()
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
    }
}