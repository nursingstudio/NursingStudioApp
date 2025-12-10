package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class PdfFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardImportant = view.findViewById<MaterialCardView>(R.id.cardImportantPdf)
        val cardTopic = view.findViewById<MaterialCardView>(R.id.cardTopicPdf)
        val cardPapers = view.findViewById<MaterialCardView>(R.id.cardPaperPdf)

        cardImportant.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Important PDFs section coming soon (next phase).",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardTopic.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Topic-wise notes PDFs coming soon.",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardPapers.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Previous year papers PDFs coming soon.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
