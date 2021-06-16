package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.ConnectedQuickConnectState
import org.jellyfin.androidtv.auth.model.PendingQuickConnectState
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.model.UnavailableQuickConnectState
import org.jellyfin.androidtv.auth.model.UnknownQuickConnectState
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.databinding.FragmentUserLoginQuickConnectBinding
import org.jellyfin.androidtv.ui.startup.UserLoginViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class UserLoginQuickConnectFragment : Fragment() {
	private val userLoginViewModel: UserLoginViewModel by sharedViewModel()
	private lateinit var binding: FragmentUserLoginQuickConnectBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentUserLoginQuickConnectBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		userLoginViewModel.clearLoginState()

		// Initialize & react to Quick Connect specific state
		lifecycleScope.launch {
			userLoginViewModel.initiateQuickconnect()

			userLoginViewModel.quickConnectState.collect { state ->
				when (state) {
					is PendingQuickConnectState -> {
						binding.quickConnectCode.text = state.code.formatCode()
						binding.loading.isVisible = false
					}

					UnavailableQuickConnectState,
					UnknownQuickConnectState,
					ConnectedQuickConnectState -> binding.loading.isVisible = true
				}
			}
		}

		// React to login state
		lifecycleScope.launch {
			userLoginViewModel.loginState.collect { state ->
				when (state) {
					is ServerVersionNotSupported -> binding.error.setText(getString(
						R.string.server_unsupported,
						state.server.version,
						ServerRepository.minimumServerVersion.toString()
					))
					AuthenticatingState -> binding.error.setText(R.string.login_authenticating)
					RequireSignInState -> binding.error.setText(R.string.login_invalid_credentials)
					ServerUnavailableState -> binding.error.setText(R.string.login_server_unavailable)
					// Do nothing because the activity will respond to the new session
					AuthenticatedState -> Unit
					// Not initialized
					null -> Unit
				}
			}
		}
	}

	/**
	 * Add space after every 3 characters so "420420" becomes "420 420".
	 */
	private fun String.formatCode() = buildString {
		@Suppress("MagicNumber")
		val interval = 3
		this@formatCode.forEachIndexed { index, character ->
			if (index != 0 && index % interval == 0) append(" ")
			append(character)
		}
	}
}
