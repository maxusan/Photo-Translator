package com.batit.phototranslator.ui.start

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.batit.phototranslator.R
import com.batit.phototranslator.databinding.FragmentTranslateBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition


class TranslateFragment : Fragment() {

    private val translateArgs: TranslateFragmentArgs by navArgs()

    private lateinit var binding: FragmentTranslateBinding

    private val viewModel: StartViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslateBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(this)
            .asBitmap()
            .load(translateArgs.imageUri)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                   binding.translateView.setImage(resource)
                    viewModel.detectText(resource){

                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }
}