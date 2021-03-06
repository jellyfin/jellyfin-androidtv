package org.jellyfin.androidtv.ui.startup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.databinding.FragmentToolbarStartupBinding
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.preference.screen.AuthPreferencesScreen

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
			val settingsIntent = Intent(requireActivity(), PreferencesActivity::class.java)
			settingsIntent.putExtra(PreferencesActivity.EXTRA_SCREEN, AuthPreferencesScreen::class.qualifiedName)
			startActivity(settingsIntent)
		}

		return binding.root
	}
}
