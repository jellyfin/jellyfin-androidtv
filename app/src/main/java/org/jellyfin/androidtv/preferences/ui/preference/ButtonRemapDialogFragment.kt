package org.jellyfin.androidtv.preferences.ui.preference

import android.os.Bundle
import android.view.*
import androidx.leanback.preference.LeanbackPreferenceDialogFragmentCompat
import kotlinx.android.synthetic.main.button_remap_preference.*
import kotlinx.android.synthetic.main.button_remap_preference.view.*
import org.jellyfin.androidtv.R

class ButtonRemapDialogFragment : LeanbackPreferenceDialogFragmentCompat() {
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
			requireView().buttonSave.isEnabled = this.keyCode != originalKeyCode
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
		val styledContext = ContextThemeWrapper(activity, R.style.PreferenceThemeOverlayLeanback)
		val styledInflater = inflater.cloneInContext(styledContext)
		return styledInflater.inflate(R.layout.button_remap_preference, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		decor_title.text = preference.title
		message.visibility = View.VISIBLE
		message.text = getString(R.string.pref_button_remapping_description)

		buttonSave.setOnClickListener { _ ->
			(preference as ButtonRemapPreference).setKeyCode(keyCode)
			parentFragmentManager.popBackStack()
		}
		buttonSave.isEnabled = false
		buttonSave.setOnKeyListener(checkKeys)

		buttonReset.setOnClickListener { _ ->
			// TODO: refactor this once the new preference workflow is here
			// FIXME: Doesn't work when using PreferenceDSL
			when (preference.key) {
				"shortcut_audio_track" -> keyCode = KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK
				"shortcut_subtitle_track" -> keyCode = KeyEvent.KEYCODE_CAPTIONS
			}

			setKeyCodeText()
			(preference as ButtonRemapPreference).setKeyCode(keyCode)
			parentFragmentManager.popBackStack()
		}
		buttonReset.setOnKeyListener(checkKeys)
		buttonReset.requestFocus()

		setKeyCodeText()
	}

	private fun setKeyCodeText() {
		textViewKeyCode.text = ButtonRemapPreference.ButtonRemapSummaryProvider.instance.provideSummary(requireContext(), keyCode)
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
