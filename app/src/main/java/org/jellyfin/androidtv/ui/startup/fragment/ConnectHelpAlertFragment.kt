package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.FragmentAlertConnectHelpBinding
import org.jellyfin.androidtv.ui.shared.AlertFragment

class ConnectHelpAlertFragment : AlertFragment() {
	private lateinit var binding: FragmentAlertConnectHelpBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		title = R.string.login_help_title
		cancelable = false
	}

	override fun onCreateChildView(inflater: LayoutInflater, contentContainer: ViewGroup): View? {
		binding = FragmentAlertConnectHelpBinding.inflate(inflater, contentContainer, false)
		return binding.root
	}
}
