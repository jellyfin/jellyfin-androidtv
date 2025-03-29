package org.jellyfin.androidtv.ui.playback.stillwatching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.databinding.FragmentStillWatchingBinding
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.VideoManager
import org.jellyfin.androidtv.ui.playback.WatchTrackerViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class StillWatchingFragment(
	private var videoManager: VideoManager
) : DialogFragment() {
	private var _binding: FragmentStillWatchingBinding? = null
	private val binding get() = _binding!!

	private val navigationRepository: NavigationRepository by inject()
	private val watchTracker: WatchTrackerViewModel by inject()
	private val viewModel: StillWatchingViewModel by viewModel()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		viewModel.state
			.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
			.onEach { state ->
				val dialog = requireActivity().supportFragmentManager.findFragmentByTag("STILL_WATCHING") as? DialogFragment

				when (state) {
					// Continue watching
					StillWatchingState.STILL_WATCHING -> {
						watchTracker.notifyInteraction()
						videoManager.play()
						dialog?.dismiss()
					}
					// Close activity
					StillWatchingState.CLOSE -> {
						navigationRepository.goBack()
						dialog?.dismiss()
					}
					// Unknown state
					else -> Unit
				}
			}.launchIn(lifecycleScope)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentStillWatchingBinding.inflate(inflater, container, false)

		binding.fragmentStillWatchingButtons.apply {
			setYesListener(viewModel::stillWatching)
			setNoListener(viewModel::close)
		}

		return binding.root
	}

	override fun onStart() {
		super.onStart()

		binding.fragmentStillWatchingButtons.focusYesButton()
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}
}
