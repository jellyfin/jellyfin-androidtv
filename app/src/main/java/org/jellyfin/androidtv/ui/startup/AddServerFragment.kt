package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.ui.shared.AlertFragment
import org.jellyfin.androidtv.ui.shared.KeyboardFocusChangeListener
import org.jellyfin.androidtv.util.toUUID
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

class AddServerFragment(
	private val onServerAdded: (serverId: UUID) -> Unit = {},
	private val onCancelCallback: () -> Unit = {},
	private val onClose: () -> Unit = {}
) : AlertFragment(
	title = R.string.lbl_enter_server_address,
	description = R.string.lbl_valid_server_address,
	onCancelCallback = onCancelCallback,
	onClose = onClose
) {
	private val loginViewModel: LoginViewModel by sharedViewModel()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)!!

		val confirm = view.findViewById<Button>(R.id.confirm)

		// Build the url field
		val address = EditText(requireContext())
		address.hint = requireActivity().getString(R.string.lbl_ip_hint)
		address.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
		address.isSingleLine = true
		address.onFocusChangeListener = KeyboardFocusChangeListener()
		address.nextFocusDownId = R.id.confirm
		address.imeOptions = EditorInfo.IME_ACTION_DONE
		address.requestFocus()
		address.setOnEditorActionListener { textView, actionId, keyEvent ->
			if (actionId == EditorInfo.IME_ACTION_DONE)
				confirm.performClick()
			else
				false
		}

		// Add the url field to the content view
		view.findViewById<LinearLayout>(R.id.content).addView(address)

		// Build the error text field
		val errorText = TextView(requireContext())
		view.findViewById<LinearLayout>(R.id.content).addView(errorText)

		// Override the default confirm button click listener to return the address field text
		confirm.setOnClickListener {
			if (address.text.isNotBlank()) {
				loginViewModel.addServer(address.text.toString()).observe(viewLifecycleOwner) { state ->
					when (state) {
						ConnectingState -> errorText.text = getString(R.string.server_connecting)
						is UnableToConnectState -> errorText.text = getString(R.string.server_connection_failed, state.error.message)
						is ConnectedState -> {
							onServerAdded(state.publicInfo.id.toUUID())
							onClose()
						}
					}
				}
			} else {
				errorText.text = getString(R.string.server_field_empty)
			}
		}

		return view
	}
}
