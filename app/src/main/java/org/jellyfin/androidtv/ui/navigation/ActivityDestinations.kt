package org.jellyfin.androidtv.ui.navigation

import android.content.Context
import android.content.Intent
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.createBundle
import kotlin.time.Duration

object ActivityDestinations {
	fun externalPlayer(context: Context, position: Duration = Duration.ZERO) = Intent(context, ExternalPlayerActivity::class.java).apply {
		putExtras(
			createBundle {
				putLong(ExternalPlayerActivity.EXTRA_POSITION, position.inWholeMilliseconds)
			}
		)
	}

	fun startup(context: Context, hideSplash: Boolean = true) = Intent(context, StartupActivity::class.java).apply {
		putExtra(StartupActivity.EXTRA_HIDE_SPLASH, hideSplash)
		// Remove history to prevent user going back to current activity
		addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
	}
}
