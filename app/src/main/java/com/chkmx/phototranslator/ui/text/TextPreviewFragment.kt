package com.chkmx.phototranslator.ui.text

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.chkmx.phototranslator.core.util.copyTextToClipboard
import com.chkmx.phototranslator.core.util.shareText
import com.chkmx.phototranslator.databinding.FragmentTextPreviewBinding


class TextPreviewFragment : Fragment() {

    private lateinit var binding: FragmentTextPreviewBinding
    private val textArgs: TextPreviewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextPreviewBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.text = textArgs.text
        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.share.setOnClickListener {
            shareText(textArgs.text)
        }
        binding.copy.setOnClickListener {
            copyTextToClipboard(textArgs.text)
        }
    }
}