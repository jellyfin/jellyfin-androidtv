package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import org.jellyfin.androidtv.R

class HomeFragment : Fragment(R.layout.fragment_content_view) {
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		childFragmentManager.commit {
			replace<HomeToolbarFragment>(R.id.content_view)
			add<HomeRowsFragment>(R.id.content_view)
		}
	}
}
