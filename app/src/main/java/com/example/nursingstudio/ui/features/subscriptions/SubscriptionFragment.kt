package com.example.nursingstudio.ui.features.subscriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import kotlinx.coroutines.launch

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

        val dataStoreManager = DataStoreManager(requireContext())
        val tvCurrentPlan = view.findViewById<TextView>(R.id.tvCurrentPlan)
        val btnSelectFree = view.findViewById<Button>(R.id.btnSelectFree)
        val btnSelectPremium = view.findViewById<Button>(R.id.btnSelectPremium)

        // 1. Point-to-Point: Current Plan ko observe karna (Gold Standard 2026)
        // Jaise hi DataStore mein badlav hoga, ye text apne aap change ho jayega
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStoreManager.subscriptionType.collect { type ->
                    tvCurrentPlan.text = "Current plan: $type"
                }
            }
        }

        // 2. Select Free Plan Logic
        btnSelectFree.setOnClickListener {
            lifecycleScope.launch {
                dataStoreManager.saveSubscription("Free")
                Toast.makeText(requireContext(), "Free plan selected", Toast.LENGTH_SHORT).show()
                // Note: Ab humein activity.updateDrawerHeader() ki zarurat nahi hai,
                // kyunki MainActivity DataStore ko observe kar rahi hai.
            }
        }

        // 3. Select Premium Plan Logic
        btnSelectPremium.setOnClickListener {
            lifecycleScope.launch {
                dataStoreManager.saveSubscription("Premium")
                Toast.makeText(requireContext(), "Premium plan selected! 🌟", Toast.LENGTH_SHORT).show()
            }
        }
    }
}