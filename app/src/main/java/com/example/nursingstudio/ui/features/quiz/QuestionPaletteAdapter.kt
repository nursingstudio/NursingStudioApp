package com.example.nursingstudio.ui.features.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.QuestionPaletteItem
import com.example.nursingstudio.data.model.QuestionStatus
import com.example.nursingstudio.databinding.ItemQuestionPaletteBinding

class QuestionPaletteAdapter(
    private val onQuestionClick: (questionIndex: Int) -> Unit
) : ListAdapter<QuestionPaletteItem, QuestionPaletteAdapter.PaletteViewHolder>(PaletteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaletteViewHolder {
        val binding = ItemQuestionPaletteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaletteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaletteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaletteViewHolder(
        private val binding: ItemQuestionPaletteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuestionPaletteItem) {
            val context = binding.root.context
            binding.tvQuestionNumber.text = item.displayIndex.toString()

            val (bgColor, textColor, strokeColor) = when (item.status) {
                QuestionStatus.ANSWERED -> Triple(
                    ContextCompat.getColor(context, R.color.palette_answered),
                    ContextCompat.getColor(context, android.R.color.white),
                    ContextCompat.getColor(context, R.color.palette_answered)
                )
                QuestionStatus.SKIPPED -> Triple(
                    ContextCompat.getColor(context, R.color.palette_skipped),
                    ContextCompat.getColor(context, android.R.color.white),
                    ContextCompat.getColor(context, R.color.palette_skipped)
                )
                QuestionStatus.UNANSWERED -> Triple(
                    ContextCompat.getColor(context, R.color.palette_unanswered),
                    ContextCompat.getColor(context, R.color.text_primary),
                    ContextCompat.getColor(context, R.color.palette_unanswered_border)
                )
                QuestionStatus.REVIEW -> Triple(
                    ContextCompat.getColor(context, R.color.palette_review),
                    ContextCompat.getColor(context, android.R.color.white),
                    ContextCompat.getColor(context, R.color.palette_review)
                )
            }

            binding.cardPaletteItem.setCardBackgroundColor(bgColor)
            binding.tvQuestionNumber.setTextColor(textColor)
            binding.cardPaletteItem.strokeColor = if (item.isCurrent) {
                ContextCompat.getColor(context, R.color.brand_saffron)
            } else strokeColor

            binding.cardPaletteItem.strokeWidth = if (item.isCurrent) 6 else 3

            binding.cardPaletteItem.setOnClickListener {
                onQuestionClick(item.questionIndex)
            }
        }
    }

    private class PaletteDiffCallback : DiffUtil.ItemCallback<QuestionPaletteItem>() {
        override fun areItemsTheSame(oldItem: QuestionPaletteItem, newItem: QuestionPaletteItem): Boolean {
            return oldItem.questionIndex == newItem.questionIndex
        }

        override fun areContentsTheSame(oldItem: QuestionPaletteItem, newItem: QuestionPaletteItem): Boolean {
            return oldItem == newItem
        }
    }
}