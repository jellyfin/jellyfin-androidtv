package org.jellyfin.androidtv.ui.shared

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.R

@Suppress("LongParameterList")
open class AlertFragment(
	@StringRes private val title: Int,
	@StringRes private val description: Int? = null,
	@StringRes private val confirmButtonText: Int? = null,
	private val onConfirmCallback: () -> Unit = {},
	@StringRes private val cancelButtonText: Int? = null,
	private val onCancelCallback: () -> Unit = {},
	private val onClose: () -> Unit = {}
) : Fragment(R.layout.fragment_alert_dialog) {
	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		val titleView = requireActivity().findViewById<TextView>(R.id.title)
		titleView.setText(title)

		val descriptionView = requireActivity().findViewById<TextView>(R.id.description)
		if (description != null) {
			descriptionView.setText(description)
		} else {
			descriptionView.visibility = View.GONE
		}

		val confirmButton = requireActivity().findViewById<Button>(R.id.confirm)
		if (confirmButtonText != null) confirmButton.setText(confirmButtonText)
		confirmButton.requestFocus()
		confirmButton.setOnClickListener {
			onConfirmCallback()
			onClose()
		}

		val cancelButton = requireActivity().findViewById<Button>(R.id.cancel)
		if (cancelButtonText != null) cancelButton.setText(cancelButtonText)
		cancelButton.setOnClickListener {
			onCancelCallback()
			onClose()
		}
	}
}
