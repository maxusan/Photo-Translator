package com.chkmx.phototranslator.ui.start

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import com.chkmx.phototranslator.R
import com.chkmx.phototranslator.databinding.ActivityStartBinding
import com.chkmx.phototranslator.ui.main.MainActivity
import java.util.concurrent.ExecutorService

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
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("first", viewModel.getPrimaryLanguage().value)
            intent.putExtra("second", viewModel.getSecondaryLanguage().value)
            startActivity(intent)
            finish()
        }
    }
}