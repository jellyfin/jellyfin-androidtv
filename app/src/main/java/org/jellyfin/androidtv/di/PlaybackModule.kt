package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.GarbagePlaybackLauncher
import org.jellyfin.androidtv.ui.playback.LegacyMediaManager
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackManager
import org.jellyfin.androidtv.ui.playback.RewritePlaybackLauncher
import org.koin.dsl.module

val playbackModule = module {
	single { PlaybackManager(get()) }
	single<MediaManager> { LegacyMediaManager(get()) }

	factory {
		val preferences = get<UserPreferences>()
		val useRewrite = preferences[UserPreferences.playbackRewriteEnabled] && BuildConfig.DEVELOPMENT

		if (useRewrite) RewritePlaybackLauncher()
		else GarbagePlaybackLauncher(get())
	}
}
