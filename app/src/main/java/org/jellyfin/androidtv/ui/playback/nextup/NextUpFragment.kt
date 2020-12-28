package org.jellyfin.androidtv.ui.playback.nextup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentNextUpBinding
import org.jellyfin.androidtv.util.toHtmlSpanned
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.android.ext.android.inject

class NextUpFragment : Fragment() {
	private val viewModel: NextUpViewModel by sharedViewModel()
	private lateinit var binding: FragmentNextUpBinding
	private val backgroundService: BackgroundService by inject()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentNextUpBinding.inflate(inflater, container, false)

		viewModel.item.observe(viewLifecycleOwner) { data ->
			// No data, keep current
			if (data == null) return@observe

			if (data.backdrop != null) backgroundService.setBackground(data.backdrop)
			else backgroundService.clearBackgrounds()

			binding.logo.setImageBitmap(data.logo)
			binding.image.setImageBitmap(data.thumbnail)

			binding.title.text = data.title
			binding.description.text = data.description?.toHtmlSpanned()
		}

		binding.fragmentNextUpButtons.apply {
			setPlayNextListener(viewModel::playNext)
			setCancelListener(viewModel::close)
		}

		return binding.root
	}

	override fun onResume() {
		super.onResume()

		binding.fragmentNextUpButtons.startTimer()
	}

	override fun onPause() {
		super.onPause()

		binding.fragmentNextUpButtons.stopTimer()
	}
}
