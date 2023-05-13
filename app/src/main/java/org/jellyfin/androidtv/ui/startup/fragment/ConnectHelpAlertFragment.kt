package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.databinding.FragmentAlertConnectHelpBinding

class ConnectHelpAlertFragment : Fragment() {
	private var _binding: FragmentAlertConnectHelpBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentAlertConnectHelpBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		with(binding.confirm) {
			requestFocus()
			setOnClickListener { parentFragmentManager.popBackStack() }
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}
}
