package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.synthetic.main.fragment_toolbar.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.shared.ToolbarFragment

class StartupToolbarFragment(
	private val onAddServerClicked: () -> Unit = {}
) : ToolbarFragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)!!

		view.toolbar_start.apply {
			this.addView(Button(requireContext()).apply {
				text = context.getString(R.string.lbl_add_server)
				setOnClickListener { onAddServerClicked() }
			})
		}

		return view
	}
}
