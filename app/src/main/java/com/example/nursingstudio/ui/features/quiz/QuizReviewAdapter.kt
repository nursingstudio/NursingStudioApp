package com.example.nursingstudio.ui.features.quiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.UserAnswerState
import com.example.nursingstudio.databinding.ItemQuizReviewQuestionBinding

data class ReviewItem(
    val question: QuestionItem,
    val userState: UserAnswerState,
    val questionNumber: Int
)

class QuizReviewAdapter : ListAdapter<ReviewItem, QuizReviewAdapter.ReviewViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemQuizReviewQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(
        private val binding: ItemQuizReviewQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReviewItem) {
            val context = binding.root.context
            binding.tvReviewQuestionNum.text = context.getString(R.string.question_number_fmt, item.questionNumber)
            binding.tvReviewQuestionText.text = item.question.questionText

            binding.tvReviewMarkedBadge.visibility = if (item.userState.isMarkedForReview) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.layoutOptionsContainer.removeAllViews()

            item.question.options.forEachIndexed { optIdx: Int, optText: String ->
                val optionView = LayoutInflater.from(context).inflate(
                    R.layout.item_quiz_review_option_row,
                    binding.layoutOptionsContainer,
                    false
                ) as TextView

                val prefix = ('A' + optIdx).toString()
                optionView.text = context.getString(R.string.option_prefix_fmt, prefix, optText)

                val isCorrectAnswer = (optIdx == item.question.correctAnswerIndex)
                val isUserSelected = (optIdx == item.userState.selectedOptionIndex)

                val (bgRes, textColorRes) = when {
                    isCorrectAnswer -> Pair(R.color.option_correct_bg, R.color.option_correct_border)
                    isUserSelected -> Pair(R.color.option_wrong_bg, R.color.option_wrong_border)
                    else -> Pair(R.color.option_neutral_bg, R.color.text_secondary)
                }

                optionView.setBackgroundColor(ContextCompat.getColor(context, bgRes))
                optionView.setTextColor(ContextCompat.getColor(context, textColorRes))

                binding.layoutOptionsContainer.addView(optionView)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ReviewItem>() {
        override fun areItemsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean =
            oldItem.question.questionId == newItem.question.questionId

        override fun areContentsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean =
            oldItem == newItem
    }
}