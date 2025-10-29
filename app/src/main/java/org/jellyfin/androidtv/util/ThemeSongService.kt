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
import org.jellyfin.androidtv.preference.ThemeSongSettings
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * ThemeSongService
 *
 * - Tracks which library item is currently active in the details screen.
 * - When you enter, it looks up that item's theme.mp3 from Jellyfin.
 * - It builds/uses a tiny PlaybackManager dedicated to just that one track.
 * - When you leave, it clears playback (but does NOT destroy the manager).
 */
class ThemeSongService(
	private val settings: ThemeSongSettings,
	private val apiClient: ApiClient,
	private val userPreferences: UserPreferences,
	private val serverVersion: ServerVersion,
	private val httpDataSourceFactory: HttpDataSource.Factory,
	private val userSettingPreferences: UserSettingPreferences,
) {
	companion object {
		/**
		 * We keep ONE shared preview PlaybackManager for the entire app process.
		 * We never throw it away, so we never try to create a second MediaSession.
		 */
		private var sharedPreviewManager: PlaybackManager? = null
	}

	// which show/movie/etc we're previewing right now
	private var currentItemId: String? = null

	// lightweight background scope for API + queue work
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	/**
	 * Build the same DeviceProfile Jellyfin uses for playback negotiation.
	 * This matches PlaybackModule.kt's `deviceProfileBuilder`.
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
	 * Build (or reuse) the mini PlaybackManager we use JUST for theme preview.
	 *
	 * IMPORTANT:
	 * - We only ever create this ONCE per process.
	 * - We reuse it between shows.
	 * - We DO NOT release() it when leaving a show.
	 *
	 * That prevents multiple MediaSession instances and fixes the
	 * "Session ID must be unique" crash.
	 */
	@Synchronized
	private fun ensurePreviewManager(ctx: Context): PlaybackManager {
		// 1. Reuse if we've already created it
		sharedPreviewManager?.let { return it }

		val appCtx = ctx.applicationContext

		// We'll give this preview player its own notif channel / session identity.
		val notificationChannelId = "theme-preview-session"

		// Create a channel for the preview player's MediaSession notification
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

		// PendingIntent so tapping the (hidden/low-priority) notification knows where to go.
		val activityIntent = Intent(appCtx, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(
			appCtx,
			/* requestCode = */ 0,
			activityIntent,
			PendingIntent.FLAG_IMMUTABLE
		)

		// Actually build our tiny PlaybackManager.
		// This mirrors createPlaybackManager() in PlaybackModule.kt,
		// but it's dedicated to audio theme previews.
		val mgr = playbackManager(appCtx) {
			// 1) ExoPlayer backend
			val exoPlayerOptions = ExoPlayerOptions(
				preferFfmpeg = userPreferences[UserPreferences.preferExoPlayerFfmpeg],
				enableDebugLogging = userPreferences[UserPreferences.debuggingEnabled],
				baseDataSourceFactory = httpDataSourceFactory,
			)
			install(exoPlayerPlugin(appCtx, exoPlayerOptions))

			// 2) Media3 session + notification plumbing
			//
			// NOTE: notificationId here just needs to be stable for *this* manager.
			// The crash you were seeing wasn't about this int,
			// it was about creating a brand new MediaSession twice.
			val mediaSessionOptions = MediaSessionOptions(
				channelId = notificationChannelId,
				notificationId = 2,
				iconSmall = R.drawable.app_icon_foreground,
				openIntent = pendingIntent,
			)
			install(media3SessionPlugin(appCtx, mediaSessionOptions))

			// 3) Jellyfin plugin (tells the server "we're playing this")
			val deviceProfileBuilder = makeDeviceProfileBuilder(appCtx)
			install(
				jellyfinPlugin(
					api = apiClient,
					deviceProfileBuilder = deviceProfileBuilder,
					lifecycle = ProcessLifecycleOwner.get().lifecycle,
				)
			)

			// 4) Optional skip amounts etc. We mirror PlaybackModule so ExoPlayer
			//    doesn't freak out if something tries to seek.
			defaultRewindAmount = {
				userSettingPreferences[UserSettingPreferences.skipBackLength].milliseconds
			}
			defaultFastForwardAmount = {
				userSettingPreferences[UserSettingPreferences.skipForwardLength].milliseconds
			}
		}

		sharedPreviewManager = mgr
		return mgr
	}

	/**
	 * Fragment calls this when the detail screen becomes visible.
	 * We:
	 *  - record which item we're previewing
	 *  - look up its theme.mp3
	 *  - enqueue it in the preview PlaybackManager
	 *  - start playback
	 */
	fun onEnterItem(ctx: Context, itemId: String?) {
		currentItemId = itemId

		if (!settings.themeSongsEnabled) {
			Timber.d("ThemeSongService: entered item=$itemId but theme songs are disabled")
			return
		}

		Timber.d("ThemeSongService: entered item=$itemId (about to start theme preview logic)")

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
	 * Fragment calls this when you leave the detail screen.
	 * We clear the preview queue and reset selection.
	 *
	 * CRUCIAL: we DO NOT destroy the preview PlaybackManager or null it out.
	 * We just stop playback so that the next show can reuse the same manager
	 * (and the same MediaSession) without crashing.
	 */
	fun onLeaveItem() {
		val leavingId = currentItemId

		if (leavingId != null && settings.themeSongsEnabled) {
			Timber.d("ThemeSongService: leaving item=$leavingId (stopping theme preview)")
		} else {
			Timber.d("ThemeSongService: leaving item=$leavingId (no-op)")
		}

		currentItemId = null

		val mgr = sharedPreviewManager
		if (mgr != null) {
			val q = mgr.queue
			scope.launch {
				// Stop playback by clearing and resetting queue state
				q.clear()
				q.setIndex(Queue.INDEX_NONE, saveHistory = false)
			}
		}
	}

	/**
	 * Ask Jellyfin for this item's theme media (inheritFromParent = true).
	 * Returns the first theme song BaseItemDto we find, or null.
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

				// grab the first theme song item, if any
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
	 * Full flow:
	 *  - look up theme song BaseItemDto
	 *  - build ThemePreviewQueueSupplier (one-track queue)
	 *  - clear queue
	 *  - add supplier
	 *  - set index 0 (starts playback)
	 *
	 * Returns true if we successfully kicked off playback.
	 */
	private suspend fun fetchThemeSongAndStartPreview(
		ctx: Context,
		showItemId: String,
	): Boolean = withContext(Dispatchers.IO) {
		// Step 1: Resolve which BaseItemDto represents the theme song
		val themeSongItem = fetchThemeSongItemFor(showItemId)
		if (themeSongItem == null) {
			return@withContext false
		}

		Timber.d(
			"ThemeSongService: will try to preview theme ${themeSongItem.id} (${themeSongItem.name}) for $showItemId"
		)

		// Step 2: Build/reuse our mini playback manager w/ real backend
		val mgr = ensurePreviewManager(ctx)

		// Step 3: Create the tiny one-track queue supplier
		val supplier = ThemePreviewQueueSupplier(
			item = themeSongItem,
			api = apiClient,
		)

		// Step 4: Clear queue and enqueue ours
		val q = mgr.queue
		q.clear()
		q.addSupplier(supplier)

		// Step 5: tell queue "play the first (and only) entry"
		q.setIndex(index = 0, saveHistory = false)

		true
	}

	/**
	 * In case we ever want to permanently tear down this service (app exit, etc).
	 * Right now we leave sharedPreviewManager alive until process death,
	 * because destroying it is what caused the crash-on-next-show.
	 */
	fun shutdown() {
		scope.cancel()
		// We intentionally do NOT release() sharedPreviewManager here.
	}
}
