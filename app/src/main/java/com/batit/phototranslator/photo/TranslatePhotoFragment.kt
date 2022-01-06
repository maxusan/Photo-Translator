package com.batit.phototranslator.photo

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.batit.phototranslator.CustomView
import com.batit.phototranslator.databinding.FragmentTranslatePhotoBinding
import com.batit.phototranslator.main.MainViewModel
import com.batit.phototranslator.util.*
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.android.synthetic.main.fragment_translate_photo.view.*
import kotlinx.android.synthetic.main.main_fragment.*
import java.io.File


class TranslatePhotoFragment : Fragment() {

    private lateinit var binding: FragmentTranslatePhotoBinding
    private val detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val viewModel: MainViewModel by activityViewModels()
    private var imageHeight: Int = 0
    private var imageWidth: Int = 0
    val array = ArrayList<Text.Line>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslatePhotoBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    private fun observeEvents() {
        viewModel.language.observe(viewLifecycleOwner){
            binding.sourceLanguageTextView.text = it
        }
        binding.translateButton.setOnClickListener {
//            array.forEach {
                translate(array, binding.sourceLanguageTextView.text.toString(), viewModel.targetLang.value!!.code)
//            }
        }
        viewModel.translatedTextLiveData.observe(viewLifecycleOwner){
            Log.e("logs", it)
            binding.translatedText.text = it
        }
        viewModel.translatedLines.observe(viewLifecycleOwner){
            Log.e("log", it.toString())
            binding.selectedPictureContainer.tr = it
        }
        binding.saveTranslateButton.setOnClickListener {
            requireContext().checkPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE){
                if(it){
                    binding.selectedPictureContainer.saveTranslationToGallery()
                }else{
                    Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun textAsBitmap(text: String?, textSize: Float, textColor: Int): Bitmap? {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        val baseline = -paint.ascent() // ascent() is negative
        val width = (paint.measureText(text) + 0.5f).toInt() // round
        val height = (baseline + paint.descent() + 0.5f).toInt()
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text!!, 0f, baseline, paint)
        return image
    }

    private fun startImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        resultLauncher.launch(intent)
    }

    private fun translate(sourceText: ArrayList<Text.Line>, sourceCode: String, targetCode: String){
       viewModel.translate(sourceText, targetCode, sourceCode)
    }



    @SuppressLint("Range")
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Uri = result.data?.data!!
                val imageStream = requireContext().contentResolver.openInputStream(data)

                val pickedImage = BitmapFactory.decodeStream(imageStream)
                imageHeight = pickedImage.height
                imageWidth = pickedImage.width
                val inputImage = InputImage.fromBitmap(pickedImage, 0)
//                val bmm = inputImage.bitmapInternal
//                val bmp = ImageUtils.convertYuv420888ImageToBitmap(inputImage.mediaImage)
                Glide.with(requireContext()).load(inputImage.bitmapInternal)
                    .into(binding.selectedPictureContainer)
                detector.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        binding.sourceTextView.text = visionText.text

                        visionText.textBlocks.forEach {
                            it.lines.forEach {
                                array.add(it)
//                                translate(it.text, binding.sourceLanguageTextView.text.toString(), viewModel.targetLang.value!!.code)
                            }
                        }
                        array.forEach {
                            Log.e("logs", it.text)
//                            translate(it.text, binding.sourceLanguageTextView.text.toString(), viewModel.targetLang.value!!.code)
                        }
                        binding.selectedPictureContainer.rw = array
                        viewModel.getSourceLang(visionText.text)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("logs", "Text recognition error", exception)
                        exception.printStackTrace()
                    }
            }
        }



}