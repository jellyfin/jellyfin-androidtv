package org.jellyfin.androidtv.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.datasource.HttpDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.util.profile.createDeviceProfile
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.playbackManager
import org.jellyfin.playback.core.queue.Queue
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.jellyfinPlugin
import org.jellyfin.playback.jellyfin.queue.ThemePreviewQueueSupplier
import org.jellyfin.playback.media3.exoplayer.ExoPlayerOptions
import org.jellyfin.playback.media3.exoplayer.exoPlayerPlugin
import org.jellyfin.playback.media3.session.MediaSessionOptions
import org.jellyfin.playback.media3.session.media3SessionPlugin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.ServerVersion
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.DeviceProfile
import timber.log.Timber

/**
 * ThemeSongService
 *
 * - Watches which series detail screen you're on
 * - Fetches that series' theme song from the server
 * - Plays it using a dedicated PlaybackManager that only handles preview audio
 *
 * We keep ONE shared PlaybackManager for all previews, so we don't
 * recreate a MediaSession every time (that was the crash before).
 */
class ThemeSongService(
	private val apiClient: ApiClient,
	private val userPreferences: UserPreferences,
	private val serverVersion: ServerVersion,
	private val httpDataSourceFactory: HttpDataSource.Factory,
) {
	companion object {
		/**
		 * Single shared preview player for the whole process.
		 * This prevents "Session ID must be unique" crashes.
		 */
		private var sharedPreviewManager: PlaybackManager? = null
	}

	// last itemId we started previewing
	private var currentItemId: String? = null

	// light background scope
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	/**
	 * Build the DeviceProfile used for playback negotiation with JellyFin.
	 * Mirrors PlaybackModule.kt logic.
	 */
	private fun makeDeviceProfileBuilder(ctx: Context): () -> DeviceProfile {
		val appCtx = ctx.applicationContext
		val prefs = userPreferences
		val ver = serverVersion

		return {
			createDeviceProfile(
				context = appCtx,
				userPreferences = prefs,
				serverVersion = ver,
			)
		}
	}

	/**
	 * Create (or reuse) the tiny PlaybackManager we use just for theme previews.
	 *
	 * IMPORTANT:
	 * - We only build this once.
	 * - We never release() it between shows.
	 * - That way we never try to register a second MediaSession with the same ID.
	 */
	@Synchronized
	private fun ensurePreviewManager(ctx: Context): PlaybackManager {
		sharedPreviewManager?.let { return it }

		val appCtx = ctx.applicationContext
		val notificationChannelId = "theme-preview-session"

		// Make sure the notification channel exists for the preview session.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				notificationChannelId,
				notificationChannelId,
				NotificationManager.IMPORTANCE_LOW,
			).apply {
				setShowBadge(false)
			}
			NotificationManagerCompat.from(appCtx).createNotificationChannel(channel)
		}

		// PendingIntent used if Android shows a notification for this session.
		val activityIntent = Intent(appCtx, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(
			appCtx,
			0,
			activityIntent,
			PendingIntent.FLAG_IMMUTABLE
		)

		// Build the dedicated preview PlaybackManager.
		val mgr = playbackManager(appCtx) {
			// 1) ExoPlayer backend (audio playback)
			val exoPlayerOptions = ExoPlayerOptions(
				preferFfmpeg = userPreferences[UserPreferences.preferExoPlayerFfmpeg],
				enableDebugLogging = userPreferences[UserPreferences.debuggingEnabled],
				baseDataSourceFactory = httpDataSourceFactory,
			)
			install(exoPlayerPlugin(appCtx, exoPlayerOptions))

			// 2) MediaSession + notification
			val mediaSessionOptions = MediaSessionOptions(
				channelId = notificationChannelId,
				notificationId = 2, // just needs to be stable for this manager
				iconSmall = R.drawable.app_icon_foreground,
				openIntent = pendingIntent,
			)
			install(media3SessionPlugin(appCtx, mediaSessionOptions))

			// 3) JellyFin integration (reports playback to server, resolves streams)
			val deviceProfileBuilder = makeDeviceProfileBuilder(appCtx)
			install(
				jellyfinPlugin(
					api = apiClient,
					deviceProfileBuilder = deviceProfileBuilder,
					lifecycle = ProcessLifecycleOwner.get().lifecycle,
				)
			)

			// We could also wire skip amounts etc if we really cared.
			// For preview audio it's not important.
		}

		sharedPreviewManager = mgr
		return mgr
	}

	/**
	 * Call this when the series detail screen becomes visible.
	 *
	 * - Records which item we're on
	 * - (if enabled) fetches theme media
	 * - starts playback via the preview manager
	 */
	fun onEnterItem(ctx: Context, itemId: String?) {
		currentItemId = itemId

		// Respect the user toggle
		val previewEnabled = userPreferences[UserPreferences.themeSongPreviewEnabled]
		if (!previewEnabled) {
			Timber.d("ThemeSongService: entered item=$itemId but preview is disabled in settings")
			return
		}

		Timber.d("ThemeSongService: entered item=$itemId (attempting theme preview)")

		if (itemId == null) {
			Timber.d("ThemeSongService: no itemId -> can't look up theme song")
			return
		}

		scope.launch {
			val started = fetchThemeSongAndStartPreview(ctx, itemId)

			if (started) {
				Timber.d("ThemeSongService: theme preview STARTED for $itemId")
			} else {
				Timber.d("ThemeSongService: theme preview not started for $itemId")
			}
		}
	}

	/**
	 * Call this when you leave that screen.
	 *
	 * We STOP playback (clear queue), but we DO NOT destroy the shared
	 * PlaybackManager or its MediaSession.
	 */
	fun onLeaveItem() {
		val leavingId = currentItemId
		Timber.d("ThemeSongService: leaving item=$leavingId (stopping theme preview)")

		currentItemId = null

		sharedPreviewManager?.let { mgr ->
			val q = mgr.queue
			scope.launch {
				q.clear()
				q.setIndex(Queue.INDEX_NONE, saveHistory = false)
			}
		}
	}

	/**
	 * Hit JellyFin for /Items/{id}/ThemeMedia?inheritFromParent=true
	 * and return the first theme song item, or null.
	 */
	private suspend fun fetchThemeSongItemFor(itemId: String): BaseItemDto? {
		return withContext(Dispatchers.IO) {
			try {
				val uuid = java.util.UUID.fromString(itemId)

				val rsp = apiClient.libraryApi.getThemeMedia(
					itemId = uuid,
					inheritFromParent = true,
				)

				val payload = rsp.content
				if (payload == null) {
					Timber.d("ThemeSongService: no payload for $itemId")
					return@withContext null
				}

				val firstThemeSongItem = payload
					.themeSongsResult
					?.items
					?.firstOrNull()

				if (firstThemeSongItem == null) {
					Timber.d("ThemeSongService: no themeSongsResult.items for $itemId")
					return@withContext null
				}

				Timber.d(
					"ThemeSongService: got theme song item ${firstThemeSongItem.id} (${firstThemeSongItem.name}) for $itemId"
				)

				firstThemeSongItem
			} catch (t: Throwable) {
				Timber.d(t, "ThemeSongService: failed to look up theme media for $itemId")
				null
			}
		}
	}

	/**
	 * Full flow to actually start the preview audio:
	 * - resolve theme item
	 * - ensure preview manager exists
	 * - build a 1-track queue supplier
	 * - clear queue, add supplier, play index 0
	 */
	private suspend fun fetchThemeSongAndStartPreview(
		ctx: Context,
		showItemId: String,
	): Boolean = withContext(Dispatchers.IO) {
		// 1. Which BaseItemDto is the theme song?
		val themeSongItem = fetchThemeSongItemFor(showItemId)
		if (themeSongItem == null) {
			return@withContext false
		}

		Timber.d(
			"ThemeSongService: will try to preview theme ${themeSongItem.id} (${themeSongItem.name}) for $showItemId"
		)

		// 2. Our dedicated preview PlaybackManager
		val mgr = ensurePreviewManager(ctx)

		// 3. Wrap that one theme item as a queue supplier
		val supplier = ThemePreviewQueueSupplier(
			item = themeSongItem,
			api = apiClient,
		)

		// 4. Clear queue + add ours
		val q = mgr.queue
		q.clear()
		q.addSupplier(supplier)

		// 5. Tell queue "play first (only) entry"
		q.setIndex(index = 0, saveHistory = false)

		true
	}

	/**
	 * If we ever want a manual cleanup hook.
	 * We intentionally do NOT release() sharedPreviewManager here,
	 * because killing it and recreating it was what caused the crash loop.
	 */
	fun shutdown() {
		scope.cancel()
	}
}
