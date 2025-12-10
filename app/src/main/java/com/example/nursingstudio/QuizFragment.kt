package com.example.nursingstudio

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Toast

class QuizFragment : Fragment() {

    companion object {
        private const val PREF_SETTINGS = "settings_prefs"
        private const val KEY_QUIZ_SOUND = "enable_quiz_sound"

        /**
         * Agar future me kahin aur se (adapter / dialog) se sound chalana ho,
         * to ye helper function use kar sakte ho:
         *
         * QuizFragment.playTestSoundIfEnabled(context)
         */
        fun playTestSoundIfEnabled(context: Context) {
            val sp = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            val soundOn = sp.getBoolean(KEY_QUIZ_SOUND, true)
            if (!soundOn) {
                // Agar off hai to sirf chup-chaap return
                return
            }

            // Raw se chhota ping sound play karega
            val mp = MediaPlayer.create(context, R.raw.test_ping)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Yahi tumhara khud ka beautiful fragment_quiz.xml inflate karega
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Yaha tum apne existing buttons / views ka code rakh sakte ho.
        // Example (sirf idea, tumhaare IDs par depend karega):
        //
        // val btnStartTest = view.findViewById<Button>(R.id.btnStartTest)
        // btnStartTest.setOnClickListener {
        //     // Apna purana logic + sound:
        //     playTestSoundIfEnabled(requireContext())
        // }

        // Abhi ke liye kuch force nahi kar rahi,
        // sirf helper function available hai.
    }

    /**
     * Agar tum QuizFragment ke andar se hi sound chalana chaho,
     * bina companion wale helper ke, to ye method use kar sakte ho:
     */
    private fun playLocalTestSound() {
        val sp = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        val soundOn = sp.getBoolean(KEY_QUIZ_SOUND, true)
        if (!soundOn) {
            Toast.makeText(
                requireContext(),
                "Turn ON 'Sound in online tests' from Settings.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.test_ping)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
            if (mediaPlayer === mp) mediaPlayer = null
        }
        mediaPlayer?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
