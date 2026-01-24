package org.jellyfin.androidtv.ui.startup

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.FragmentPinDialogBinding
import org.jellyfin.androidtv.preference.repository.UserPinRepository
import org.koin.android.ext.android.inject
import java.util.UUID

class PinDialogFragment : DialogFragment() {

    companion object {
        const val ARG_USER_ID = "user_id"
        const val ARG_REQUEST_KEY = "request_key"
        const val ARG_MODE = "mode"
        const val RESULT_KEY_PIN_ENTERED = "result_pin_entered"
        const val RESULT_EXTRA_SUCCESS = "success"

        enum class Mode {
            VERIFY,
            SET
        }

        fun newInstance(userId: UUID, requestKey: String = RESULT_KEY_PIN_ENTERED, mode: Mode = Mode.VERIFY): PinDialogFragment {
            return PinDialogFragment().apply {
                arguments = bundleOf(
                    ARG_USER_ID to userId.toString(),
                    ARG_REQUEST_KEY to requestKey,
                    ARG_MODE to mode.name
                )
            }
        }
    }

    private var _binding: FragmentPinDialogBinding? = null
    private val binding get() = _binding!!
    private val userPinRepository: UserPinRepository by inject()

    private val userId: UUID by lazy { UUID.fromString(requireArguments().getString(ARG_USER_ID)) }
    private val requestKey: String by lazy { requireArguments().getString(ARG_REQUEST_KEY)!! }
    private val mode: Mode by lazy { Mode.valueOf(requireArguments().getString(ARG_MODE)!!) }

    private var currentPin = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPinDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title.text = if (mode == Mode.SET) getString(R.string.lbl_set_pin) else getString(R.string.lbl_enter_pin)
        updatePinDisplay()

        setupKeypad()
    }

    private fun setupKeypad() {
        val buttons = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            null, "0", null // null for non-digit spots in grid order if 0 is center bottom
        )
        // My layout has buttons manually added.
        // Let's attach listeners based on ID or traversing the grid.
        
        // Manual mapping based on layout I created
        val grid = binding.keypadGrid
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (child is Button) {
                val text = child.text.toString()
                if (text.all { it.isDigit() }) {
                    child.setOnClickListener { onPinDigit(text.toInt()) }
                } else if (text == "⌫") {
                    child.setOnClickListener { onDelete() }
                } else if (text == "OK") {
                    child.setOnClickListener { onEnter() }
                    // Request focus for OK button as default
                    child.post { child.requestFocus() }
                }
            }
        }
    }

    private fun updatePinDisplay() {
        binding.pinInput.setText(currentPin)
    }

    private fun onPinDigit(digit: Int) {
        currentPin += digit.toString()
        updatePinDisplay()
    }

    private fun onDelete() {
        if (currentPin.isNotEmpty()) {
            currentPin = currentPin.dropLast(1)
            updatePinDisplay()
        }
    }

    private var tempPin: String? = null

    private fun onEnter() {
        if (mode == Mode.SET) {
            if (currentPin.isNotEmpty()) {
                if (tempPin == null) {
                    // First entry
                    tempPin = currentPin
                    currentPin = ""
                    updatePinDisplay()
                    binding.title.text = getString(R.string.lbl_reenter_pin)
                } else {
                    // Confirmation entry
                    if (currentPin == tempPin) {
                        userPinRepository.setPin(userId, currentPin)
                        Toast.makeText(context, R.string.lbl_pin_set, Toast.LENGTH_SHORT).show()
                        setFragmentResult(requestKey, bundleOf(RESULT_EXTRA_SUCCESS to true))
                        dismiss()
                    } else {
                        Toast.makeText(context, R.string.lbl_pin_mismatch, Toast.LENGTH_SHORT).show()
                        // Reset to start
                        tempPin = null
                        currentPin = ""
                        updatePinDisplay()
                        binding.title.text = getString(R.string.lbl_set_pin)
                    }
                }
            }
        } else {
            if (userPinRepository.verifyPin(userId, currentPin)) {
                setFragmentResult(requestKey, bundleOf(RESULT_EXTRA_SUCCESS to true))
                dismiss()
            } else {
                Toast.makeText(context, R.string.lbl_incorrect_pin, Toast.LENGTH_SHORT).show()
                currentPin = ""
                updatePinDisplay()
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // Handle physical keyboard / remote number keys
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                 when (keyCode) {
                     in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                         onPinDigit(keyCode - KeyEvent.KEYCODE_0)
                         true
                     }
                     KeyEvent.KEYCODE_DEL -> {
                         onDelete()
                         true
                     }
                     KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                         // Let the focused view handle it (e.g. OK button)
                         // But if focus is elsewhere (e.g. Input field which is not focusable?), we might want to handle it?
                         // The grid buttons are focusable.
                         false
                     }
                     else -> false
                 }
            } else false
        }
        return dialog
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
