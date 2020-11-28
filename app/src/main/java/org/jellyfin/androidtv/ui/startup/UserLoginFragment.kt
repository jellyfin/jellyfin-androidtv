package org.jellyfin.androidtv.ui.startup

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import kotlinx.android.synthetic.main.fragment_alert_dialog.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.repository.AuthenticatedState
import org.jellyfin.androidtv.data.repository.AuthenticatingState
import org.jellyfin.androidtv.data.repository.RequireSignInState
import org.jellyfin.androidtv.data.repository.ServerUnavailableState
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

		// Override the default confirm button click listener to return the address field text
		view.confirm.setOnClickListener {
			if (username.text.isNotBlank())
				signIn(serverId, username.text.toString(), password.text.toString())
		}

		return view
	}

	private fun signIn(server: UUID, username: String, password: String) {
		loginViewModel.login(server, username, password).observe(viewLifecycleOwner) { state ->
			println(state)
			when (state) {
				AuthenticatingState -> {
					// loading
				}
				RequireSignInState -> {
					// unreachable
				}
				ServerUnavailableState -> {
					// TODO show error
				}
				AuthenticatedState -> {
					onClose()

					// TODO use view model and observe in activity
					(requireActivity() as StartupActivity).openNextActivity()
				}
			}
		}
	}
}
