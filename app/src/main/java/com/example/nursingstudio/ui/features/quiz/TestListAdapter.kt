package com.example.nursingstudio.ui.features.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.TestItem
import com.example.nursingstudio.databinding.ItemTestBinding

class TestListAdapter(
    private val onTestClicked: (TestItem) -> Unit
) : ListAdapter<TestItem, TestListAdapter.TestViewHolder>(DiffCallback) {

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

    inner class TestViewHolder(private val binding: ItemTestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TestItem) {
            binding.tvTestTitle.text = item.title
            binding.tvTestSubtitle.text = "${item.totalQuestions} Questions • ${item.totalDurationMinutes} Mins"

            // Handle Dynamic Lock/Unlock Drawables
            if (item.isFree || !item.isLocked) {
                binding.ivLockStatus.setImageResource(R.drawable.ic_lock_open)
            } else {
                binding.ivLockStatus.setImageResource(R.drawable.ic_lock)
            }

            binding.root.setOnClickListener {
                onTestClicked(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TestItem>() {
        override fun areItemsTheSame(oldItem: TestItem, newItem: TestItem): Boolean {
            return oldItem.testId == newItem.testId
        }

        override fun areContentsTheSame(oldItem: TestItem, newItem: TestItem): Boolean {
            return oldItem == newItem
        }
    }
}