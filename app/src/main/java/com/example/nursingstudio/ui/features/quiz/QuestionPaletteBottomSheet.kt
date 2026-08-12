package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.nursingstudio.data.model.QuestionPaletteItem
import com.example.nursingstudio.data.model.QuestionStatus
import com.example.nursingstudio.databinding.BottomSheetQuestionPaletteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuestionPaletteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQuestionPaletteBinding? = null
    private val binding get() = _binding!!

    private var onQuestionSelectedListener: ((Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQuestionPaletteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val totalQuestions = arguments?.getInt(ARG_TOTAL_QUESTIONS, 0) ?: 0
        val currentPosition = arguments?.getInt(ARG_CURRENT_POSITION, 0) ?: 0

        val paletteAdapter = QuestionPaletteAdapter { selectedIndex ->
            onQuestionSelectedListener?.invoke(selectedIndex)
            dismiss()
        }

        binding.rvQuestionGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = paletteAdapter
        }

        // Generate Palette Items (Using exact 0-based to 1-based mapping)
        val paletteList = (0 until totalQuestions).map { index ->
            QuestionPaletteItem(
                questionIndex = index,
                displayIndex = index + 1,
                status = QuestionStatus.UNANSWERED, // Dynamic binding from ViewModel state
                isCurrent = (index == currentPosition)
            )
        }

        paletteAdapter.submitList(paletteList)
    }

    fun setOnQuestionSelectedListener(listener: (Int) -> Unit) {
        onQuestionSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TOTAL_QUESTIONS = "arg_total_questions"
        private const val ARG_CURRENT_POSITION = "arg_current_position"

        fun newInstance(
            totalQuestions: Int,
            currentPosition: Int,
            onQuestionSelected: (Int) -> Unit
        ): QuestionPaletteBottomSheet {
            val fragment = QuestionPaletteBottomSheet()
            fragment.arguments = Bundle().apply {
                putInt(ARG_TOTAL_QUESTIONS, totalQuestions)
                putInt(ARG_CURRENT_POSITION, currentPosition)
            }
            fragment.setOnQuestionSelectedListener(onQuestionSelected)
            return fragment
        }
    }
}