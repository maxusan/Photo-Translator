package com.batit.phototranslator.photo

import android.R
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.batit.phototranslator.databinding.FragmentTranslatePhotoBinding
import com.batit.phototranslator.main.MainViewModel
import com.batit.phototranslator.util.Language
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.main_fragment.*

class TranslatePhotoFragment : Fragment() {

    private lateinit var binding: FragmentTranslatePhotoBinding
    private val detector = TextRecognition.getClient()
    private val viewModel: MainViewModel by activityViewModels()


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
            translate(binding.sourceTextView.text.toString(), binding.sourceLanguageTextView.text.toString(), viewModel.targetLang.value!!.code,)
        }
        viewModel.translatedTextLiveData.observe(viewLifecycleOwner){
            binding.translatedText.text = it
        }

    }

    private fun startImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        resultLauncher.launch(intent)
    }

    private fun translate(sourceText: String, sourceCode: String, targetCode: String){
//        val text = viewModel.translate(sourceText, sourceCode, targetCode).result
       viewModel.translate(sourceText, targetCode, sourceCode)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Uri = result.data?.data!!
                val imageStream = requireContext().contentResolver.openInputStream(data)
                val pickedImage = BitmapFactory.decodeStream(imageStream)
                Glide.with(requireContext()).load(pickedImage)
                    .into(binding.selectedPictureContainer)
                val inputImage = InputImage.fromBitmap(pickedImage, 0)
                detector.process(inputImage)
                    .addOnSuccessListener { visionText ->
//                        viewModel.sourceText.value = visionText.text
                        binding.sourceTextView.text = visionText.text
                        viewModel.getSourceLang(visionText.text)
//                        Toast.makeText(requireContext(), visionText.text, Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("logs", "Text recognition error", exception)
                        exception.printStackTrace()
                    }
            }
        }
}