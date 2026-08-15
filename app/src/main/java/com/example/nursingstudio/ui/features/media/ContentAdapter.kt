package com.example.nursingstudio.ui.features.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.MediaItemModel
import com.example.nursingstudio.databinding.ItemMediaContentBinding

class ContentAdapter(
    private val items: List<MediaItemModel>,
    private val onItemClick: (MediaItemModel) -> Unit
) : RecyclerView.Adapter<ContentAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(private val binding: ItemMediaContentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItemModel) {
            binding.tvContentTitle.text = item.title

            // 1. Dynamic Start Drawable (PDF vs VIDEO)
            val startIconRes = if (item.type.equals("PDF", ignoreCase = true)) {
                R.drawable.ic_pdf_24
            } else {
                R.drawable.ic_video_24
            }

            binding.tvContentTitle.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(binding.root.context, startIconRes),
                null, null, null
            )

            // 2. Dynamic Lock Status End Icon (FREE vs PAID)
            if (item.computedIsLocked) {
                binding.ivLockStatus.setImageResource(R.drawable.ic_lock)
            } else {
                binding.ivLockStatus.setImageResource(R.drawable.ic_lock_open)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaContentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}