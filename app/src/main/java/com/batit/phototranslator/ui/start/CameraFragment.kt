package com.batit.phototranslator.ui.start

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.batit.phototranslator.core.util.checkPermissions
import com.batit.phototranslator.databinding.FragmentCameraBinding
import com.github.dhaval2404.imagepicker.ImagePicker
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.batit.phototranslator.R


class CameraFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    private var torchEnabled: Boolean = false

    private lateinit var binding: FragmentCameraBinding
    private val viewModel: StartViewModel by activityViewModels()
    private lateinit var primarySpinnerAdapter: ArrayAdapter<String>
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkPermissionsForCamera()
        setupListeners()
        setupSpinners()
    }

    private fun setupSpinners() {
        val languages = viewModel.getLanguages()
        val primarySpinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item, languages
        )
        primarySpinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.primarySpinner.setAdapter(primarySpinnerAdapter)

        val secondarySpinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item, languages
        )
        secondarySpinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.secondarySpinner.setAdapter(primarySpinnerAdapter)
    }

    private fun setupListeners() {
        binding.torchButton.setOnClickListener {
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                torchEnabled = !torchEnabled
                binding.torch = torchEnabled
                camera?.cameraControl?.enableTorch(torchEnabled)
            }
        }
        binding.galleryButton.setOnClickListener {
            launchGalleryPicker()
        }
        binding.photoButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        val takePicture = imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("logs", "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    kotlin.runCatching {
                        val folder = File(requireContext().cacheDir.path + "CameraX/")
                        if (!folder.exists()) {
                            folder.mkdir()
                        }
                        val imageFileDest = File(folder, UUID.randomUUID().toString() + ".jpg")
                        val intent = UCrop.of(output.savedUri!!, Uri.fromFile(imageFileDest))
                            .getIntent(requireActivity())
                        startForCrop.launch(intent)
                    }
                }
            }
        )
    }


    private fun launchGalleryPicker() {
        ImagePicker.with(this)
            .galleryOnly()
            .crop()
            .createIntent { intent ->
                startForGalleryImageResult.launch(intent)
            }
    }

    private fun checkPermissionsForCamera() {
        requireContext().checkPermissions(android.Manifest.permission.CAMERA) {
            if (it) {
                startCamera()
            }
        }
    }

    var startForCrop = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            assert(result.data != null)
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                findNavController().navigate(
                    CameraFragmentDirections.actionCameraFragmentToTranslateFragment(
                        resultUri
                    )
                )
            }
        } else {
            Toast.makeText(requireContext(), "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val startForGalleryImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data
            if (resultCode == Activity.RESULT_OK) {
                kotlin.runCatching {
                    findNavController().navigate(
                        CameraFragmentDirections.actionCameraFragmentToTranslateFragment(
                            data?.data!!
                        )
                    )
                }
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.createSurfaceProvider())
                }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )


            } catch (exc: Exception) {
                Log.e("logs", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}