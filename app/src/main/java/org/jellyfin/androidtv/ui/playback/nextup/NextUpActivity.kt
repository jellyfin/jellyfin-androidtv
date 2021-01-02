package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class NextUpActivity : FragmentActivity() {
	private val viewModel: NextUpViewModel by viewModel()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Observe state
		viewModel.state.observe(this) {state ->
			when (state) {
				// Open next item
				NextUpState.PLAY_NEXT -> {
					startActivity(Intent(this, PlaybackOverlayActivity::class.java))
					finish()
				}
				// Close activity
				NextUpState.CLOSE -> finish()
				// Unknown state
				else -> Unit
			}
		}

		// Add background manager
		BackgroundManager.getInstance(this).attach(window)

		// Add fragment
		supportFragmentManager
			.beginTransaction()
			.add(android.R.id.content, NextUpFragment())
			.commit()

		// Load item info
		val id = intent.getStringExtra("id")
		viewModel.setItemId(id)
	}
}
