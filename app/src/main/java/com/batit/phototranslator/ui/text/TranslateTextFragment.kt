package com.batit.phototranslator.ui.text

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.batit.phototranslator.R
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.util.copyTextToClipboard
import com.batit.phototranslator.core.util.shareText
import com.batit.phototranslator.databinding.FragmentTranslateTextBinding
import com.batit.phototranslator.ui.MainViewModel
import kotlinx.android.synthetic.main.fragment_text_preview.*


class TranslateTextFragment : Fragment() {

    private lateinit var binding: FragmentTranslateTextBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var primarySpinnerAdapter: ArrayAdapter<Language>
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<Language>

    private lateinit var secondaryLanguages: MutableList<Language>
    private lateinit var primaryLanguages: MutableList<Language>

    private val textArguments: TranslateTextFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslateTextBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupListeners()
        if(textArguments.text != null){
            binding.editText.setText(textArguments.text)
        }
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
        binding.secondarySpinner.setSelection(secondarySpinnerAdapter.getPosition(Language("en")))

        primarySpinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item, primaryLanguages
        )
        primarySpinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.primarySpinner.adapter = primarySpinnerAdapter
        binding.primarySpinner.setSelection(primarySpinnerAdapter.getPosition(Language("ru")))
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
                    findNavController().navigate(TranslateTextFragmentDirections.actionTranslateTextFragmentToTextPreviewFragment(it.toString()))
                }
            }
        }
        binding.preview2.setOnClickListener {
            binding.text.text?.let {
                if (it.toString().isNotBlank() && it.toString() != "null") {
                    findNavController().navigate(TranslateTextFragmentDirections.actionTranslateTextFragmentToTextPreviewFragment(it.toString()))
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
        kotlin.runCatching {
            binding.editText.text.toString()?.let {
                if (!it.isNullOrBlank()) {
                    viewModel.translateText(
                        it,
                        viewModel.getPrimaryLanguage().value!!.code,
                        viewModel.getSecondaryLanguage().value!!.code
                    ) {
                        binding.text.text = it
                    }
                }
            }
        }
    }


}