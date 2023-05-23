package org.jellyfin.androidtv.ui.preference.custom

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.preference.LeanbackPreferenceDialogFragmentCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.PreferenceButtonRemapBinding

class ButtonRemapDialogFragment : LeanbackPreferenceDialogFragmentCompat() {
	private var _binding: PreferenceButtonRemapBinding? = null
	private val binding get() = _binding!!

	private var dialogTitle: CharSequence? = null
	private var dialogMessage: CharSequence? = null
	private var keyCode: Int = 0
	private var originalKeyCode: Int = 0
	private var ignoreKeys = listOf(
		KeyEvent.KEYCODE_BACK,
		KeyEvent.KEYCODE_HOME,
		KeyEvent.KEYCODE_APP_SWITCH,
		KeyEvent.KEYCODE_DPAD_CENTER,
		KeyEvent.KEYCODE_DPAD_UP,
		KeyEvent.KEYCODE_DPAD_DOWN,
		KeyEvent.KEYCODE_DPAD_LEFT,
		KeyEvent.KEYCODE_DPAD_RIGHT,
		KeyEvent.KEYCODE_ENTER
	)
	private var checkKeys: View.OnKeyListener = View.OnKeyListener { _, keyCode, _ ->
		// ignore navigation buttons
		if (ignoreKeys.contains(keyCode))
			false
		else {
			this.keyCode = keyCode
			setKeyCodeText()
			binding.buttonSave.isEnabled = this.keyCode != originalKeyCode
			true
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			dialogTitle = preference.dialogTitle
			dialogMessage = preference.dialogMessage
			if (preference is ButtonRemapPreference) {
				dialogTitle = preference.dialogTitle
				dialogMessage = preference.dialogMessage
				keyCode = (preference as ButtonRemapPreference).keyCode
			} else {
				throw IllegalArgumentException("Preference must be a ButtonRemapPreference")
			}
		} else {
			dialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE)
			dialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE)
			keyCode = savedInstanceState.getInt(SAVE_STATE_KEYCODE)
		}
		originalKeyCode = keyCode
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putCharSequence(SAVE_STATE_TITLE, dialogTitle)
		outState.putCharSequence(SAVE_STATE_MESSAGE, dialogMessage)
		outState.putInt(SAVE_STATE_KEYCODE, keyCode)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val styledContext = ContextThemeWrapper(activity, androidx.leanback.preference.R.style.PreferenceThemeOverlayLeanback)
		val styledInflater = inflater.cloneInContext(styledContext)
		_binding = PreferenceButtonRemapBinding.inflate(styledInflater, container, false)

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val buttonRemapPreference = preference as ButtonRemapPreference

		binding.decorTitle.text = preference.title
		binding.message.apply {
			visibility = View.VISIBLE
			text = getString(R.string.pref_button_remapping_description)
		}

		binding.buttonSave.apply {
			setOnClickListener { _ ->
				buttonRemapPreference.keyCode = keyCode
				parentFragmentManager.popBackStack()
			}

			isEnabled = false
			setOnKeyListener(checkKeys)
		}

		binding.buttonReset.apply {
			setOnClickListener { _ ->
				setKeyCodeText()
				buttonRemapPreference.keyCode = buttonRemapPreference.defaultKeyCode
				parentFragmentManager.popBackStack()
			}

			setOnKeyListener(checkKeys)
			requestFocus()
		}

		setKeyCodeText()
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	private fun setKeyCodeText() {
		val provider = ButtonRemapPreference.ButtonRemapSummaryProvider.instance
		binding.textViewKeyCode.text = provider.getKeycodeName(requireContext(), keyCode)
	}

	companion object {
		private const val SAVE_STATE_TITLE = "ButtonRemapDialog.title"
		private const val SAVE_STATE_MESSAGE = "ButtonRemapDialog.message"
		private const val SAVE_STATE_KEYCODE = "ButtonRemapDialog.keycode"

		/**
		 * Creates a new ButtonRemapDialogFragment instance.
		 *
		 * @param key the preference key
		 * @return the new ButtonRemapDialogFragment instance
		 */
		fun newInstance(key: String): ButtonRemapDialogFragment {
			return ButtonRemapDialogFragment().apply {
				arguments = Bundle().apply {
					putString(ARG_KEY, key)
				}
			}
		}
	}
}
