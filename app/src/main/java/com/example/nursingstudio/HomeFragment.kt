package com.example.nursingstudio

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.random.Random

class HomeFragment : Fragment() {

    companion object {
        private const val PREF_SESSION = "session"
        private const val PREF_SETTINGS = "settings_prefs"
        private const val KEY_MOTIVATION = "enable_motivation"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        val layoutMotivationCard = view.findViewById<LinearLayout>(R.id.layoutMotivationCard)
        val tvMotivationTitle = view.findViewById<TextView>(R.id.tvMotivationTitle)
        val tvMotivationText = view.findViewById<TextView>(R.id.tvMotivationText)

        // Name session se
        val sessionSp = requireContext().getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
        val name = sessionSp.getString("reg_name", "Nursing Student")

        tvGreeting.text = "Welcome, $name"
        tvSubtitle.text = "Start smart preparation for your nursing exams."

        // Settings se motivation toggle read
        val settingsSp =
            requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        val motivationOn = settingsSp.getBoolean(KEY_MOTIVATION, true)

        if (!motivationOn) {
            // Agar toggle OFF ho to card hide
            layoutMotivationCard.visibility = View.GONE
        } else {
            layoutMotivationCard.visibility = View.VISIBLE

            // Simple list of motivational quotes
            val quotes = listOf(
                "Every MCQ you solve takes you one step closer to your posting.",
                "Study like a nurse in training, think like a staff nurse on duty.",
                "Small consistent study today, big results in your exam.",
                "Don’t wait for the right time. This page itself is your starting point.",
                "You are not just clearing an exam, you are building a nursing career."
            )

            val randomQuote = quotes[Random.nextInt(quotes.size)]
            tvMotivationTitle.text = "Daily Motivation"
            tvMotivationText.text = randomQuote
        }
    }
}
