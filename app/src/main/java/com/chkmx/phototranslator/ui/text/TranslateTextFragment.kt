package com.chkmx.phototranslator.ui.text

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.chkmx.phototranslator.R
import com.chkmx.phototranslator.core.data.Language
import com.chkmx.phototranslator.core.util.copyTextToClipboard
import com.chkmx.phototranslator.core.util.hideKeyboard
import com.chkmx.phototranslator.core.util.shareText
import com.chkmx.phototranslator.core.util.showSoftKeyboard
import com.chkmx.phototranslator.databinding.FragmentTranslateTextBinding
import com.chkmx.phototranslator.ui.MainViewModel
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import java.io.IOException


class TranslateTextFragment : Fragment() {

    private lateinit var binding: FragmentTranslateTextBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var primarySpinnerAdapter: ArrayAdapter<Language>
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<Language>

    private lateinit var secondaryLanguages: MutableList<Language>
    private lateinit var primaryLanguages: MutableList<Language>

    private val textArguments: TranslateTextFragmentArgs by navArgs()

    private var translating: Boolean = false

    private var translatedText: String = ""

    private var modelDownloading: Boolean = false

    private lateinit var translate: Translate

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslateTextBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getTranslateService()
        setupSpinners()
        setupListeners()
        if (textArguments.text != null) {
            binding.editText.setText(textArguments.text)
        }
        binding.text.text = translatedText
    }

    private fun setupSpinners() {
        primaryLanguages = viewModel.getLanguages().toMutableList().apply {
            add(0, Language.getDefaultLanguage())
        }
//        primaryLanguages.add(0, Language("Detect language"))
        secondaryLanguages = viewModel.getLanguages().toMutableList()
        secondarySpinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item, secondaryLanguages
        )
        secondarySpinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.secondarySpinner.adapter = secondarySpinnerAdapter


        primarySpinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item, primaryLanguages
        )
        primarySpinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.primarySpinner.adapter = primarySpinnerAdapter
        binding.secondarySpinner.setSelection(secondarySpinnerAdapter.getPosition(viewModel.getSecondaryLanguage().value!!))
        binding.primarySpinner.setSelection(primarySpinnerAdapter.getPosition(viewModel.getPrimaryLanguage().value!!))
        binding.ids.setOnClickListener {
            binding.editText.showSoftKeyboard()
        }
        KeyboardVisibilityEvent.setEventListener(
            requireActivity(),
            KeyboardVisibilityEventListener {
                if (it) {
                    binding.ids.isClickable = false
                    binding.ids.isFocusable = false
                } else {
                    binding.ids.isClickable = true
                    binding.ids.isFocusable = true
                }
            })
    }

    private fun setupListeners() {
        binding.primarySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.setPrimaryLanguage(primaryLanguages[p2])
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

            }
        binding.secondarySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.setSecondaryLanguage(secondaryLanguages[p2])
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

            }
        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                translateText()
            }
        })

        viewModel.getPrimaryLanguage().observe(viewLifecycleOwner) {
            translateText()

        }

        viewModel.getSecondaryLanguage().observe(viewLifecycleOwner) {
            translateText()

        }

        binding.swapButton.setOnClickListener {
            val firstIndex = binding.primarySpinner.selectedItemId
            val secondIndex = binding.secondarySpinner.selectedItemId
            val primaryLanguage = primaryLanguages[firstIndex.toInt()]
            val secondaryLanguage = secondaryLanguages[secondIndex.toInt()]
            if (primaryLanguage.code != Language.getDefaultLanguage().code) {
                binding.secondarySpinner.setSelection(
                    secondarySpinnerAdapter.getPosition(
                        primaryLanguage
                    )
                )
                binding.primarySpinner.setSelection(
                    primarySpinnerAdapter.getPosition(
                        secondaryLanguage
                    )
                )
            }
        }
        binding.copy1.setOnClickListener {
            binding.editText.text?.let {
                if (it.toString().isNotBlank() && it.toString() != "null") {
                    copyTextToClipboard(it.toString())
                }
            }
        }
        binding.copy2.setOnClickListener {
            binding.text.text?.let {
                if (it.toString().isNotBlank() && it.toString() != "null") {
                    copyTextToClipboard(it.toString())
                }
            }
        }
        binding.share1.setOnClickListener {
            binding.editText.text?.let {
                if (it.toString().isNotBlank() && it.toString() != "null") {
                    shareText(it.toString())
                }
            }
        }
        binding.share2.setOnClickListener {
            binding.text.text?.let {
                if (it.toString().isNotBlank() && it.toString() != "null") {
                    shareText(it.toString())
                }
            }
        }
        binding.preview1.setOnClickListener {
            binding.editText.text?.let {
                if (it.toString().isNotBlank() && it.toString() != "null") {
                    findNavController().navigate(
                        TranslateTextFragmentDirections.actionTranslateTextFragmentToTextPreviewFragment(
                            it.toString()
                        )
                    )
                }
            }
        }
        binding.preview2.setOnClickListener {
            binding.text.text?.let {
                if (it.toString().isNotBlank() && it.toString() != "null") {
                    findNavController().navigate(
                        TranslateTextFragmentDirections.actionTranslateTextFragmentToTextPreviewFragment(
                            it.toString()
                        )
                    )
                }
            }
        }
        binding.editTextRoot.setOnClickListener {
            binding.editText.requestFocus()
        }
        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }
    }


    private fun translateText() {
        binding.editText.text.toString()?.let {
                lifecycleScope.launch(Dispatchers.IO){
                    translate(
                        viewModel.getPrimaryLanguage().value!!.code,
                        viewModel.getSecondaryLanguage().value!!.code,
                        it
                    )
                }
        }

    }

    private fun getTranslateService() {
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            resources.openRawResource(R.raw.credentials).use { `is` ->
                val myCredentials = GoogleCredentials.fromStream(`is`)
                val translateOptions =
                    TranslateOptions.newBuilder().setCredentials(myCredentials).build()
                translate = translateOptions.service
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    suspend fun translate(source: String, target: String, text: String) {
        kotlin.runCatching {
            val translation: Translation = translate.translate(
                text,
                Translate.TranslateOption.targetLanguage(target),
                if (source != Language.getDefaultLanguage().code) Translate.TranslateOption.sourceLanguage(
                    source
                ) else Translate.TranslateOption.model("base")
            )
            withContext(Dispatchers.Main){
                translatedText = translation.translatedText
                binding.text.text = translatedText
            }
        }.exceptionOrNull()?.let{
            withContext(Dispatchers.Main){
                binding.text.text = binding.editText.text.toString()
                it.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().hideKeyboard()
    }


}