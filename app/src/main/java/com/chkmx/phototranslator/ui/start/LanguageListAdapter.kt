package com.batit.phototranslator.ui.start

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.batit.phototranslator.R
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.databinding.ListItemLanguageBinding

class LanguageListAdapter: ListAdapter<Language, LanguageListAdapter.LanguageHolder>(LanguageCallback()) {

    var languageClick: LanguageClick? = null

    inner class LanguageHolder(val binding: ListItemLanguageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bindLanguage(language: Language) {
            binding.language = language
            binding.root.setOnClickListener {
                languageClick?.languageClick(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageHolder {
        val binding: ListItemLanguageBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.list_item_language,
            parent,
            false
        )
        return LanguageHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageHolder, position: Int) {
        holder.bindLanguage(currentList[position])
    }

    interface LanguageClick{
        fun languageClick(language: Language)
    }
}

class LanguageCallback: DiffUtil.ItemCallback<Language>(){
    override fun areItemsTheSame(oldItem: Language, newItem: Language): Boolean {
        return oldItem.code == newItem.code
    }

    override fun areContentsTheSame(oldItem: Language, newItem: Language): Boolean {
        return oldItem == newItem
    }

}