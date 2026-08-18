package com.example.nursingstudio.ui.features.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.QuestionStatus
import com.example.nursingstudio.data.model.UserAnswerState
import com.example.nursingstudio.databinding.ItemQuestionPaletteBinding

class QuestionPaletteAdapter(
    private val onQuestionClick: (Int) -> Unit
) : ListAdapter<UserAnswerState, QuestionPaletteAdapter.PaletteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaletteViewHolder {
        val binding = ItemQuestionPaletteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaletteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaletteViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class PaletteViewHolder(
        private val binding: ItemQuestionPaletteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: UserAnswerState, questionNum: Int) {
            binding.tvQuestionNumber.text = questionNum.toString()
            val context = binding.root.context

            val (bgColorRes, textColorRes) = when (state.status) {
                QuestionStatus.UNVISITED -> Pair(R.color.palette_unvisited, R.color.white)
                QuestionStatus.UNANSWERED -> Pair(R.color.palette_unanswered, R.color.white)
                QuestionStatus.ANSWERED -> Pair(R.color.palette_answered, R.color.white)
                QuestionStatus.MARKED_FOR_REVIEW -> Pair(R.color.palette_review, R.color.white)
                QuestionStatus.ANSWERED_AND_MARKED -> Pair(R.color.palette_answered_marked, R.color.white)
            }

            binding.cardPaletteItem.setCardBackgroundColor(ContextCompat.getColor(context, bgColorRes))
            binding.tvQuestionNumber.setTextColor(ContextCompat.getColor(context, textColorRes))

            // 🚀 Toggle Green Badge for ANSWERED_AND_MARKED status
            binding.viewAnsweredBadge.isVisible = (state.status == QuestionStatus.ANSWERED_AND_MARKED)

            // Safe position reference using bindingAdapterPosition
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onQuestionClick(position)
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<UserAnswerState>() {
        override fun areItemsTheSame(oldItem: UserAnswerState, newItem: UserAnswerState): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: UserAnswerState, newItem: UserAnswerState): Boolean {
            return oldItem == newItem
        }
    }
}