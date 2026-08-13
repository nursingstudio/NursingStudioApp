package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentTestListBinding
import com.example.nursingstudio.utils.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TestListFragment : Fragment() {

    private var _binding: FragmentTestListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TestListViewModel by viewModels()
    private lateinit var testListAdapter: TestListAdapter

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

        val categoryId = arguments?.getString("category_id") ?: "category_full_syllabus"

        setupRecyclerView()
        observeViewModel()

        viewModel.loadTests(categoryId)
    }

    private fun setupRecyclerView() {
        testListAdapter = TestListAdapter { testItem ->
            if (testItem.isLocked) {
                Toast.makeText(requireContext(), "This test is locked. Upgrade to unlock!", Toast.LENGTH_SHORT).show()
            } else {
                val args = Bundle().apply {
                    putString("quiz_id", testItem.testId)
                }
                findNavController().safeNavigate(
                    currentDestinationId = R.id.nav_test_list,
                    actionId = R.id.action_nav_test_list_to_nav_quiz_engine,
                    args = args
                )
            }
        }

        binding.rvTestList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = testListAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TestListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is TestListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            testListAdapter.submitList(state.tests)
                        }
                        is TestListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
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