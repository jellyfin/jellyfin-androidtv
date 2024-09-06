package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ApiClientErrorLoginState
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.databinding.FragmentUserLoginCredentialsBinding
import org.jellyfin.androidtv.ui.startup.UserLoginViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UserLoginCredentialsFragment : Fragment() {
	private val userLoginViewModel: UserLoginViewModel by activityViewModel()
	private var _binding: FragmentUserLoginCredentialsBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = FragmentUserLoginCredentialsBinding.inflate(inflater, container, false)

		with(binding.username) {
			// Prefill username
			if (userLoginViewModel.forcedUsername != null) {
				isFocusable = false
				isEnabled = false
				setText(userLoginViewModel.forcedUsername)
			}
		}

		with(binding.password) {
			setOnEditorActionListener { _, actionId, _ ->
				when (actionId) {
					EditorInfo.IME_ACTION_DONE -> {
						loginWithCredentials()
						true
					}

					else -> false
				}
			}
		}

		with(binding.confirm) {
			setOnClickListener { loginWithCredentials() }
		}

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Set focus
		if (binding.username.isFocusable) binding.username.requestFocus()
		else binding.password.requestFocus()

		// React to login state
		lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				userLoginViewModel.loginState.onEach { state ->
					when (state) {
						is ServerVersionNotSupported -> binding.error.setText(
							getString(
								R.string.server_issue_outdated_version,
								state.server.version,
								ServerRepository.recommendedServerVersion.toString()
							)
						)

						AuthenticatingState -> binding.error.setText(R.string.login_authenticating)
						RequireSignInState -> binding.error.setText(R.string.login_invalid_credentials)
						ServerUnavailableState,
						is ApiClientErrorLoginState -> binding.error.setText(R.string.login_server_unavailable)
						// Do nothing because the activity will respond to the new session
						AuthenticatedState -> Unit
						// Not initialized
						null -> Unit
					}
				}.launchIn(this)
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	private fun loginWithCredentials() {
		when {
			binding.username.text.isNotBlank() -> lifecycleScope.launch {
				userLoginViewModel.login(
					binding.username.text.toString(),
					binding.password.text.toString()
				)
			}

			else -> binding.error.setText(R.string.login_username_field_empty)
		}
	}
}
