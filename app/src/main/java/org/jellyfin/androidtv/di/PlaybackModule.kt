package org.jellyfin.androidtv.di

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.playback.GarbagePlaybackLauncher
import org.jellyfin.androidtv.ui.playback.LegacyMediaManager
import org.jellyfin.androidtv.ui.playback.RewritePlaybackLauncher
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.rewrite.RewriteMediaManager
import org.jellyfin.playback.core.mediasession.MediaSessionOptions
import org.jellyfin.playback.core.mediasession.mediaSessionPlugin
import org.jellyfin.playback.core.playbackManager
import org.jellyfin.playback.exoplayer.exoPlayerPlugin
import org.jellyfin.playback.jellyfin.jellyfinPlugin
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import org.jellyfin.androidtv.ui.playback.PlaybackManager as LegacyPlaybackManager

val playbackModule = module {
	single { LegacyPlaybackManager(get()) }
	single { VideoQueueManager() }
	single { LegacyMediaManager(get()) }
	single { RewriteMediaManager(get(), get(), get(), get()) }

	factory {
		val preferences = get<UserPreferences>()
		val useRewrite = preferences[UserPreferences.playbackRewriteAudioEnabled]

		if (useRewrite) get<RewriteMediaManager>()
		else get<LegacyMediaManager>()
	}

	factory {
		val preferences = get<UserPreferences>()
		val useRewrite = preferences[UserPreferences.playbackRewriteVideoEnabled] && BuildConfig.DEVELOPMENT

		if (useRewrite) RewritePlaybackLauncher()
		else GarbagePlaybackLauncher(get())
	}

	single { createPlaybackManager() }
}

fun Scope.createPlaybackManager() = playbackManager(androidContext()) {
	install(exoPlayerPlugin(get()))
	install(jellyfinPlugin(get(), get()))

	val activityIntent = Intent(get(), MainActivity::class.java)
	val pendingIntent = PendingIntent.getActivity(get(), 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)

	val notificationChannelId = "mediasession"
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		val channel = NotificationChannel(
			notificationChannelId,
			notificationChannelId,
			NotificationManager.IMPORTANCE_LOW
		)
		NotificationManagerCompat.from(get()).createNotificationChannel(channel)
	}

	install(mediaSessionPlugin(get(), MediaSessionOptions(
		channelId = notificationChannelId,
		notificationId = 1,
		iconSmall = R.drawable.app_icon_foreground,
		openIntent = pendingIntent,
	)))

	// Options
	val userSettingPreferences = get<UserSettingPreferences>()
	defaultRewindAmount = { userSettingPreferences[UserSettingPreferences.skipBackLength].milliseconds }
	defaultFastForwardAmount = { userSettingPreferences[UserSettingPreferences.skipForwardLength].milliseconds }
}
