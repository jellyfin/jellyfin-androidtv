package org.jellyfin.androidtv.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_alert_dialog.view.*
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
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)!!

		val titleView = view.title
		titleView.setText(title)

		val descriptionView = view.description
		if (description != null) {
			descriptionView.setText(description)
		} else {
			descriptionView.visibility = View.GONE
		}

		val confirmButton = view.confirm
		if (confirmButtonText != null) confirmButton.setText(confirmButtonText)
		confirmButton.requestFocus()
		confirmButton.setOnClickListener {
			onConfirmCallback()
			onClose()
		}

		val cancelButton = view.cancel
		if (cancelButtonText != null) cancelButton.setText(cancelButtonText)
		cancelButton.setOnClickListener {
			onCancelCallback()
			onClose()
		}

		return view
	}
}
