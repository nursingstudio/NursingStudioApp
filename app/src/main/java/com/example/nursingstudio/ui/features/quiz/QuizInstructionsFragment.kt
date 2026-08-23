package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentQuizInstructionsBinding

class QuizInstructionsFragment : Fragment() {

    private var _binding: FragmentQuizInstructionsBinding? = null
    private val binding get() = _binding!!

    private var testId: String = ""
    private var testTitle: String = ""
    private var hasScrolledToBottom: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizInstructionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Arguments Extraction (NavArgs Safe Extract)
        arguments?.let { args ->
            testId = args.getString("testId", "")
            testTitle = args.getString("title", "Test Instructions")
        }

        if (testTitle.isNotEmpty()) {
            binding.tvInstructionTitle.text = testTitle
        }

        // Standard Default Instructions Text Injection
        setupDefaultInstructionsText()

        // 2. Setup Dynamic Scroll Listener & Edge-Case Detection
        setupScrollAndLockLogic()

        // 3. Checkbox Listener -> Button State Control
        binding.cbAgreeTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnStartQuiz.isEnabled = isChecked
        }

        // 4. Start Quiz Action Navigation Handler
        // 4. Start Quiz Action Navigation Handler (Passes testId AND title)
        binding.btnStartQuiz.setOnClickListener {
            val bundle = Bundle().apply {
                putString("testId", testId)
                putString("title", testTitle) // FIXED: Passing exact title forward
            }
            findNavController().navigate(
                R.id.action_quizInstructionsFragment_to_quizEngineFragment,
                bundle
            )
        }
    }

    private fun setupScrollAndLockLogic() {
        // Auto-Check if text is shorter than screen height
        binding.scrollViewInstructions.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.scrollViewInstructions.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val child = binding.scrollViewInstructions.getChildAt(0)
                    if (child != null) {
                        val contentHeight = child.height
                        val scrollHeight = binding.scrollViewInstructions.height
                        if (contentHeight <= scrollHeight) {
                            // Text fits inside container without needing to scroll
                            unlockCheckbox()
                        }
                    }
                }
            }
        )

        // Real-time Scroll Listener
        binding.scrollViewInstructions.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (hasScrolledToBottom) return@setOnScrollChangeListener

            val child = binding.scrollViewInstructions.getChildAt(0)
            if (child != null) {
                val diff = child.bottom - (binding.scrollViewInstructions.height + scrollY)
                // Threshold tolerance offset of 20px
                if (diff <= 20) {
                    unlockCheckbox()
                }
            }
        }
    }

    private fun unlockCheckbox() {
        if (!hasScrolledToBottom) {
            hasScrolledToBottom = true
            binding.cbAgreeTerms.isEnabled = true
        }
    }

    private fun setupDefaultInstructionsText() {
        val rawHtml = getString(R.string.quiz_default_instructions)
        // 🚀 2026 Gold Standard HTML Text Rendering for Spanned Formatting
        binding.tvInstructionsContent.text = androidx.core.text.HtmlCompat.fromHtml(
            rawHtml,
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}