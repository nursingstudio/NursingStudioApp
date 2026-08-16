package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.DialogTestSubmitBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TestSubmitBottomSheetFragment(
    private val total: Int,
    private val answered: Int,
    private val unanswered: Int,
    private val review: Int,
    private val timeFormatted: String,
    private val onSubmitConfirmed: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogTestSubmitBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTestSubmitBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvStatTotalVal.text = total.toString()
        binding.tvStatAnsweredVal.text = answered.toString()
        binding.tvStatUnansweredVal.text = unanswered.toString()
        binding.tvStatReviewVal.text = review.toString()
        binding.tvSheetTimeRemaining.text = getString(R.string.time_left_label, timeFormatted)

        binding.btnConfirmSubmit.setOnClickListener {
            dismiss()
            onSubmitConfirmed()
        }

        binding.btnResumeTest.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}