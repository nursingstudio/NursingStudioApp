package com.example.nursingstudio.ui.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutMpinKeypadBinding
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class MpinBottomSheet(
    private val onMpinSuccess: (email: String?, pass: String?) -> Unit,
    private val onBiometricRequest: () -> Unit,
    private val onForgotMpinRequested: () -> Unit // 🚀 2026 Standard Event Parameter Injection
) : BottomSheetDialogFragment() {

    private var _binding: LayoutMpinKeypadBinding? = null
    private val binding get() = _binding!!
    private var enteredMpin = ""

    private val numericButtons = ArrayList<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.GlassBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutMpinKeypadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGlassEffect()
        initializeNumericButtonsList()
        shuffleAndPopulateKeypad()
        setupKeypadControllers()
    }

    private fun setupGlassEffect() {
        dialog?.window?.let { window ->
            window.attributes.blurBehindRadius = 30
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
    }

    private fun initializeNumericButtonsList() {
        numericButtons.clear()
        numericButtons.add(binding.btnKey1)
        numericButtons.add(binding.btnKey2)
        numericButtons.add(binding.btnKey3)
        numericButtons.add(binding.btnKey4)
        numericButtons.add(binding.btnKey5)
        numericButtons.add(binding.btnKey6)
        numericButtons.add(binding.btnKey7)
        numericButtons.add(binding.btnKey8)
        numericButtons.add(binding.btnKey9)
        numericButtons.add(binding.btnKey0)
    }

    private fun shuffleAndPopulateKeypad() {
        val digitsList = arrayListOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        digitsList.shuffle()

        for (i in numericButtons.indices) {
            numericButtons[i].text = digitsList[i]
            numericButtons[i].tag = digitsList[i]
        }
    }

    private fun setupKeypadControllers() {
        val bioManager = BiometricSettingsManager(requireContext())
        val correctMpin = bioManager.getMPIN()

        val handleKeyClick: (String) -> Unit = { digit ->
            if (enteredMpin.length < 4) {
                enteredMpin += digit
                updateMpinDots(enteredMpin.length)
            }
        }

        numericButtons.forEach { button ->
            button.setOnClickListener {
                val numericValue = button.tag.toString()
                handleKeyClick(numericValue)
            }
        }

        binding.btnKeyBackspace.setOnClickListener {
            if (enteredMpin.isNotEmpty()) {
                enteredMpin = enteredMpin.dropLast(1)
                updateMpinDots(enteredMpin.length)
            }
        }

        binding.btnKeyBackspace.setOnLongClickListener {
            enteredMpin = ""
            updateMpinDots(0)
            AppSettings.triggerVibration(requireContext(), 50)
            true
        }

        binding.btnKeyEnter.setOnClickListener {
            when {
                enteredMpin.length < 4 -> {
                    Toast.makeText(context, "Please enter 4-digit MPIN", Toast.LENGTH_SHORT).show()
                }
                enteredMpin == correctMpin -> {
                    val email = bioManager.getSavedEmail()
                    val pass = bioManager.getSavedPass()
                    onMpinSuccess(email, pass)
                    dismiss()
                }
                else -> {
                    AppSettings.triggerErrorEffect(requireContext(), binding.layoutMpinDots)
                    enteredMpin = ""
                    binding.root.postDelayed({ updateMpinDots(0) }, 300)
                    Toast.makeText(context, "Incorrect MPIN!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnUseBiometrics.setOnClickListener {
            dismiss()
            onBiometricRequest()
        }

        /**
         * Exact Location: End of setupKeypadControllers framework scope
         * 🚀 2026 Inter-Sheet Communication Bridge Execution
         * Maps layout design button directly to navigation routing callbacks safely.
         */
        binding.btnForgotMpin.setOnClickListener {
            dismiss() // Safe dismiss to prevent bottom-sheet layering overlap crashes
            onForgotMpinRequested.invoke() // Emits callback trigger outwards to the lifecycle host
        }
    }

    private fun updateMpinDots(length: Int) {
        val dotsLayout = binding.layoutMpinDots
        for (i in 0 until dotsLayout.childCount) {
            val dot = dotsLayout.getChildAt(i)
            if (i < length) {
                dot.setBackgroundResource(R.drawable.mpin_dot_filled)
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