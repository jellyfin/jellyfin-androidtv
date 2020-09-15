package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.ui.playback.PlaybackManager
import org.jellyfin.apiclient.interaction.AndroidDevice
import org.jellyfin.apiclient.logging.AndroidLogger
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val playbackModule = module {
	single {
		PlaybackManager(
			AndroidDevice.fromContext(androidApplication()),
			AndroidLogger("PlaybackManager")
		)
	}
}
