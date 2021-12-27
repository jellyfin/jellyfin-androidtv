package org.jellyfin.androidtv.ui.playback.nextup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentNextUpBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NextUpFragment : Fragment(), KoinComponent {
	private val viewModel: NextUpViewModel by sharedViewModel()
	private lateinit var binding: FragmentNextUpBinding
	private val backgroundService: BackgroundService by inject()
	private var timerStarted = false
	private var duration = 0L;

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentNextUpBinding.inflate(inflater, container, false)
		duration = get<UserPreferences>()[UserPreferences.nextUpTimeout].toLong()

		viewModel.item.observe(viewLifecycleOwner) { data ->
			// No data, keep current
			if (data == null) return@observe

			backgroundService.setBackground(data.baseItem)

			binding.logo.setImageBitmap(data.logo)
			binding.image.setImageBitmap(data.thumbnail)
			binding.title.text = data.title
		}

		binding.fragmentNextUpButtons.apply {
			duration = get<UserPreferences>()[UserPreferences.nextUpTimeout].toLong()
			countdownTimerEnabled = when {
				duration == 0L -> false
				else -> true
			}
			setPlayNextListener(viewModel::playNext)
			setCancelListener(viewModel::close)
		}

		return binding.root
	}

	override fun onStart() {
		super.onStart()

		if (!timerStarted) {
			binding.fragmentNextUpButtons.startTimer()
			timerStarted = true
		}
	}

	override fun onPause() {
		super.onPause()

		binding.fragmentNextUpButtons.stopTimer()
	}
}
