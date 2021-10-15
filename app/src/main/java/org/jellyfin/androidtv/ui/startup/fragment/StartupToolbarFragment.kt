package org.jellyfin.androidtv.ui.startup.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.FragmentToolbarStartupBinding
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.preference.screen.AuthPreferencesScreen

class StartupToolbarFragment : Fragment() {
	private lateinit var binding: FragmentToolbarStartupBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentToolbarStartupBinding.inflate(inflater, container, false)

		binding.help.setOnClickListener {
			parentFragmentManager.commit {
				addToBackStack(null)
				replace<ConnectHelpAlertFragment>(R.id.content_view)
			}
		}

		binding.settings.setOnClickListener {
			val intent = Intent(requireActivity(), PreferencesActivity::class.java)
			intent.putExtra(PreferencesActivity.EXTRA_SCREEN, AuthPreferencesScreen::class.qualifiedName)
			intent.putExtra(PreferencesActivity.EXTRA_SCREEN_ARGS, bundleOf(
				AuthPreferencesScreen.ARG_SHOW_ABOUT to true
			))
			startActivity(intent)
		}

		return binding.root
	}
}
