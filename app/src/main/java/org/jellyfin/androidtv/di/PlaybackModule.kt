package org.jellyfin.androidtv.di

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.rewrite.RewriteMediaManager
import org.jellyfin.androidtv.util.profile.createDeviceProfile
import org.jellyfin.playback.core.playbackManager
import org.jellyfin.playback.jellyfin.jellyfinPlugin
import org.jellyfin.playback.media3.exoplayer.ExoPlayerOptions
import org.jellyfin.playback.media3.exoplayer.exoPlayerPlugin
import org.jellyfin.playback.media3.session.MediaSessionOptions
import org.jellyfin.playback.media3.session.media3SessionPlugin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import org.jellyfin.androidtv.ui.playback.PlaybackManager as LegacyPlaybackManager

val playbackModule = module {
	single { LegacyPlaybackManager(get()) }
	single { VideoQueueManager() }
	single { RewriteMediaManager(get(), get(), get(), get()) }
	single<MediaManager> { get() as RewriteMediaManager }

	single { PlaybackLauncher(get(), get(), get(), get()) }

	// OkHttp data source using OkHttpFactory from SDK
	single<HttpDataSource.Factory> {
		val httpClientOptions = get<HttpClientOptions>()

		OkHttpDataSource.Factory(OkHttpClient.Builder().apply {
			followRedirects(httpClientOptions.followRedirects)
			connectTimeout(httpClientOptions.connectTimeout.toJavaDuration())
			callTimeout(httpClientOptions.requestTimeout.toJavaDuration())
			readTimeout(httpClientOptions.socketTimeout.toJavaDuration())
			writeTimeout(httpClientOptions.socketTimeout.toJavaDuration())
		}.build())
	}

	single { createPlaybackManager() }
}

fun Scope.createPlaybackManager() = playbackManager(androidContext()) {
	val activityIntent = Intent(get(), MainActivity::class.java)
	val pendingIntent = PendingIntent.getActivity(get(), 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)

	val notificationChannelId = "session"
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		val channel = NotificationChannel(
			notificationChannelId,
			notificationChannelId,
			NotificationManager.IMPORTANCE_LOW
		)
		NotificationManagerCompat.from(get()).createNotificationChannel(channel)
	}

	val userPreferences = get<UserPreferences>()
	val api = get<ApiClient>()
	val exoPlayerOptions = ExoPlayerOptions(
		preferFfmpeg = userPreferences[UserPreferences.preferExoPlayerFfmpeg],
		enableDebugLogging = userPreferences[UserPreferences.debuggingEnabled],
		baseDataSourceFactory = get<HttpDataSource.Factory>(),
	)
	install(exoPlayerPlugin(get(), exoPlayerOptions))

	val mediaSessionOptions = MediaSessionOptions(
		channelId = notificationChannelId,
		notificationId = 1,
		iconSmall = R.drawable.app_icon_foreground,
		openIntent = pendingIntent,
	)
	install(media3SessionPlugin(get(), mediaSessionOptions))

	val deviceProfileBuilder = { createDeviceProfile(userPreferences, false) }
	install(jellyfinPlugin(get(), deviceProfileBuilder))

	// Options
	val userSettingPreferences = get<UserSettingPreferences>()
	defaultRewindAmount = { userSettingPreferences[UserSettingPreferences.skipBackLength].milliseconds }
	defaultFastForwardAmount = { userSettingPreferences[UserSettingPreferences.skipForwardLength].milliseconds }
}
