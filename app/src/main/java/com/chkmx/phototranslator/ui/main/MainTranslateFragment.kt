package com.chkmx.phototranslator.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.chkmx.phototranslator.R
import com.chkmx.phototranslator.core.util.SaveManager
import com.chkmx.phototranslator.core.util.getImageFromUri
import com.chkmx.phototranslator.core.view.TranslateView
import com.chkmx.phototranslator.databinding.FragmentTranslateBinding
import com.chkmx.phototranslator.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*


class MainTranslateFragment : Fragment() {
    private val translateArgs: MainTranslateFragmentArgs by navArgs()

    private lateinit var binding: FragmentTranslateBinding

    private val viewModel: MainViewModel by activityViewModels()

    private var modelDownloading: Boolean = true
    private lateinit var snackbar: Snackbar
    private var imageUri: Uri? = null
    private var cropped: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslateBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processText(if(!cropped)translateArgs.imageUri else imageUri!!)
        snackbar = Snackbar.make(binding.root, "Please wait", Snackbar.LENGTH_INDEFINITE)
        viewModel.getModelDownloading().observe(viewLifecycleOwner) {
            modelDownloading = it
            if (it) {
                snackbar.show()
            } else {
                snackbar.dismiss()
            }
        }
        binding.buttonText.setOnClickListener {
            if (modelDownloading) {
                Toast.makeText(
                    requireContext(),
                    "Please wait until download finish",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                binding.translateView.showText = !binding.translateView.showText
            }
        }
        binding.buttonThreeDot.setOnClickListener {
            showMenu(it)
        }
        binding.buttonShare.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.captureProgress.visibility = View.VISIBLE
                }
                val path = SaveManager.saveImage(
                    requireContext(),
                    binding.translateView.getImageWithTranslate()
                )
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().packageName + ".fileprovider",
                    File(path)
                )
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "image/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(shareIntent, "Share Image"))
                withContext(Dispatchers.Main) {
                    binding.captureProgress.visibility = View.GONE
                }
            }
            binding.translateView.setDrawingCallback(object : TranslateView.DrawingCallback {
                override fun drawing(value: Boolean) {
                    if (value) {
                        binding.captureProgress.visibility = View.VISIBLE
                    } else {
                        binding.captureProgress.visibility = View.GONE
                    }
                }

            })

        }
        binding.close.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun processText(uri: Uri) {
        kotlin.runCatching {
            requireContext().getImageFromUri(uri) {
                binding.translateView.setTranslatedText(mutableListOf())
                binding.translateView.setImage(it)
                viewModel.detectText(it) {
                    viewModel.translateText(
                        requireContext(),
                        it,
                        viewModel.getPrimaryLanguage().value!!.code,
                        viewModel.getSecondaryLanguage().value!!.code
                    ) {
                        binding.translateView.setTranslatedText(it)
                    }
                }
            }
        }.exceptionOrNull()?.printStackTrace()
    }

    private fun showMenu(it: View) {
        val popup = PopupMenu(requireActivity(), it)
        popup.menuInflater
            .inflate(R.menu.translate_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.translate -> {
                    findNavController().navigate(MainTranslateFragmentDirections.actionTranslateFragment2ToPickLanguageFragment2())
                }
                R.id.cropImage -> {
                    cropImage(translateArgs.imageUri)
                }
            }
            true
        }

        popup.show()
    }

    private var startForCrop = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                cropped = true
                imageUri = resultUri
                viewModel.setModelDownloading(true)
                processText(resultUri)
            }
        } else {
            Toast.makeText(requireContext(), "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropImage(imageUri: Uri) {
        kotlin.runCatching {
            val folder = File(requireContext().cacheDir.path + "CameraX/")
            if (!folder.exists()) {
                folder.mkdir()
            }
            val imageFileDest = File(folder, UUID.randomUUID().toString() + ".jpg")
            val intent = UCrop.of(imageUri, Uri.fromFile(imageFileDest))
                .getIntent(requireActivity())
            startForCrop.launch(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        snackbar.dismiss()
    }

}