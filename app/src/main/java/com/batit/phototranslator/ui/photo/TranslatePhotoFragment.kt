package com.batit.phototranslator.ui.photo

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.batit.phototranslator.databinding.FragmentTranslatePhotoBinding
import com.batit.phototranslator.ui.main.MainViewModel
import com.batit.phototranslator.core.util.Language
import com.batit.phototranslator.core.util.checkPermissions
import com.batit.phototranslator.core.util.saveTranslationToGallery
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.android.synthetic.main.main_fragment.*


class TranslatePhotoFragment : Fragment() {

    private lateinit var binding: FragmentTranslatePhotoBinding
    private val detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val viewModel: MainViewModel by activityViewModels()
    private var imageHeight: Int = 0
    private var imageWidth: Int = 0
    val array = ArrayList<FirebaseVisionText.Line>()

    private lateinit var functions: FirebaseFunctions


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslatePhotoBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        functions = Firebase.
        setupViews()
        observeEvents()
        startImagePicker()
    }



    private fun setupViews() {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_dropdown_item, viewModel.availableLanguages
        )
        targetLangSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.targetLang.value = adapter.getItem(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        binding.targetLangSelector.adapter = adapter
        binding.targetLangSelector.setSelection(adapter.getPosition(Language("ru")))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observeEvents() {
        viewModel.language.observe(viewLifecycleOwner) {
            binding.sourceLanguageTextView.text = it
        }
        binding.translateButton.setOnClickListener {
//            array.forEach {
            translate(
                array,
                binding.sourceLanguageTextView.text.toString(),
                viewModel.targetLang.value!!.code
            )
//            }
        }
        viewModel.translatedTextLiveData.observe(viewLifecycleOwner) {
            Log.e("logs", it)
            binding.translatedText.text = it
        }
        viewModel.translatedLines.observe(viewLifecycleOwner) {
            Log.e("log", it.toString())
            binding.selectedPictureContainer.tr = it
        }
        binding.saveTranslateButton.setOnClickListener {
            requireContext().checkPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) {
                if (it) {
                    binding.selectedPictureContainer.saveTranslationToGallery()
                } else {
                    Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        binding.selectedPictureContainer.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                Toast.makeText(
                    requireContext(),
                    "${motionEvent.x}  x  ${motionEvent.y}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return@setOnTouchListener true
        }

    }

    private fun startImagePicker() {
        ImagePicker.with(this)
            .galleryOnly()
            .crop()
            .createIntent { intent ->
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                resultLauncher.launch(intent)
            }
    }

    private fun translate(
        sourceText: ArrayList<FirebaseVisionText.Line>,
        sourceCode: String,
        targetCode: String
    ) {
        viewModel.translate(sourceText, targetCode, sourceCode)
    }


    @SuppressLint("Range")
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Uri = result.data?.data!!
                val imageStream = requireContext().contentResolver.openInputStream(data)
                var pickedImage = BitmapFactory.decodeStream(imageStream)

//                Log.e("logs", "or " + or)
                val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display: Display = wm.defaultDisplay
                imageHeight = pickedImage.height
                imageWidth = pickedImage.width
                if (display.height.toFloat() <= imageHeight) {
                    pickedImage = Bitmap.createScaledBitmap(
                        pickedImage,
                        (pickedImage.width * 0.9).toInt(), (pickedImage.height * 0.9).toInt(), true
                    )
                }
//                runTextRecognition(pickedImage)

                val inputImage = InputImage.fromFilePath(requireContext(), data)
                val image = FirebaseVisionImage.fromBitmap(pickedImage )
                val firebaseDetector = FirebaseVision.getInstance()
                    .cloudTextRecognizer

                val result = firebaseDetector.processImage(image)
                    .addOnSuccessListener { firebaseVisionText ->
                        Log.e("logs", firebaseVisionText.text)
                        binding.sourceTextView.text = firebaseVisionText.text

                        firebaseVisionText.textBlocks.forEach {
                            it.lines.forEach {
                                array.add(it)
                            }
                        }
                        binding.selectedPictureContainer.rw = array
                        viewModel.getSourceLang(firebaseVisionText.text)
                    }
                    .addOnFailureListener { e ->
                    }
                binding.selectedPictureContainer.setImageURI(data)

            }
        }


}