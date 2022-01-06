package com.batit.phototranslator.photo

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.batit.phototranslator.CustomView
import com.batit.phototranslator.databinding.FragmentTranslatePhotoBinding
import com.batit.phototranslator.main.MainViewModel
import com.batit.phototranslator.util.Language
import com.batit.phototranslator.util.PathUtil
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
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
        binding.targetLangSelector.setSelection(adapter.getPosition(Language("en")))
    }

    private fun observeEvents() {
        viewModel.language.observe(viewLifecycleOwner){
            binding.sourceLanguageTextView.text = it
        }
        binding.translateButton.setOnClickListener {
            translate(binding.sourceTextView.text.toString(), binding.sourceLanguageTextView.text.toString(), viewModel.targetLang.value!!.code)

        }
        viewModel.translatedTextLiveData.observe(viewLifecycleOwner){
            binding.translatedText.text = it
//            val bmp = textAsBitmap(binding.translatedText.text.toString(), 14f, Color.BLACK)
//            bmp
//            val bmp2 = createBitmapFromView(binding.translatedText, binding.selectedPictureContainer.width, binding.selectedPictureContainer.height)
//            bmp2
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

    private fun translate(sourceText: String, sourceCode: String, targetCode: String){
       viewModel.translate(sourceText, targetCode, sourceCode)
    }



    @SuppressLint("Range")
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Uri = result.data?.data!!
                val imageStream = requireContext().contentResolver.openInputStream(data)

                val pickedImage = BitmapFactory.decodeStream(imageStream)
//                val exifInterface = ExifInterface(PathUtil.getPath(requireContext(), data))
//                val degree = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)?.toInt()
                imageHeight = pickedImage.height
                imageWidth = pickedImage.width


                val inputImage = InputImage.fromFilePath(requireContext(), data)
                val image = inputImage.bitmapInternal
                Glide.with(requireContext()).load(inputImage.bitmapInternal)
                    .into(binding.selectedPictureContainer)


//                Log.e("logs", inputImage.)
//                Log.e("logs", inputImage.)
//                Log.e("logs", inputImage.)
//                Log.e("logs", inputImage.)
                detector.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        binding.sourceTextView.text = visionText.text
                        val array = visionText.text.split("\n")
                        array.forEach {
                            Log.e("logs", it)
                        }
                        binding.selectedPictureContainer.rw = visionText.textBlocks
                        viewModel.getSourceLang(visionText.text)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("logs", "Text recognition error", exception)
                        exception.printStackTrace()
                    }
            }
        }



}