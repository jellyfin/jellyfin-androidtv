package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.R

class StartupToolbarFragment(
	private val onAddServerClicked: () -> Unit = {}
) : Fragment(R.layout.fragment_toolbar_startup) {
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<Button>(R.id.add_server).setOnClickListener {
			onAddServerClicked()
		}
	}
}
