package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.databinding.FragmentAlertUserLoginBinding
import org.jellyfin.androidtv.ui.shared.AlertFragment
import org.jellyfin.androidtv.ui.shared.KeyboardFocusChangeListener
import org.jellyfin.androidtv.util.toUUIDOrNull
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class UserLoginAlertFragment : AlertFragment() {
	companion object {
		const val ARG_USERNAME = "username"
		const val ARG_SERVER_ID = "server_id"
	}

	private val loginViewModel: LoginViewModel by sharedViewModel()
	private lateinit var binding: FragmentAlertUserLoginBinding

	private val usernameArgument get() = arguments?.getString(ARG_USERNAME)?.ifBlank { null }
	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.ifBlank { null }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		title = R.string.lbl_sign_in
	}

	override fun onCreateChildView(inflater: LayoutInflater, contentContainer: ViewGroup): View? {
		binding = FragmentAlertUserLoginBinding.inflate(inflater, contentContainer, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		with(binding.username) {
			onFocusChangeListener = KeyboardFocusChangeListener()

			if (usernameArgument != null) {
				isFocusable = false
				isEnabled = false
				setText(usernameArgument)
				binding.password.requestFocus()
			}
		}

		with(binding.password) {
			onFocusChangeListener = KeyboardFocusChangeListener()
			nextFocusForwardId = parentBinding.confirm.id
			nextFocusDownId = parentBinding.confirm.id

			imeOptions = EditorInfo.IME_ACTION_DONE
			setOnEditorActionListener { _, actionId, _ ->
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					clearFocus()
					parentBinding.confirm.performClick()
					return@setOnEditorActionListener true
				}
				else false
			}
		}

		// Set focus
		if (usernameArgument == null) binding.username.requestFocus()
		else binding.password.requestFocus()
	}

	override fun onConfirm(): Boolean {
		val server = serverIdArgument?.toUUIDOrNull()?.let { loginViewModel.getServer(it) }
		if (server == null) {
			binding.error.setText(R.string.msg_error_server_unavailable)
		} else if (binding.username.text.isNotBlank()) {
			loginViewModel.login(
				server,
				binding.username.text.toString(),
				binding.password.text.toString()
			).observe(viewLifecycleOwner) { state ->
				when (state) {
					AuthenticatingState -> binding.error.setText(R.string.login_authenticating)
					RequireSignInState -> binding.error.setText(R.string.login_invalid_credentials)
					ServerUnavailableState -> binding.error.setText(R.string.login_server_unavailable)
					AuthenticatedState -> onClose()
				}
			}
		} else {
			binding.error.setText(R.string.login_username_field_empty)
		}

		return false
	}
}
