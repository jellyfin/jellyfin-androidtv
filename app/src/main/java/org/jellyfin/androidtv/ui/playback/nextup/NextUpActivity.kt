package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class NextUpActivity : FragmentActivity() {
	companion object {
		const val EXTRA_ID = "id"
		const val EXTRA_USE_EXTERNAL_PLAYER = "useExternalPlayer"
	}

	private val viewModel: NextUpViewModel by viewModel()
	private val backgroundService: BackgroundService by inject()
	private val mediaManager: MediaManager by inject()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val useExternalPlayer = intent.getBooleanExtra(EXTRA_USE_EXTERNAL_PLAYER, false)

		// Observe state
		viewModel.state.observe(this) { state ->
			when (state) {
				// Open next item
				NextUpState.PLAY_NEXT -> {
					when (useExternalPlayer) {
						true -> startActivity(Intent(this, ExternalPlayerActivity::class.java))
						false -> startActivity(Intent(this, PlaybackOverlayActivity::class.java))
					}
					finish()
				}
				// Skip next item
				NextUpState.SKIP -> {
					when (useExternalPlayer) {
						true -> mediaManager.currentVideoQueue.removeAt(0)
						false -> {
							val playbackController = TvApp.getApplication().playbackController
							when (playbackController != null) {
								true -> {
									playbackController.clearFragment()
									playbackController.removePreviousQueueItems()
								}
								false -> Timber.e("Unable to skip item; playback controller is null")
							}
						}
					}
					val intent = Intent(this, NextUpActivity::class.java)
					intent.putExtra(EXTRA_ID, mediaManager.currentVideoQueue[0].id)
					intent.putExtra(EXTRA_USE_EXTERNAL_PLAYER, useExternalPlayer)
					startActivity(intent)
					finish()
				}
				// Close activity
				NextUpState.CLOSE -> {
					mediaManager.isVideoQueueShuffled = false
					finish()
				}
				// Unknown state
				else -> Unit
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
		val id = intent.getStringExtra(EXTRA_ID)
		viewModel.setItemId(id)
	}
}
