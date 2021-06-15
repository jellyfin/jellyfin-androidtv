package org.jellyfin.androidtv.ui.startup.fragment

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

		binding.help.setOnClickListener {
			// TODO: What do
		}

		binding.settings.setOnClickListener {
			val intent = Intent(requireActivity(), PreferencesActivity::class.java)
			intent.putExtra(PreferencesActivity.EXTRA_SCREEN, AuthPreferencesScreen::class.qualifiedName)
			startActivity(intent)
		}

		return binding.root
	}
}
