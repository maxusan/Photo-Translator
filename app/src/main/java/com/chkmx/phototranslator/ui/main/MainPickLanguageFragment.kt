package com.chkmx.phototranslator.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.chkmx.phototranslator.core.data.Language
import com.chkmx.phototranslator.core.data.LanguageProvider
import com.chkmx.phototranslator.databinding.FragmentPickLanguageBinding
import com.chkmx.phototranslator.ui.MainViewModel
import com.chkmx.phototranslator.ui.start.LanguageListAdapter
import com.chkmx.phototranslator.ui.start.LanguageState

class MainPickLanguageFragment: Fragment() {

    private lateinit var binding: FragmentPickLanguageBinding
    private val viewModel: MainViewModel by activityViewModels()

    private val primaryAdapter: LanguageListAdapter by lazy { LanguageListAdapter() }
    private val secondaryAdapter: LanguageListAdapter by lazy { LanguageListAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickLanguageBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        primaryAdapter.submitList(LanguageProvider.getLanguages().toMutableList().apply {
            this.add(0, Language.getDefaultLanguage())
        })
        secondaryAdapter.submitList(LanguageProvider.getLanguages())
        viewModel.getPrimaryLanguage().observe(viewLifecycleOwner) {
            binding.primary = it
            val primaryList = primaryAdapter.currentList.toMutableList()
            val prevIndex = primaryList.indexOfFirst { lang -> lang.languageSelected }
            val currIndex = primaryList.indexOf(it)
            if (prevIndex != -1)
                primaryList[prevIndex].languageSelected = false
            primaryList[currIndex].languageSelected = true
            primaryAdapter.submitList(primaryList)
        }
        viewModel.getSecondaryLanguage().observe(viewLifecycleOwner) {
            binding.secondary = it
            val secondaryList = secondaryAdapter.currentList.toMutableList()
            val prevIndex = secondaryList.indexOfFirst { lang -> lang.languageSelected }
            val currIndex = secondaryList.indexOf(it)
            if (prevIndex != -1)
                secondaryList[prevIndex].languageSelected = false
            secondaryList[currIndex].languageSelected = true
            secondaryAdapter.submitList(secondaryList)
        }
        viewModel.getLanguageState().observe(viewLifecycleOwner) {
            binding.mode = it
            when (it) {
                LanguageState.PRIMARY -> binding.languageList.adapter = primaryAdapter
                LanguageState.SECONDARY -> binding.languageList.adapter = secondaryAdapter
            }

        }
        binding.primaryLanguage.setOnClickListener {
            viewModel.setLanguageState(LanguageState.PRIMARY)
        }
        binding.secondaryLanguage.setOnClickListener {
            viewModel.setLanguageState(LanguageState.SECONDARY)
        }
        binding.swapButton.setOnClickListener {
            val primaryLanguage = viewModel.getPrimaryLanguage().value!!
            val secondaryLanguage = viewModel.getSecondaryLanguage().value!!
            if(primaryLanguage.code != Language.getDefaultLanguage().code){
                viewModel.setPrimaryLanguage(secondaryLanguage)
                viewModel.setSecondaryLanguage(primaryLanguage)
            }
        }

        primaryAdapter.languageClick = object: LanguageListAdapter.LanguageClick{
            override fun languageClick(language: Language) {
                viewModel.setPrimaryLanguage(language)
            }
        }
        secondaryAdapter.languageClick = object: LanguageListAdapter.LanguageClick{
            override fun languageClick(language: Language) {
                viewModel.setSecondaryLanguage(language)
            }
        }
        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }
    }

}