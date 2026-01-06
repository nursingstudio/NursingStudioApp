package com.example.nursingstudio

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.nursingstudio.profile.ProfileFragment
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Cards
        val cardTest = view.findViewById<MaterialCardView>(R.id.cardTest)
        val cardPdf = view.findViewById<MaterialCardView>(R.id.cardPdf)
        val cardVideo = view.findViewById<MaterialCardView>(R.id.cardVideo)
        val cardProgress = view.findViewById<MaterialCardView>(R.id.cardProgress)

        cardTest.setOnClickListener {
            ProgressManager.increment(requireContext(), "test_attempted")
            openFragment(QuizFragment())
        }

        cardPdf.setOnClickListener {
            ProgressManager.increment(requireContext(), "pdf_opened")
            openFragment(PdfFragment())
        }

        cardVideo.setOnClickListener {
            ProgressManager.increment(requireContext(), "video_watched")
            openFragment(VideoFragment())
        }

        cardProgress.setOnClickListener {
            openFragment(ProfileFragment())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Welcome text
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val session = requireActivity()
            .getSharedPreferences("session", Context.MODE_PRIVATE)

        val name = session.getString("reg_name", "User")
        tvWelcome.text = getString(R.string.welcome_user, name)

        // Daily motivation
        setupDailyMotivation(view)
    }

    private fun setupDailyMotivation(view: View) {
        val tvMotivation = view.findViewById<TextView>(R.id.tvMotivation)

        val quotes = listOf(
            "Consistency beats intensity.",
            "Small steps daily create big success.",
            "Today’s effort is tomorrow’s result.",
            "Study smart, not just hard.",
            "Discipline today, success tomorrow."
        )

        val sp = requireContext()
            .getSharedPreferences("daily_motivation", Context.MODE_PRIVATE)

        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val savedDate = sp.getString("date", "")
        val savedQuote = sp.getString("quote", "")

        if (today == savedDate && !savedQuote.isNullOrEmpty()) {
            tvMotivation.text = savedQuote
        } else {
            val newQuote = quotes.random()
            tvMotivation.text = newQuote

            sp.edit()
                .putString("date", today)
                .putString("quote", newQuote)
                .apply()
        }
    }

    private fun openFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
