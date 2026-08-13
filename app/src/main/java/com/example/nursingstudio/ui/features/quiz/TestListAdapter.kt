package com.example.nursingstudio.ui.features.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.QuizTestItem
import com.example.nursingstudio.databinding.ItemTestBinding

class TestListAdapter(
    private val onTestClick: (QuizTestItem) -> Unit
) : ListAdapter<QuizTestItem, TestListAdapter.TestViewHolder>(TestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val binding = ItemTestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TestViewHolder(
        private val binding: ItemTestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuizTestItem) {
            val context = binding.root.context
            binding.tvTestTitle.text = item.title
            binding.tvTestDetails.text = context.getString(
                R.string.test_details_format,
                item.totalQuestions,
                item.durationMinutes
            )

            if (item.isLocked) {
                binding.ivLockStatus.setImageResource(R.drawable.ic_lock)
                binding.ivLockStatus.setColorFilter(ContextCompat.getColor(context, R.color.text_muted))
            } else {
                binding.ivLockStatus.setImageResource(R.drawable.ic_unlock)
                binding.ivLockStatus.setColorFilter(ContextCompat.getColor(context, R.color.brand_saffron))
            }

            binding.root.setOnClickListener {
                onTestClick(item)
            }
        }
    }

    private class TestDiffCallback : DiffUtil.ItemCallback<QuizTestItem>() {
        override fun areItemsTheSame(oldItem: QuizTestItem, newItem: QuizTestItem): Boolean {
            return oldItem.testId == newItem.testId
        }

        override fun areContentsTheSame(oldItem: QuizTestItem, newItem: QuizTestItem): Boolean {
            return oldItem == newItem
        }
    }
}