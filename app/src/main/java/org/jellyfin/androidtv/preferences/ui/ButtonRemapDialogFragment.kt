package org.jellyfin.androidtv.preferences.ui

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.TextView
import androidx.leanback.preference.LeanbackPreferenceDialogFragmentCompat

class ButtonRemapDialogFragment : LeanbackPreferenceDialogFragmentCompat() {
	private var mDialogTitle: CharSequence? = null
	private var mDialogMessage: CharSequence? = null
	private var mKeyCode: Int = 0

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
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle)
		outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage)
		outState.putInt(SAVE_STATE_KEYCODE, mKeyCode)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		val theme = org.jellyfin.androidtv.R.style.PreferenceThemeOverlayLeanback
		val styledContext: Context = ContextThemeWrapper(activity, theme)
		val styledInflater = inflater.cloneInContext(styledContext)
		val view: View = styledInflater.inflate(org.jellyfin.androidtv.R.layout.button_remap_preference,
			container, false)
		if (!TextUtils.isEmpty(mDialogTitle)) {
			val titleView = view.findViewById<View>(org.jellyfin.androidtv.R.id.decor_title) as TextView
			titleView.text = mDialogTitle
		}
		if (!TextUtils.isEmpty(mDialogMessage)) {
			val messageView = view.findViewById<View>(android.R.id.message) as TextView
			messageView.visibility = View.VISIBLE
			messageView.text = mDialogMessage
		}
		val mKeyCodeText = view.findViewById<TextView>(org.jellyfin.androidtv.R.id.buttonKeyCodeTextView)
		mKeyCodeText.text = mKeyCode.toString()

		view.isFocusableInTouchMode = true
		view.requestFocus()
		view.setOnKeyListener{ _, keyCode, _ ->
			// ignore navigation buttons
			if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_APP_SWITCH)
				false
			else {
				mKeyCode = keyCode
				mKeyCodeText.text = mKeyCode.toString()
				(preference as ButtonRemapPreference).setKeyCode(mKeyCode)
				true
			}
		}

		return view
	}

	companion object {
		private const val SAVE_STATE_TITLE = "ButtonRemapDialog.title"
		private const val SAVE_STATE_MESSAGE = "ButtonRemapDialog.message"
		private const val SAVE_STATE_KEYCODE = "ButtonRemapDialog.keycode"

		/**
		 * Creates a new ButtonRemapDialogFragment instance
		 *
		 * @param the preference key
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
