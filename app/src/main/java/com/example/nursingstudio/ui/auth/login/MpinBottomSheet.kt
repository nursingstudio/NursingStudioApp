package com.example.nursingstudio.ui.auth.login

import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutMpinKeypadBinding
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MpinBottomSheet(
    private val onMpinSuccess: (email: String?, pass: String?) -> Unit,
    private val onBiometricRequest: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutMpinKeypadBinding? = null
    private val binding get() = _binding!!
    private var enteredMpin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Gold Standard: Transparent background for custom glass effect
        setStyle(STYLE_NORMAL, R.style.GlassBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutMpinKeypadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGlassEffect()
        setupKeypad()
    }

    private fun setupGlassEffect() {
        dialog?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.attributes.blurBehindRadius = 30
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }

    private fun setupKeypad() {
        val bioManager = BiometricSettingsManager(requireContext())
        val correctMpin = bioManager.getMPIN()

        val handleKeyClick: (String) -> Unit = { key ->
            if (enteredMpin.length < 4) {
                enteredMpin += key
                updateMpinDots(enteredMpin.length)

                if (enteredMpin.length == 4) {
                    if (enteredMpin == correctMpin) {
                        val email = bioManager.getSavedEmail()
                        val pass = bioManager.getSavedPass()
                        onMpinSuccess(email, pass)
                        dismiss()
                    } else {
                        // Wrong MPIN: Shake effect and vibration
                        AppSettings.triggerErrorEffect(requireContext(), binding.layoutMpinDots)
                        enteredMpin = ""
                        binding.root.postDelayed({ updateMpinDots(0) }, 300)
                        Toast.makeText(context, "Incorrect MPIN!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Map Buttons
        val keyMap = mapOf(
            binding.btnKey1 to "1", binding.btnKey2 to "2", binding.btnKey3 to "3",
            binding.btnKey4 to "4", binding.btnKey5 to "5", binding.btnKey6 to "6",
            binding.btnKey7 to "7", binding.btnKey8 to "8", binding.btnKey9 to "9",
            binding.btnKey0 to "0"
        )

        keyMap.forEach { (btn, value) -> btn.setOnClickListener { handleKeyClick(value) } }

        binding.btnKeyDel.setOnClickListener {
            if (enteredMpin.isNotEmpty()) {
                enteredMpin = enteredMpin.dropLast(1)
                updateMpinDots(enteredMpin.length)
            }
        }

        binding.btnKeyDel.setOnLongClickListener {
            enteredMpin = ""
            updateMpinDots(0)
            AppSettings.triggerVibration(requireContext(), 50)
            true
        }

        binding.btnKeyBio.setOnClickListener {
            dismiss()
            onBiometricRequest()
        }
    }

    private fun updateMpinDots(length: Int) {
        val dotsLayout = binding.layoutMpinDots
        for (i in 0 until dotsLayout.childCount) {
            val dot = dotsLayout.getChildAt(i)
            if (i < length) {
                dot.setBackgroundResource(R.drawable.mpin_dot_filled)
                // Gold Standard: Small scale animation on each dot
                dot.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction {
                    dot.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()
            } else {
                dot.setBackgroundResource(R.drawable.mpin_dot_empty)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}