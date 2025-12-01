package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SubscriptionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscription, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvCurrentPlan = view.findViewById<TextView>(R.id.tvCurrentPlan)
        val btnSelectFree = view.findViewById<Button>(R.id.btnSelectFree)
        val btnSelectPremium = view.findViewById<Button>(R.id.btnSelectPremium)

        val sp = requireContext().getSharedPreferences("session", 0)
        val currentType = sp.getString("subscription_type", "Free")

        tvCurrentPlan.text = "Current plan: $currentType"

        // Optional: yahan ek baar sync kara diya header ko
        (activity as? MainActivity)?.updateDrawerHeader()

        btnSelectFree.setOnClickListener {
            sp.edit().putString("subscription_type", "Free").apply()
            tvCurrentPlan.text = "Current plan: Free"
            Toast.makeText(requireContext(), "Free plan selected", Toast.LENGTH_SHORT).show()

            // 🔹 NAYA: Drawer header turant refresh
            (activity as? MainActivity)?.updateDrawerHeader()
        }

        btnSelectPremium.setOnClickListener {
            // Abhi demo: sirf local flag change kar rahe hain
            sp.edit().putString("subscription_type", "Premium").apply()
            tvCurrentPlan.text = "Current plan: Premium"
            Toast.makeText(requireContext(), "Premium plan selected (demo)", Toast.LENGTH_SHORT).show()

            // 🔹 NAYA: Drawer header turant refresh
            (activity as? MainActivity)?.updateDrawerHeader()
        }
    }
}
