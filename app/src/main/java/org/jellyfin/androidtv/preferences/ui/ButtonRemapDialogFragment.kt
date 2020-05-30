package org.jellyfin.androidtv.preferences.ui

import org.jellyfin.androidtv.R
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.leanback.preference.LeanbackPreferenceDialogFragmentCompat
import java.util.*

class ButtonRemapDialogFragment : LeanbackPreferenceDialogFragmentCompat() {
	private var mDialogTitle: CharSequence? = null
	private var mDialogMessage: CharSequence? = null
	private var mKeyCode: Int = 0
	private var mOriginalKeyCode: Int = 0
	private lateinit var mKeyCodeText: TextView
	private lateinit var mSaveButton: Button
	private var mCheckKeys: View.OnKeyListener = View.OnKeyListener { _, keyCode, _ ->
		// ignore navigation buttons
		if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_HOME
				|| keyCode == KeyEvent.KEYCODE_APP_SWITCH
				|| keyCode == KeyEvent.KEYCODE_DPAD_CENTER
				|| keyCode == KeyEvent.KEYCODE_DPAD_UP
				|| keyCode == KeyEvent.KEYCODE_DPAD_DOWN
				|| keyCode == KeyEvent.KEYCODE_DPAD_LEFT
				|| keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
		)
			false
		else {
			mKeyCode = keyCode
			setKeyCodeText()
			mSaveButton.isEnabled = mKeyCode != mOriginalKeyCode
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
		if (!TextUtils.isEmpty(mDialogTitle)) {
			val titleView = view.findViewById<View>(R.id.decor_title) as TextView
			titleView.text = mDialogTitle
		}
		if (!TextUtils.isEmpty(mDialogMessage)) {
			val messageView = view.findViewById<View>(android.R.id.message) as TextView
			messageView.visibility = View.VISIBLE
			messageView.text = mDialogMessage
		}
		mKeyCodeText = view.findViewById(R.id.buttonKeyCodeTextView)
		setKeyCodeText()

		mSaveButton = view.findViewById(R.id.Save)
		mSaveButton.setOnClickListener { _ ->
			mSaveButton.isEnabled = false
			mOriginalKeyCode = mKeyCode
			(preference as ButtonRemapPreference).setKeyCode(mKeyCode)
		}
		mSaveButton.isEnabled = false
		mSaveButton.setOnKeyListener(mCheckKeys)

		val resetButton = view.findViewById<Button>(R.id.Reset)
		resetButton.setOnClickListener { _ ->
			// TODO: refactor this once the new preference workflow is here
			when (preference.key) {
				"audio_language_button_keycode" -> mKeyCode = KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK
				"subtitle_language_button_keycode" -> mKeyCode = KeyEvent.KEYCODE_CAPTIONS
			}

			setKeyCodeText()
			(preference as ButtonRemapPreference).setKeyCode(mKeyCode)
		}
		resetButton.setOnKeyListener(mCheckKeys)
		resetButton.requestFocus()

		return view
	}

	private fun setKeyCodeText() {
		var keyCodeString = KeyEvent.keyCodeToString(mKeyCode)
		if (keyCodeString.startsWith("KEYCODE")) {
			keyCodeString = keyCodeString.split("_").drop(1).joinToString(" ") { e -> e.toLowerCase(Locale.getDefault()).capitalize() }
		}
		else {
			keyCodeString = "Unknown ($keyCodeString)"
		}
		mKeyCodeText.text = keyCodeString
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
