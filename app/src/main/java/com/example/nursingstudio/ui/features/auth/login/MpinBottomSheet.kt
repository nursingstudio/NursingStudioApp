package com.example.nursingstudio.ui.features.auth.login

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
    private val onForgotMpinRequested: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutMpinKeypadBinding? = null
    private val binding get() = _binding!!
    private var enteredMpin = ""

    private val numericButtons = ArrayList<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
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

        AppSettings.setPushEffect(binding.btnForgotMpin)
        AppSettings.setPushEffect(binding.btnUseBiometrics)
    }

    private fun setupGlassEffect() {
        // 🚀 FIXED: Secured the window... window hierarchy parameters mapping to isolate runtime context exceptions during rapid dialog swaps
        val currentDialog = dialog ?: return
        currentDialog.window?.let { window ->
            window.attributes.blurBehindRadius = 30
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
    }

    private fun initializeNumericButtonsList() {
        numericButtons.clear()
        // 🚀 FIXED: Enforced structured layout initialization variables mappings using strict safe call checking indicators
        _binding?.let { b ->
            numericButtons.add(b.btnKey1)
            numericButtons.add(b.btnKey2)
            numericButtons.add(b.btnKey3)
            numericButtons.add(b.btnKey4)
            numericButtons.add(b.btnKey5)
            numericButtons.add(b.btnKey6)
            numericButtons.add(b.btnKey7)
            numericButtons.add(b.btnKey8)
            numericButtons.add(b.btnKey9)
            numericButtons.add(b.btnKey0)
        }
    }

    private fun shuffleAndPopulateKeypad() {
        val digitsList = arrayListOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        digitsList.shuffle()

        // 🚀 FIXED: Strict length bounding parameters verification check introduced to seamlessly eliminate structural indexing array leakage
        val iterationLimit = minOf(numericButtons.size, digitsList.size)
        for (i in 0 until iterationLimit) {
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
                val numericValue = button.tag?.toString() ?: ""
                if (numericValue.isNotEmpty()) {
                    handleKeyClick(numericValue)
                }
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
                    binding.root.postDelayed({
                        if (_binding != null) updateMpinDots(0)
                    }, 300)
                    Toast.makeText(context, "Incorrect MPIN!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🚀 CRITICAL FIX: Inverted execution pipeline order to resolve Asynchronous Lifecycle Race Condition.
        // Callback routines are invoked BEFORE component dismissal to ensure context reference safety.
        binding.btnUseBiometrics.setOnClickListener {
            if (isAdded) {
                // Step 1: Capture the function reference and safely invoke on host context
                onBiometricRequest.invoke()

                // Step 2: Post the structural dismiss event to the main loop safely
                binding.root.post {
                    if (isAdded && !isStateSaved) {
                        dismissAllowingStateLoss()
                    }
                }
            }
        }

        binding.btnForgotMpin.setOnClickListener {
            if (isAdded) {
                // Step 1: Trigger external route execution before context detachment
                onForgotMpinRequested.invoke()

                // Step 2: Smooth cleanup execution sequence without losing asynchronous focus
                binding.root.post {
                    if (isAdded && !isStateSaved) {
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    private fun updateMpinDots(length: Int) {
        // 🚀 FIXED: Complete structural protection null-safety check added to circumvent UI postDelayed rendering anomalies
        val dotsLayout = _binding?.layoutMpinDots ?: return
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
        numericButtons.clear()
        super.onDestroyView()
        _binding = null
    }
}