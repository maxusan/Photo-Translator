package com.batit.phototranslator.ui.main

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.batit.phototranslator.R
import com.batit.phototranslator.core.db.PhotoItem
import com.batit.phototranslator.databinding.ListItemPhotoBinding


class HistoryAdapter(var photoClicker: PhotoClicker) : ListAdapter<PhotoItem, HistoryAdapter.HistoryPhotoHolder>(PhotoCallback()) {

    var inLongClick: Boolean = false

    inner class HistoryPhotoHolder(val binding: ListItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindPhoto(photoItem: PhotoItem) {
            binding.photo = photoItem
            binding.root.setOnLongClickListener {
                photoClicker.longClick(photoItem)
                true
            }
            binding.root.setOnClickListener {
                if(inLongClick){
                    photoClicker.longClick(photoItem)
                }else{
                    photoClicker.click(photoItem)
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryPhotoHolder {
        val binding: ListItemPhotoBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.list_item_photo,
            parent,
            false
        )
        return HistoryPhotoHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryPhotoHolder, position: Int) {
        holder.bindPhoto(currentList[position])
    }

    interface PhotoClicker{
        fun longClick(photoItem: PhotoItem)
        fun click(photoItem: PhotoItem)
    }
}

class PhotoCallback : DiffUtil.ItemCallback<PhotoItem>() {
    override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem == newItem
    }

}

class SwipeToDeleteCallback(
    var adapter: HistoryAdapter,
    context: Context,
    var swipeDelete: SwipeDelete
) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private var background: Drawable? = null
    private var icon: Drawable? = null

    init {
        background = ContextCompat.getDrawable(context, R.drawable.delete_background)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView: View = viewHolder.itemView
        val iconMargin: Int = (itemView.getHeight() - icon!!.getIntrinsicHeight()) / 4
        val iconTop: Int =
            itemView.getTop() + (itemView.getHeight() - icon!!.getIntrinsicHeight()) / 2
        val iconBottom: Int = iconTop + icon!!.getIntrinsicHeight()

        background!!.setBounds(
            itemView.left,
            itemView.top, itemView.right, itemView.bottom
        )
        val iconLeft = itemView.right - iconMargin - icon!!.intrinsicWidth
        val iconRight = itemView.right - iconMargin
        icon!!.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        background!!.draw(c)
        icon!!.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        var deleted = false
        AlertDialog.Builder(
            viewHolder.itemView.context
        ).setMessage(
            "This action cannot be undone.\n" +
                    "Do you want to continue?"
        ).setPositiveButton(
            "Delete"
        ) { p0, p1 ->
            deleted = true
            swipeDelete.swipeDelete(viewHolder.adapterPosition)
        }
            .setNegativeButton("Cancel") { p0, p1 ->
                p0.dismiss()
            }
            .setOnDismissListener {
                if (!deleted) {
                    adapter.notifyItemChanged(viewHolder.adapterPosition)
                }
            }
            .show()

    }

    interface SwipeDelete {
        fun swipeDelete(position: Int)
    }

}