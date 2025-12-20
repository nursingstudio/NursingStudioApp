package com.example.nursingstudio

import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class TermsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_terms, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val web = view.findViewById<WebView>(R.id.webTerms)
        web.settings.javaScriptEnabled = false

        val html = """
            <html>
            <body style="padding:16px; font-family:sans-serif;">
                <h2>Terms & Conditions</h2>
                <p>
                By using Nursing Studio, you agree that all information provided by you is true and accurate.
                This app is intended for educational purposes only.
                We do not guarantee exam results or job placement.
                Misuse of content is strictly prohibited.
                </p>

                <h2>Privacy Policy</h2>
                <p>
                We respect your privacy.
                Your personal data such as name, mobile number, email and profile details
                are stored securely and are never sold to third parties.
                OTP verification is used only for authentication.
                </p>

                <p>
                By continuing, you acknowledge that you have read and understood
                our Terms & Conditions and Privacy Policy.
                </p>
            </body>
            </html>
        """.trimIndent()

        // 🔥 OPTION 2 (BEST): Online hosted legal page
        web.loadUrl("https://yourdomain.com/terms")

        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }
}
