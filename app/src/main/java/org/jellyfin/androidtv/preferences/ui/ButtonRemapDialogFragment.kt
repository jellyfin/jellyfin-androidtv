package org.jellyfin.androidtv.preferences.ui

import org.jellyfin.androidtv.R
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.leanback.preference.LeanbackPreferenceDialogFragmentCompat
import kotlinx.android.synthetic.main.button_remap_preference.*
import java.util.*

class ButtonRemapDialogFragment : LeanbackPreferenceDialogFragmentCompat() {
	private var mDialogTitle: CharSequence? = null
	private var mDialogMessage: CharSequence? = null
	private var mKeyCode: Int = 0
	private var mOriginalKeyCode: Int = 0
	private var mIgnoreKeys = listOf(
			KeyEvent.KEYCODE_BACK,
			KeyEvent.KEYCODE_HOME,
			KeyEvent.KEYCODE_APP_SWITCH,
			KeyEvent.KEYCODE_DPAD_CENTER,
			KeyEvent.KEYCODE_DPAD_UP,
			KeyEvent.KEYCODE_DPAD_DOWN,
			KeyEvent.KEYCODE_DPAD_LEFT,
			KeyEvent.KEYCODE_DPAD_RIGHT)
	private var mCheckKeys: View.OnKeyListener = View.OnKeyListener { _, keyCode, _ ->
		// ignore navigation buttons
		if (mIgnoreKeys.contains(keyCode))
			false
		else {
			mKeyCode = keyCode
			setKeyCodeText()
			requireView().findViewById<Button>(R.id.buttonSave).isEnabled = mKeyCode != mOriginalKeyCode
			true
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			val preference = preference
			mDialogTitle = preference.dialogTitle
			mDialogMessage = preference.dialogMessage
			if (preference is ButtonRemapPreference) {
				mDialogTitle = preference.getDialogTitle()
				mDialogMessage = preference.getDialogMessage()
				mKeyCode = preference.getKeyCode()
			} else {
				throw IllegalArgumentException("Preference must be a ButtonRemapPreference")
			}
		} else {
			mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE)
			mDialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE)
			mKeyCode = savedInstanceState.getInt(SAVE_STATE_KEYCODE)
		}
		mOriginalKeyCode = mKeyCode
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle)
		outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage)
		outState.putInt(SAVE_STATE_KEYCODE, mKeyCode)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		val theme = R.style.PreferenceThemeOverlayLeanback
		val styledContext: Context = ContextThemeWrapper(activity, theme)
		val styledInflater = inflater.cloneInContext(styledContext)
		val view: View = styledInflater.inflate(R.layout.button_remap_preference,
			container, false)
		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (!TextUtils.isEmpty(mDialogTitle)) {
			decor_title.text = mDialogTitle
		}
		if (!TextUtils.isEmpty(mDialogMessage)) {
			message.visibility = View.VISIBLE
			message.text = mDialogMessage
		}

		buttonSave.setOnClickListener { _ ->
			(preference as ButtonRemapPreference).setKeyCode(mKeyCode)
			parentFragmentManager.popBackStack()
		}
		buttonSave.isEnabled = false
		buttonSave.setOnKeyListener(mCheckKeys)

		buttonReset.setOnClickListener { _ ->
			// TODO: refactor this once the new preference workflow is here
			when (preference.key) {
				"audio_language_button_keycode" -> mKeyCode = KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK
				"subtitle_language_button_keycode" -> mKeyCode = KeyEvent.KEYCODE_CAPTIONS
			}

			setKeyCodeText()
			(preference as ButtonRemapPreference).setKeyCode(mKeyCode)
			parentFragmentManager.popBackStack()
		}
		buttonReset.setOnKeyListener(mCheckKeys)
		buttonReset.requestFocus()

		setKeyCodeText()
	}

	private fun setKeyCodeText() {
		textViewKeyCode.text = ButtonRemapSummaryProvider.instance!!.provideSummary(mKeyCode)
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
		fun newInstance(key: String?): ButtonRemapDialogFragment {
			val args = Bundle(1)
			args.putString(ARG_KEY, key)
			val fragment = ButtonRemapDialogFragment()
			fragment.arguments = args
			return fragment
		}
	}
}
