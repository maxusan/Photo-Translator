package com.batit.phototranslator.ui.main

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.batit.phototranslator.R
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.util.checkPermissions
import com.batit.phototranslator.databinding.FragmentMainBinding
import com.batit.phototranslator.ui.MainViewModel


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<Language>
    private lateinit var secondaryLanguages: MutableList<Language>
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
        binding.appBarMain.secondarySpinner.setSelection(secondarySpinnerAdapter.getPosition(Language("en")))
        binding.appBarMain.secondarySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
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
            findNavController().navigate(MainFragmentDirections.actionHomeToTranslateTextFragment())
        }
        binding.record.setOnClickListener {
            requireContext().checkPermissions(android.Manifest.permission.RECORD_AUDIO){
                if(it){
                    startSpeechRecognition()
                }
            }
        }
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
        val listener: RecognitionListener = object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val voiceResults = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (voiceResults == null) {
                    println("No voice results")
                } else {
                    println("Printing matches: ")
                    for (match in voiceResults) {
                        println(match)
                    }
                }
            }

            override fun onReadyForSpeech(params: Bundle) {
                println("Ready for speech")
            }

            override fun onError(error: Int) {
                Toast.makeText(requireContext(), "Error listening for speech: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {
                Toast.makeText(requireContext(), "Listening", Toast.LENGTH_SHORT).show()
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