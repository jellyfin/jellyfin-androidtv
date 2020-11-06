package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.shared.AlertFragment
import org.jellyfin.androidtv.ui.shared.KeyboardFocusChangeListener

class AddServerFragment(
	private val onConfirmCallback: (url: String) -> Unit = {},
	onCancelCallback: () -> Unit = {},
	private val onClose: () -> Unit = {}
) : AlertFragment(
	title = R.string.lbl_enter_server_address,
	description = R.string.lbl_valid_server_address,
	onCancelCallback = onCancelCallback,
	onClose = onClose
) {
	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		val contentView = requireActivity().findViewById<LinearLayout>(R.id.content)

		// Build the url field
		val address = EditText(activity)
		address.hint = requireActivity().getString(R.string.lbl_ip_hint)
		address.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
		address.isSingleLine = true
		address.onFocusChangeListener = KeyboardFocusChangeListener()
		address.nextFocusDownId = R.id.confirm
		address.requestFocus()
		// Add the url field to the content view
		contentView.addView(address)

		// Override the default confirm button click listener to return the address field text
		val confirmButton = requireActivity().findViewById<Button>(R.id.confirm)
		confirmButton.setOnClickListener {
			if (address.text.isNotBlank()) {
				onConfirmCallback(address.text.toString())
				onClose()
			}
		}
	}
}
