package com.batit.phototranslator.ui.start

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import com.batit.phototranslator.R
import com.batit.phototranslator.core.util.checkPermissions
import com.batit.phototranslator.databinding.ActivityStartBinding
import com.batit.phototranslator.ui.main.MainActivity
import com.karumi.dexter.Dexter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding
    private val viewModel: StartViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        viewModel.startMainEvent.observe(this){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}