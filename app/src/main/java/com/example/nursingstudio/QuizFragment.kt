package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class QuizFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cards find karo
        val cardSubjectTests =
            view.findViewById<MaterialCardView>(R.id.cardSubjectTests)
        val cardMockTests =
            view.findViewById<MaterialCardView>(R.id.cardMockTests)
        val cardPreviousPapers =
            view.findViewById<MaterialCardView>(R.id.cardPreviousPapers)

        // Abhi ke liye sirf Coming soon – baad me yahi se actual quiz screens open karenge
        cardSubjectTests.setOnClickListener {
            Toast.makeText(requireContext(),
                "Subject-wise tests coming soon 🔄",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardMockTests.setOnClickListener {
            Toast.makeText(requireContext(),
                "Full mock tests coming soon 🔄",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardPreviousPapers.setOnClickListener {
            Toast.makeText(requireContext(),
                "Previous year papers coming soon 🔄",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
