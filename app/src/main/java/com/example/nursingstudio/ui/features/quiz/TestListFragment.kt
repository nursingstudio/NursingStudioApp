package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nursingstudio.databinding.FragmentTestListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TestListFragment : Fragment() {

    private var _binding: FragmentTestListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TestListViewModel by viewModels()
    private lateinit var adapter: TestListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryId = arguments?.getString("category_id") ?: ""

        setupRecyclerView()
        observeViewModel()

        if (categoryId.isNotEmpty()) {
            viewModel.fetchTestsForCategory(categoryId)
        } else {
            binding.tvEmptyMessage.text = "Category ID missing"
            binding.tvEmptyMessage.isVisible = true
        }
    }

    private fun setupRecyclerView() {
        adapter = TestListAdapter { selectedTest ->
            if (selectedTest.isLocked && !selectedTest.isFree) {
                Toast.makeText(requireContext(), "This test is locked. Purchase subscription to unlock.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Opening Instructions for: ${selectedTest.title}", Toast.LENGTH_SHORT).show()
                // Next step: Open Instructions Fragment here
            }
        }
        binding.rvTestList.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TestListUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.rvTestList.isVisible = false
                            binding.tvEmptyMessage.isVisible = false
                        }
                        is TestListUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.rvTestList.isVisible = true
                            binding.tvEmptyMessage.isVisible = false
                            adapter.submitList(state.tests)
                        }
                        is TestListUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.rvTestList.isVisible = false
                            binding.tvEmptyMessage.text = state.message
                            binding.tvEmptyMessage.isVisible = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}