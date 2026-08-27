package com.example.nursingstudio.ui.features.quiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.MediaType
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

    override fun onViewRecycled(holder: ReviewViewHolder) {
        super.onViewRecycled(holder)
        holder.releaseMedia()
    }

    class ReviewViewHolder(
        private val binding: ItemQuizReviewQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var exoPlayer: ExoPlayer? = null

        fun bind(item: ReviewItem) {
            val context = binding.root.context
            binding.tvReviewQuestionNum.text = context.getString(R.string.question_number_fmt, item.questionNumber)
            binding.tvReviewQuestionText.text = HtmlCompat.fromHtml(
                item.question.questionText,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.tvReviewMarkedBadge.visibility = if (item.userState.isMarkedForReview) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Bind Media Content for Review
            bindReviewMedia(item.question.mediaType, item.question.mediaUrl)

            binding.layoutOptionsContainer.removeAllViews()

            item.question.options.forEachIndexed { optIdx: Int, optText: String ->
                val optionView = LayoutInflater.from(context).inflate(
                    R.layout.item_quiz_review_option_row,
                    binding.layoutOptionsContainer,
                    false
                ) as TextView

                val prefix = ('A' + optIdx.toChar().code).toString()
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

        private fun bindReviewMedia(mediaType: MediaType, mediaUrl: String?) {
            if (mediaUrl.isNullOrBlank() || mediaType == MediaType.NONE) {
                binding.frameReviewMediaContainer.visibility = View.GONE
                releaseMedia()
                return
            }

            binding.frameReviewMediaContainer.visibility = View.VISIBLE

            when (mediaType) {
                MediaType.IMAGE -> {
                    releaseMedia()
                    binding.playerViewReview.visibility = View.GONE
                    binding.ivReviewImage.visibility = View.VISIBLE

                    binding.ivReviewImage.load(mediaUrl) {
                        crossfade(true)
                        scale(Scale.FIT)
                        placeholder(R.drawable.ic_placeholder_image)
                        error(R.drawable.ic_placeholder_image)
                    }
                }
                MediaType.VIDEO -> {
                    binding.ivReviewImage.visibility = View.GONE
                    binding.playerViewReview.visibility = View.VISIBLE

                    releaseMedia()
                    val player = ExoPlayer.Builder(binding.root.context).build()
                    exoPlayer = player
                    binding.playerViewReview.player = player
                    player.setMediaItem(MediaItem.fromUri(mediaUrl))
                    player.prepare()
                }
            }
        }

        fun releaseMedia() {
            binding.playerViewReview.player = null
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ReviewItem>() {
        override fun areItemsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean {
            return oldItem.questionNumber == newItem.questionNumber
        }

        override fun areContentsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean {
            return oldItem == newItem
        }
    }
}