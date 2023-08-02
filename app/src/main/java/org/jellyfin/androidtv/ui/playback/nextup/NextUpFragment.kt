package org.jellyfin.androidtv.ui.playback.nextup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentNextUpBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class NextUpFragment : Fragment() {
	companion object {
		const val ARGUMENT_ITEM_ID = "item_id"
	}

	private var _binding: FragmentNextUpBinding? = null
	private val binding get() = _binding!!

	private val viewModel: NextUpViewModel by viewModel()
	private val backgroundService: BackgroundService by inject()
	private val userPreferences: UserPreferences by inject()
	private val navigationRepository: NavigationRepository by inject()
	private var timerStarted = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val id = arguments?.getString(ARGUMENT_ITEM_ID)?.toUUIDOrNull()
		viewModel.setItemId(id)

		viewModel.state
			.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
			.onEach { state ->
				when (state) {
					// Open next item
					NextUpState.PLAY_NEXT -> navigationRepository.navigate(Destinations.videoPlayer(0), true)
					// Close activity
					NextUpState.CLOSE -> navigationRepository.goBack()
					// Unknown state
					else -> Unit
				}
			}.launchIn(lifecycleScope)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentNextUpBinding.inflate(inflater, container, false)

		viewModel.item
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.filterNotNull()
			.onEach { data ->
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
			}.launchIn(viewLifecycleOwner.lifecycleScope)

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

		binding.fragmentNextUpButtons.focusPlayNextButton()

		if (!timerStarted) {
			// We need to workaround an issue where compose claims focus for a single draw
			// causing the NextUpButtonsView to auto-stop the timer
			lifecycleScope.launch {
				delay(1)
				binding.fragmentNextUpButtons.startTimer()
			}

			timerStarted = true
		}
	}

	override fun onPause() {
		super.onPause()

		binding.fragmentNextUpButtons.stopTimer()
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}
}
