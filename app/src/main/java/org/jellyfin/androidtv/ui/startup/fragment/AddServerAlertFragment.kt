package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.databinding.FragmentAlertAddServerBinding
import org.jellyfin.androidtv.ui.shared.AlertFragment
import org.jellyfin.androidtv.ui.shared.KeyboardFocusChangeListener
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AddServerAlertFragment : AlertFragment() {
	companion object {
		const val ARG_SERVER_ADDRESS = "server_address"
	}

	private val loginViewModel: LoginViewModel by sharedViewModel()
	private lateinit var binding: FragmentAlertAddServerBinding

	private val serverAddressArgument get() = arguments?.getString(ARG_SERVER_ADDRESS)?.ifBlank { null }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		title = R.string.lbl_enter_server_address
	}

	override fun onCreateChildView(inflater: LayoutInflater, contentContainer: ViewGroup): View? {
		binding = FragmentAlertAddServerBinding.inflate(inflater, contentContainer, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		with(binding.address) {
			onFocusChangeListener = KeyboardFocusChangeListener()
			nextFocusForwardId = parentBinding.confirm.id
			nextFocusDownId = parentBinding.confirm.id
			imeOptions = EditorInfo.IME_ACTION_DONE
			setOnEditorActionListener { _, actionId, _ ->
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					clearFocus()
					parentBinding.confirm.performClick()
					true
				} else {
					false
				}
			}

			if (serverAddressArgument != null) {
				setText(serverAddressArgument)
				isEnabled = false
				parentBinding.confirm.performClick()
			} else {
				requestFocus()
			}
		}
	}

	override fun onConfirm(): Boolean {
		if (binding.address.text.isNotBlank()) {
			loginViewModel.addServer(binding.address.text.toString()).observe(viewLifecycleOwner) { state ->
				when (state) {
					is ConnectingState -> binding.error.text = getString(R.string.server_connecting, state.address)
					is UnableToConnectState -> binding.error.text = getString(
						R.string.server_connection_failed,
						state.addressCandidates.joinToString(prefix = "\n", separator = "\n")
					)
					is ConnectedState -> parentFragmentManager.commit {
						replace<StartupToolbarFragment>(R.id.content_view)
						add<ServerFragment>(
							R.id.content_view,
							null,
							bundleOf(
								ServerFragment.ARG_SERVER_ID to state.id.toString()
							)
						)
					}
				}
			}
		} else {
			binding.error.setText(R.string.server_field_empty)
		}

		return false
	}
}
