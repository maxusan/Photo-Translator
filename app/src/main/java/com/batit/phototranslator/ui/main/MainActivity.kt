package com.batit.phototranslator.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.batit.phototranslator.R
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.databinding.ActivityMainBinding
import com.batit.phototranslator.ui.MainViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var secondarySpinnerAdapter: ArrayAdapter<Language>
    private lateinit var secondaryLanguages: MutableList<Language>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val navController = Navigation.findNavController(this, R.id.container)
        binding.bottomNavView.setupWithNavController(navController)
        viewModel.openDrawerEvent.observe(this) {
            binding.drawer.openDrawer(Gravity.LEFT, true)
        }
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.home -> showBottomNav()
                R.id.history -> showBottomNav()
                else -> hideBottomNav()
            }
        }
    }

    private fun hideBottomNav() {
        binding.bottomNavView.visibility = View.GONE
    }

    private fun showBottomNav() {
        binding.bottomNavView.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        kotlin.runCatching {
//            if (resultCode == Activity.RESULT_OK && data != null) {
//                val docPaths = ArrayList<Uri>()
//                docPaths.addAll(data.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS)!!)
//                val uri = docPaths[0]
//                viewModel.pickDocument(uri)
//            }
//        }
    }
}
