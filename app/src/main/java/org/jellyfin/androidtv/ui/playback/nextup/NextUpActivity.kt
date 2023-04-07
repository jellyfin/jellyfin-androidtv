package org.jellyfin.androidtv.ui.playback.nextup

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.ui.validateAuthentication
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class NextUpActivity : FragmentActivity(R.layout.activity_main) {
	companion object {
		const val EXTRA_ID = "id"
		const val EXTRA_USE_EXTERNAL_PLAYER = "useExternalPlayer"
	}

	private val viewModel: NextUpViewModel by viewModel()
	private val navigationRepository: NavigationRepository by inject()

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		if (!validateAuthentication()) return

		val useExternalPlayer = intent.getBooleanExtra(EXTRA_USE_EXTERNAL_PLAYER, false)

		// Observe state
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.state.collect { state ->
					when (state) {
						// Open next item
						NextUpState.PLAY_NEXT -> {
							when (useExternalPlayer) {
								true -> navigationRepository.navigate(Destinations.externalPlayer(0))
								false -> navigationRepository.navigate(Destinations.videoPlayer(0))
							}
							finish()
						}
						// Close activity
						NextUpState.CLOSE -> finish()
						// Unknown state
						else -> Unit
					}
				}
			}
		}

		// Add background
		findViewById<ComposeView>(R.id.background).setContent {
			AppBackground()
		}

		// Add fragment
		supportFragmentManager
			.beginTransaction()
			.add(R.id.content_view, NextUpFragment())
			.commit()

		// Load item info
		val id = intent.getStringExtra(EXTRA_ID)?.toUUIDOrNull()
		viewModel.setItemId(id)
	}
}
