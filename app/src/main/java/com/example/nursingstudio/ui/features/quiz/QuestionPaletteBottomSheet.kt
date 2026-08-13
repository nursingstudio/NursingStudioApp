package com.example.nursingstudio.ui.features.quiz

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.nursingstudio.data.model.QuestionPaletteItem
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

        val paletteItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_PALETTE_ITEMS, QuestionPaletteItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList(ARG_PALETTE_ITEMS)
        } ?: emptyList()

        val paletteAdapter = QuestionPaletteAdapter { selectedIndex ->
            onQuestionSelectedListener?.invoke(selectedIndex)
            dismiss()
        }

        binding.rvQuestionGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = paletteAdapter
        }

        paletteAdapter.submitList(paletteItems)
    }

    fun setOnQuestionSelectedListener(listener: (Int) -> Unit) {
        onQuestionSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PALETTE_ITEMS = "arg_palette_items"

        fun newInstance(
            paletteItems: List<QuestionPaletteItem>,
            onQuestionSelected: (Int) -> Unit
        ): QuestionPaletteBottomSheet {
            val fragment = QuestionPaletteBottomSheet()
            fragment.arguments = Bundle().apply {
                putParcelableArrayList(ARG_PALETTE_ITEMS, ArrayList(paletteItems))
            }
            fragment.setOnQuestionSelectedListener(onQuestionSelected)
            return fragment
        }
    }
}