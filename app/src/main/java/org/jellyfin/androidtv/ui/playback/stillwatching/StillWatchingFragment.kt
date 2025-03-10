package org.jellyfin.androidtv.ui.playback.stillwatching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.databinding.FragmentStillWatchingBinding
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class StillWatchingFragment : Fragment() {
	companion object {
		const val STILL_WATCHING_TIMER_DISABLED = 0
	}

	private var _binding: FragmentStillWatchingBinding? = null
	private val binding get() = _binding!!

	private val viewModel: StillWatchingViewModel by viewModel()
	private val navigationRepository: NavigationRepository by inject()
	private var timerStarted = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		viewModel.state
			.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
			.onEach { state ->
				when (state) {
					// Open next item
					StillWatchingState.STILL_WATCHING -> navigationRepository.navigate(Destinations.videoPlayer(0), true)
					// Close activity
					StillWatchingState.CLOSE -> navigationRepository.goBack()
					// Unknown state
					else -> Unit
				}
			}.launchIn(lifecycleScope)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentStillWatchingBinding.inflate(inflater, container, false)

		binding.fragmentStillWatchingButtons.apply {
			// duration = userPreferences[UserPreferences.nextUpTimeout]
			duration = 30000
			countdownTimerEnabled = duration != STILL_WATCHING_TIMER_DISABLED
			setYesListener(viewModel::stillWatching)
			setNoListener(viewModel::close)
		}

		return binding.root
	}

	override fun onStart() {
		super.onStart()

		binding.fragmentStillWatchingButtons.focusNoButton()

		if (!timerStarted) {
			// We need to workaround an issue where compose claims focus for a single draw
			// causing the NextUpButtonsView to auto-stop the timer
			lifecycleScope.launch {
				delay(1)
				binding.fragmentStillWatchingButtons.startTimer()
			}

			timerStarted = true
		}
	}

	override fun onPause() {
		super.onPause()

		binding.fragmentStillWatchingButtons.stopTimer()
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}
}
