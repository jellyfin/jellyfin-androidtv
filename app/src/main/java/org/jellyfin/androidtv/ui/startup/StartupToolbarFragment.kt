package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.databinding.FragmentToolbarStartupBinding

class StartupToolbarFragment : Fragment() {
	private lateinit var binding: FragmentToolbarStartupBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentToolbarStartupBinding.inflate(inflater, container, false)

		// Add click listener
		binding.addServer.setOnClickListener {
			val activity = requireActivity()
			if (activity is StartupActivity) activity.addServer()
		}

		binding.manageServers.setOnClickListener {
			val activity = requireActivity()
			if (activity is StartupActivity) activity.manageServers()
		}

		return binding.root
	}
}
