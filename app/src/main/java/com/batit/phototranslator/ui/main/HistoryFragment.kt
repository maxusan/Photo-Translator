package com.batit.phototranslator.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.batit.phototranslator.core.db.PhotoItem
import com.batit.phototranslator.databinding.FragmentHistoryBinding
import com.batit.phototranslator.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HistoryFragment : Fragment(), HistoryAdapter.PhotoClicker {

    private lateinit var binding: FragmentHistoryBinding
    private val adapter: HistoryAdapter by lazy { HistoryAdapter(this) }
    private val viewModel: MainViewModel by activityViewModels()

    private var flag: Boolean = false

    private var resume: Boolean = false

    override fun onResume() {
        super.onResume()
        resume = true
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.photoList.adapter = adapter
        viewModel.getPhotos().observe(viewLifecycleOwner) {
            if(!resume){
                adapter.submitList(null)
            }
            adapter.submitList(it.asReversed())
            viewModel.setInDelete(false)
            resume = false
        }
        val itemTouchHelper = ItemTouchHelper(
            SwipeToDeleteCallback(
                adapter,
                requireContext(),
                object : SwipeToDeleteCallback.SwipeDelete {
                    override fun swipeDelete(position: Int) {
                        val currentPhoto = adapter.currentList[position]
                        viewModel.deletePhoto(currentPhoto)
                    }
                })
        )
        itemTouchHelper.attachToRecyclerView(binding.photoList)
        binding.selectAll.setOnClickListener {
            val currntList = adapter.currentList.toMutableList()
            currntList.forEach {
                if (!it.photoPicked) {
                    it.setPhotoPicked()
                }
            }
            viewModel.setInDelete(true)
            adapter.submitList(currntList)
            adapter.notifyDataSetChanged()
        }
        binding.delete.setOnClickListener {
            val currntList = adapter.currentList.toMutableList().filter { item -> item.photoPicked }
            currntList.forEach {
                viewModel.deletePhoto(it)
            }
            viewModel.setInDelete(false)
        }
        binding.cancel.setOnClickListener {
            val currntList = adapter.currentList.toMutableList()
            currntList.forEach {
                if (it.photoPicked)
                    it.setPhotoPicked()
            }
            viewModel.setInDelete(false)
            adapter.submitList(currntList)
        }
        viewModel.getInDelete().observe(viewLifecycleOwner) {
            binding.inDelete = it
            adapter.inLongClick = it
        }
        binding.appBarHistory.toolbar.setNavigationOnClickListener {
            viewModel.openDrawer()
        }
    }

    override fun longClick(photoItem: PhotoItem) {
        val currList = adapter.currentList.toMutableList()
        val idx = currList.indexOf(photoItem)
        currList[idx].setPhotoPicked()
        val countOfPicked = currList.count { item -> item.photoPicked }
        val inDelete = countOfPicked != 0
        adapter.inLongClick = inDelete
        viewModel.setInDelete(inDelete)

        adapter.submitList(currList)
        adapter.notifyItemChanged(idx)
    }

    override fun click(photoItem: PhotoItem) {
        if(!flag){
            flag = true
            lifecycleScope.launch(Dispatchers.Main){
                kotlin.runCatching {
                    findNavController().navigate(
                        HistoryFragmentDirections.actionHistoryToTranslateFragment2(
                            Uri.parse(photoItem.photoUri)
                        )
                    )
                }.exceptionOrNull()?.printStackTrace()
                delay(100)
                flag = false
            }
        }


    }
}