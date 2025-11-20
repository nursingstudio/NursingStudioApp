package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mypage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvMobile = view.findViewById<TextView>(R.id.tvMobile)
        val tvCategory = view.findViewById<TextView>(R.id.tvCategory)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)


        val sp = requireContext().getSharedPreferences("session", 0)
        val name = sp.getString("reg_name", "User")
        val mobile = sp.getString("reg_mobile", "Not set")
        // future me category yahin se laayenge
        val category = sp.getString("reg_category", "(Coming soon)")

        tvWelcome.text = "Welcome,"
        tvName.text = name
        tvMobile.text = mobile
        tvCategory.text = category

        btnLogout.setOnClickListener {
            val sp = requireContext().getSharedPreferences("session", 0)
            sp.edit()
                .putBoolean("logged_in", false)
                .apply()

            // AuthActivity khol do, current activity khatam
            val intent = android.content.Intent(requireContext(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

    }
}
