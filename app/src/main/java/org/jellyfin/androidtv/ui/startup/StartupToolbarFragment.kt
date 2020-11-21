package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_toolbar_startup.view.*
import org.jellyfin.androidtv.R

class StartupToolbarFragment(
	private val onAddServerClicked: () -> Unit = {}
) : Fragment(R.layout.fragment_toolbar_startup) {
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.add_server.setOnClickListener {
			onAddServerClicked()
		}
	}
}
