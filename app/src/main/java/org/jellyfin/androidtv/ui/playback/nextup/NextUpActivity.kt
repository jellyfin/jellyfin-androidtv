package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class NextUpActivity : FragmentActivity() {
	companion object {
		const val EXTRA_ID = "id"
		const val EXTRA_USE_EXTERNAL_PLAYER = "useExternalPlayer"
	}

	private val viewModel: NextUpViewModel by viewModel()
	private val backgroundService: BackgroundService by inject()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val useExternalPlayer = intent.getBooleanExtra(EXTRA_USE_EXTERNAL_PLAYER, false)

		// Observe state
		lifecycleScope.launchWhenCreated {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.state.collect { state ->
					when (state) {
						// Open next item
						NextUpState.PLAY_NEXT -> {
							when (useExternalPlayer) {
								true -> startActivity(Intent(this@NextUpActivity, ExternalPlayerActivity::class.java))
								false -> startActivity(Intent(this@NextUpActivity, PlaybackOverlayActivity::class.java))
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

		// Add background manager
		backgroundService.attach(this)

		// Add fragment
		supportFragmentManager
			.beginTransaction()
			.add(android.R.id.content, NextUpFragment())
			.commit()

		// Load item info
		val id = intent.getStringExtra(EXTRA_ID)?.toUUIDOrNull()
		viewModel.setItemId(id)
	}
}
