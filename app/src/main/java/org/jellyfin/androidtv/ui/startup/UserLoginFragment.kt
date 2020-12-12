package org.jellyfin.androidtv.ui.startup

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_alert_dialog.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.ui.shared.AlertFragment
import org.jellyfin.androidtv.ui.shared.KeyboardFocusChangeListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class UserLoginFragment(
	private val serverId: UUID,
	private val user: User? = null,
	private val onClose: () -> Unit = {}
) : AlertFragment(
	title = R.string.lbl_sign_in,
	onCancelCallback = {},
	onClose = onClose
) {
	private val loginViewModel: LoginViewModel by viewModel()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)!!

		view.content.minimumWidth = 360

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
		view.content.addView(username)

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
		view.content.addView(password)

		// Build the error text field
		val errorText = TextView(requireContext())
		view.content.addView(errorText)

		// Override the default confirm button click listener to return the address field text
		view.confirm.setOnClickListener {
			if (username.text.isNotBlank()) {
				loginViewModel.login(serverId, username.text.toString(), password.text.toString()).observe(viewLifecycleOwner) { state ->
					println(state)
					when (state) {
						AuthenticatingState -> {
							errorText.text = getString(R.string.login_authenticating)
						}
						RequireSignInState -> {
							errorText.text = getString(R.string.login_invalid_credentials)
						}
						ServerUnavailableState -> {
							errorText.text = getString(R.string.login_server_unavailable)
						}
						AuthenticatedState -> {
							onClose()

							// TODO use view model and observe in activity
							(requireActivity() as StartupActivity).openNextActivity()
						}
					}
				}
			} else {
				errorText.text = getString(R.string.login_username_field_empty)
			}
		}

		return view
	}
}
