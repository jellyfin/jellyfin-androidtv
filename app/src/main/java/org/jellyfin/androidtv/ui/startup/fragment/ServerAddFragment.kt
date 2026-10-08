package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.databinding.FragmentServerAddBinding
import org.jellyfin.androidtv.ui.startup.ServerAddViewModel
import org.jellyfin.androidtv.util.createBundle
import org.jellyfin.androidtv.util.getSummary
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServerAddFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ADDRESS = "server_address"
	}

	private val startupViewModel: ServerAddViewModel by viewModel()
	private var _binding: FragmentServerAddBinding? = null
	private val binding get() = _binding!!

	private val serverAddressArgument get() = arguments?.getString(ARG_SERVER_ADDRESS)?.ifBlank { null }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentServerAddBinding.inflate(inflater, container, false)

		with(binding.address) {
			setOnEditorActionListener { _, actionId, _ ->
				when (actionId) {
					EditorInfo.IME_ACTION_DONE -> {
						submitAddress()
						true
					}

					else -> false
				}
			}
		}

		with(binding.headers) {
			// On Android TV, ENTER is otherwise treated as a "select" key and never reaches a
			// focused text field as a line break. Insert the newline ourselves so headers can be
			// entered one per line with a hardware keyboard or remote.
			setOnKeyListener { _, keyCode, event ->
				val isEnter = keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
				if (isEnter && event.action == KeyEvent.ACTION_DOWN) {
					val from = selectionStart.coerceAtLeast(0)
					val to = selectionEnd.coerceAtLeast(0)
					text?.replace(minOf(from, to), maxOf(from, to), "\n")
					true
				} else {
					// Let ENTER's key-up and all other keys fall through untouched
					isEnter
				}
			}
		}

		with(binding.advancedToggle) {
			setOnClickListener { toggleAdvanced() }
		}

		with(binding.confirm) {
			setOnClickListener { submitAddress() }
		}

		return binding.root
	}

	/** Expand or collapse the advanced options accordion holding the custom headers field. */
	private fun toggleAdvanced() {
		val expand = !binding.advancedSection.isVisible
		binding.advancedSection.isVisible = expand
		binding.advancedToggle.setText(
			if (expand) R.string.lbl_advanced_options_hide else R.string.lbl_advanced_options_show
		)
		// Keep D-pad navigation sensible as the section appears/disappears
		binding.advancedToggle.nextFocusDownId = if (expand) binding.headers.id else binding.confirm.id
		if (expand) binding.headers.requestFocus()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (serverAddressArgument != null) {
			binding.address.setText(serverAddressArgument)
			binding.address.isEnabled = false
			submitAddress()
		} else {
			binding.address.requestFocus()
		}

		startupViewModel.state.onEach { state ->
			when (state) {
				is ConnectingState -> {
					// Disable form
					binding.address.isEnabled = false
					binding.confirm.isEnabled = false
					// Update state text
					binding.error.text = getString(R.string.server_connecting, state.address)
				}

				is UnableToConnectState -> {
					// Enable form
					binding.address.isEnabled = true
					binding.confirm.isEnabled = true
					// Update state text
					binding.error.text = getString(
						R.string.server_connection_failed_candidates,
						state.addressCandidates
							.map { "${it.key} - ${it.value.getSummary(requireContext())}" }
							.joinToString(prefix = "\n", separator = "\n")
					)
				}

				is ConnectedState -> parentFragmentManager.commit {
					// Open server view
					replace<StartupToolbarFragment>(R.id.content_view)
					add<ServerFragment>(
						R.id.content_view,
						null,
						createBundle {
							putString(ServerFragment.ARG_SERVER_ID, state.id.toString())
						}
					)
				}

				null -> Unit
			}
		}.launchIn(lifecycleScope)
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	private fun submitAddress() = when {
		binding.address.text.isNotBlank() -> startupViewModel.addServer(
			binding.address.text.toString(),
			parseHeaders(binding.headers.text.toString()),
		)

		else -> binding.error.setText(R.string.server_field_empty)
	}

	/**
	 * Parse the custom headers field, with one `Header-Name: value` entry per line. Blank lines and
	 * lines without a name are ignored.
	 */
	private fun parseHeaders(raw: String): Map<String, String> = raw.lineSequence()
		.mapNotNull { line ->
			val separator = line.indexOf(':')
			if (separator <= 0) return@mapNotNull null

			val name = line.substring(0, separator).trim()
			val value = line.substring(separator + 1).trim()
			if (name.isEmpty()) null else name to value
		}
		.toMap()
}
