package org.jellyfin.androidtv.ui.playback.nextup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentNextUpBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class NextUpFragment : Fragment() {
	private val viewModel: NextUpViewModel by sharedViewModel()
	private lateinit var binding: FragmentNextUpBinding
	private val backgroundService: BackgroundService by inject()
	private val userPreferences: UserPreferences by inject()
	private var timerStarted = false

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentNextUpBinding.inflate(inflater, container, false)

		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.item.collect { data ->
					// No data, keep current
					if (data == null) return@collect

					backgroundService.setBackground(data.baseItem)

					binding.logo.load(
						url = data.logo.url,
						blurHash = data.logo.blurHash,
						aspectRatio = data.logo.aspectRatio
					)
					binding.image.load(
						url = data.thumbnail.url,
						blurHash = data.thumbnail.blurHash,
						aspectRatio = data.thumbnail.aspectRatio
					)
					binding.title.text = data.title
				}
			}
		}

		binding.fragmentNextUpButtons.apply {
			duration = userPreferences[UserPreferences.nextUpTimeout]
			countdownTimerEnabled = duration != NEXTUP_TIMER_DISABLED
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
