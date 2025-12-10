package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class StaticPageFragment : Fragment() {

    companion object {
        private const val ARG_PAGE_TYPE = "page_type"

        fun newInstance(pageType: String): StaticPageFragment {
            val frag = StaticPageFragment()
            val args = Bundle()
            args.putString(ARG_PAGE_TYPE, pageType)
            frag.arguments = args
            return frag
        }
    }

    private var pageType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageType = arguments?.getString(ARG_PAGE_TYPE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_static_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvPageTitle)
        val tvBody = view.findViewById<TextView>(R.id.tvPageBody)

        when (pageType) {
            "privacy" -> {
                tvTitle.text = "Privacy Policy"
                tvBody.text = """
                    • We store your basic profile details (name, email, mobile) only to manage your Nursing Studio account.
                    • Your test performance data is used to show analytics and improve your learning.
                    • We do not sell your personal data to any third party.
                    • Some analytics / crash tools may automatically collect anonymous usage data.
                    
                    This is a sample text. Later, you can paste your proper Privacy Policy here or load it from a web link.
                """.trimIndent()
            }

            "terms" -> {
                tvTitle.text = "Terms of Use"
                tvBody.text = """
                    • Nursing Studio is designed to support preparation for nursing competitive exams.
                    • Content (tests, PDFs, videos) may not be 100% error-free; always cross-check with official sources.
                    • Do not share your login with others or misuse paid features.
                    • By using the app, you agree to use it only for lawful and ethical learning purposes.
                    
                    This is sample Terms of Use content. Replace with your proper Terms later.
                """.trimIndent()
            }

            "disclaimer" -> {
                tvTitle.text = "Disclaimer"
                tvBody.text = """
                    • This app is not an official app of any government body or exam board.
                    • Question papers, explanations, and notes are for practice and guidance only.
                    • Final selection always depends on official exams and notifications.
                    
                    This is sample Disclaimer text. You can customize it as per your need.
                """.trimIndent()
            }

            "about" -> {
                tvTitle.text = "About Nursing Studio"
                tvBody.text = """
                    Nursing Studio is designed to bring Online Test Series, PDFs and videos together for nursing competitive exams. 
                    
                    Key points:
                    • Topic-wise and full-length tests (future updates)
                    • Study material designed for staff nurse / nursing officer exams
                    • Built by a nursing professional for nursing students
                    
                    You can modify this text later with your story, mission and vision.
                """.trimIndent()
            }

            else -> {
                tvTitle.text = "Information"
                tvBody.text = "No content available."
            }
        }
    }
}
