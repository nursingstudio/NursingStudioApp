package com.example.nursingstudio.ui.features.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nursingstudio.data.model.MediaItemModel
import com.example.nursingstudio.databinding.ItemMediaContentBinding

class ContentAdapter(
    private val items: List<MediaItemModel>,
    private val onItemClick: (MediaItemModel) -> Unit
) : RecyclerView.Adapter<ContentAdapter.MediaViewHolder>() {

    // 🚀 FIXED: Re-mapped with correct ViewBinding references to eliminate Java File core conflicts
    inner class MediaViewHolder(private val binding: ItemMediaContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaItemModel) {
            binding.tvContentTitle.text = item.title

            // 🔒 2026 Enterprise Vector Extraction Standard
            val iconResId = if (item.type == "PDF") {
                android.R.drawable.ic_menu_sort_by_size
            } else {
                android.R.drawable.ic_menu_slideshow
            }

            // Injects dynamic drawable vector directly into text bounds context safely
            binding.tvContentTitle.setCompoundDrawablesWithIntrinsicBounds(
                iconResId, // drawableStart
                0,         // drawableTop
                0,         // drawableRight
                0          // drawableBottom
            )

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}