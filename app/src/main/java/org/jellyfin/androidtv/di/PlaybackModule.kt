package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.GarbagePlaybackLauncher
import org.jellyfin.androidtv.ui.playback.PlaybackManager
import org.jellyfin.androidtv.ui.playback.RewritePlaybackLauncher
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

	factory {
		val preferences = get<UserPreferences>()
		val useRewrite = preferences[UserPreferences.playbackRewriteEnabled] && BuildConfig.DEVELOPMENT

		// TODO Inject PlaybackLauncher for playback rewrite here
		if (useRewrite) RewritePlaybackLauncher()
		else GarbagePlaybackLauncher(get())
	}
}
