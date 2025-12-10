package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

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

        // Full syllabus
        view.findViewById<View?>(R.id.cardFullSyllabus)?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Full Syllabus Test Series – coming soon 🚧",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Subject-wise
        view.findViewById<View?>(R.id.cardSubjectWise)?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Subject-wise tests – coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Topic-wise
        view.findViewById<View?>(R.id.cardTopicWise)?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Topic-wise practice – coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Previous year
        view.findViewById<View?>(R.id.cardPreviousYear)?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Previous year papers – coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Quick practice
        view.findViewById<View?>(R.id.cardQuickPractice)?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Quick practice tests – coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
