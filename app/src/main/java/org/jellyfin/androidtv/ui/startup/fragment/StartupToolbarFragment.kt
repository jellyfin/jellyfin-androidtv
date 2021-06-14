package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.databinding.FragmentToolbarStartupBinding

class StartupToolbarFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
		FragmentToolbarStartupBinding.inflate(inflater, container, false).root
}
