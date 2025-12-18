package com.example.nursingstudio

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val cardTest = view.findViewById<MaterialCardView>(R.id.cardTest)
        val cardPdf = view.findViewById<MaterialCardView>(R.id.cardPdf)
        val cardVideo = view.findViewById<MaterialCardView>(R.id.cardVideo)
        val cardProgress = view.findViewById<MaterialCardView>(R.id.cardProgress)

        cardTest.setOnClickListener {
            openFragment(QuizFragment())
        }

        cardPdf.setOnClickListener {
            openFragment(PdfFragment())
        }

        cardVideo.setOnClickListener {
            openFragment(VideoFragment())
        }

        cardProgress.setOnClickListener {
            openFragment(MyPageFragment())
        }

        return view
    }

    private fun openFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)

        val session = requireActivity()
            .getSharedPreferences("session", Context.MODE_PRIVATE)

        val name = session.getString("reg_name", "User")

        tvWelcome.text = getString(R.string.welcome_user, name)
    }

}
