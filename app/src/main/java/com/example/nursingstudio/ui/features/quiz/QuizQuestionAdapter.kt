package com.example.nursingstudio.ui.features.quiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.databinding.ItemQuizQuestionBinding

class QuizQuestionAdapter(
    private val onOptionSelected: (questionIndex: Int, optionIndex: Int) -> Unit
) : ListAdapter<QuestionItem, QuizQuestionAdapter.QuestionViewHolder>(QuestionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuizQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: QuestionViewHolder) {
        super.onViewRecycled(holder)
        holder.releasePlayer()
    }

    inner class QuestionViewHolder(
        private val binding: ItemQuizQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var player: ExoPlayer? = null

        fun bind(item: QuestionItem) {
            val context = binding.root.context

            // Fixed Lint Warning: Using localized string format resource
            binding.tvQuestionText.text = context.getString(
                R.string.quiz_question_format,
                item.questionIndex,
                item.questionText
            )

            // Handle Dynamic Media Content with Optimized Memory Pipeline
            when (item.mediaType.uppercase()) {
                "IMAGE" -> {
                    binding.ivQuestionImage.visibility = View.VISIBLE
                    binding.pvQuestionVideo.visibility = View.GONE
                    releasePlayer()

                    // Fixed Unresolved Reference & Added Disk Caching
                    Glide.with(context)
                        .load(item.mediaUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_placeholder_image)
                        .into(binding.ivQuestionImage)
                }
                "VIDEO" -> {
                    binding.ivQuestionImage.visibility = View.GONE
                    binding.pvQuestionVideo.visibility = View.VISIBLE
                    setupVideoPlayer(item.mediaUrl)
                }
                else -> {
                    binding.ivQuestionImage.visibility = View.GONE
                    binding.pvQuestionVideo.visibility = View.GONE
                    releasePlayer()
                }
            }

            // Bind Options Text
            val options = item.options
            binding.tvOptionA.text = options.getOrNull(0) ?: ""
            binding.tvOptionB.text = options.getOrNull(1) ?: ""
            binding.tvOptionC.text = options.getOrNull(2) ?: ""
            binding.tvOptionD.text = options.getOrNull(3) ?: ""

            // Highlight Selected State
            updateOptionSelectionStates(item.selectedOptionIndex)

            // Setup Click Listeners
            binding.cardOptionA.setOnClickListener { onOptionClick(item.questionIndex - 1, 0) }
            binding.cardOptionB.setOnClickListener { onOptionClick(item.questionIndex - 1, 1) }
            binding.cardOptionC.setOnClickListener { onOptionClick(item.questionIndex - 1, 2) }
            binding.cardOptionD.setOnClickListener { onOptionClick(item.questionIndex - 1, 3) }
        }

        private fun onOptionClick(questionPos: Int, optionIndex: Int) {
            updateOptionSelectionStates(optionIndex)
            onOptionSelected(questionPos, optionIndex)
        }

        private fun updateOptionSelectionStates(selectedIndex: Int) {
            val context = binding.root.context
            val selectedColor = ContextCompat.getColor(context, R.color.brand_saffron)
            val defaultColor = ContextCompat.getColor(context, R.color.border_grey)
            val selectedBg = ContextCompat.getColor(context, R.color.saffron_light)
            val defaultBg = ContextCompat.getColor(context, R.color.surface_card)

            val cards = listOf(
                binding.cardOptionA,
                binding.cardOptionB,
                binding.cardOptionC,
                binding.cardOptionD
            )

            cards.forEachIndexed { index, card ->
                if (index == selectedIndex) {
                    card.strokeColor = selectedColor
                    card.strokeWidth = 4
                    card.setCardBackgroundColor(selectedBg)
                } else {
                    card.strokeColor = defaultColor
                    card.strokeWidth = 2
                    card.setCardBackgroundColor(defaultBg)
                }
            }
        }

        private fun setupVideoPlayer(url: String) {
            if (url.isEmpty()) return
            releasePlayer()
            player = ExoPlayer.Builder(binding.root.context).build().apply {
                setMediaItem(MediaItem.fromUri(url.toUri()))
                prepare()
                playWhenReady = false
            }
            binding.pvQuestionVideo.player = player
        }

        fun releasePlayer() {
            player?.stop()
            player?.release()
            player = null
            binding.pvQuestionVideo.player = null
        }
    }

    private class QuestionDiffCallback : DiffUtil.ItemCallback<QuestionItem>() {
        override fun areItemsTheSame(oldItem: QuestionItem, newItem: QuestionItem): Boolean {
            return oldItem.questionId == newItem.questionId
        }

        override fun areContentsTheSame(oldItem: QuestionItem, newItem: QuestionItem): Boolean {
            return oldItem == newItem
        }
    }
}