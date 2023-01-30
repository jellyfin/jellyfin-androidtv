package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.GarbagePlaybackLauncher
import org.jellyfin.androidtv.ui.playback.LegacyMediaManager
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackManager
import org.jellyfin.androidtv.ui.playback.RewritePlaybackLauncher
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.koin.dsl.module

val playbackModule = module {
	single { PlaybackManager(get()) }
	single { VideoQueueManager() }
	single { LegacyMediaManager(get()) }

	factory<MediaManager> {
		val preferences = get<UserPreferences>()
		val useRewrite = preferences[UserPreferences.playbackRewriteAudioEnabled] && BuildConfig.DEVELOPMENT

		// TODO Return RewriteMediaManager when merged to master
		if (useRewrite) get<LegacyMediaManager>()
		else get<LegacyMediaManager>()
	}

	factory {
		val preferences = get<UserPreferences>()
		val useRewrite = preferences[UserPreferences.playbackRewriteVideoEnabled] && BuildConfig.DEVELOPMENT

		if (useRewrite) RewritePlaybackLauncher()
		else GarbagePlaybackLauncher(get())
	}
}
