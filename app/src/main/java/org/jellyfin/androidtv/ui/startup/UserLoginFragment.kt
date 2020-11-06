package org.jellyfin.androidtv.ui.startup

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.ui.shared.AlertFragment
import org.jellyfin.androidtv.ui.shared.KeyboardFocusChangeListener

class UserLoginFragment(
	private val user: User? = null,
	private val onConfirmCallback: (username: String, password: String) -> Unit = { _: String, _: String -> },
	onCancelCallback: () -> Unit = {},
	private val onClose: () -> Unit = {}
) : AlertFragment(
	title = R.string.lbl_sign_in,
	onCancelCallback = onCancelCallback,
	onClose = onClose
) {
	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		val contentView = requireActivity().findViewById<LinearLayout>(R.id.content)
		contentView.minimumWidth = 360

		// Build the username field
		val username = EditText(activity)
		username.hint = getString(R.string.lbl_enter_user_name)
		username.isSingleLine = true
		if (user != null) {
			username.setText(user.name)
			username.isEnabled = false
			username.inputType = InputType.TYPE_NULL
		} else {
			username.onFocusChangeListener = KeyboardFocusChangeListener()
			username.requestFocus()
		}
		// Add the username field to the content view
		contentView.addView(username)

		// Build the password field
		val password = EditText(activity)
		password.hint = getString(R.string.lbl_enter_user_pw)
		password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
		password.isSingleLine = true
		password.onFocusChangeListener = KeyboardFocusChangeListener()
		password.nextFocusDownId = R.id.confirm
		password.typeface = Typeface.DEFAULT
		if (user != null) password.requestFocus()
		// Add the password field to the content view
		contentView.addView(password)

		// Override the default confirm button click listener to return the address field text
		val confirmButton = requireActivity().findViewById<Button>(R.id.confirm)
		confirmButton.setOnClickListener {
			if (username.text.isNotBlank()) {
				onConfirmCallback(username.text.toString(), password.text.toString())
				onClose()
			}
		}
	}
}
