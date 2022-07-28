package com.batit.phototranslator.ui.main

import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.batit.phototranslator.BuildConfig
import com.batit.phototranslator.R
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.data.LanguageProvider
import com.batit.phototranslator.core.util.openLink
import com.batit.phototranslator.databinding.ActivityMainBinding
import com.batit.phototranslator.ui.MainViewModel
import com.google.android.material.navigation.NavigationView

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
        binding.navView.setupWithNavController(navController)
        viewModel.openDrawerEvent.observe(this) {
            binding.drawer.openDrawer(Gravity.LEFT, true)
        }
        val version = binding.navView.getHeaderView(0).findViewById<TextView>(R.id.version)
        version.text = "Version: ${BuildConfig.VERSION_NAME}"
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.home -> showBottomNav()
                R.id.history -> showBottomNav()
                else -> hideBottomNav()
            }
        }
        val menu = binding.navView.menu
        menu.findItem(R.id.rate).setOnMenuItemClickListener {
            openLink(getString(R.string.market_url))
            true
        }
        menu.findItem(R.id.terms).setOnMenuItemClickListener {
            openLink(getString(R.string.terms_link))
            true
        }
        viewModel.setPrimaryLanguage(intent.getSerializableExtra("first")!! as Language)
        viewModel.setSecondaryLanguage(intent.getSerializableExtra("second")!! as Language)
        viewModel.getInDelete().observe(this) {
            if (it && navController.currentDestination!!.id == R.id.history) {
                hideBottomNav()
            } else if (!it && navController.currentDestination!!.id == R.id.history) {
                showBottomNav()
            }

        }
    }

    private fun hideBottomNav() {
        binding.bottomNavView.visibility = View.GONE
    }

    private fun showBottomNav() {
        binding.bottomNavView.visibility = View.VISIBLE
    }
}
