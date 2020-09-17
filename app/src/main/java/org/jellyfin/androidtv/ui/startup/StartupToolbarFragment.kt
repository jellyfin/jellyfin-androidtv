package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.shared.ToolbarFragment

class StartupToolbarFragment(
	private val onAddServerClicked: () -> Unit = {}
) : ToolbarFragment() {
	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		requireActivity().findViewById<LinearLayout>(R.id.toolbar_start).apply {
			this.addView(Button(requireContext()).apply {
				text = context.getString(R.string.lbl_add_server)
				setOnClickListener { onAddServerClicked() }
			})
		}
	}
}
