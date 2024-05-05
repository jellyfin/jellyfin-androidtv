package org.jellyfin.playback.core.mediasession

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.metadata

class MediaSessionService(
	private val androidContext: Context,
	private val options: MediaSessionOptions,
) : PlayerService() {
	private val notificationManager = NotificationManagerCompat.from(androidContext)
	private var notifiedNotificationId: Int? = null

	override suspend fun onInitialize() {
		val player = MediaSessionPlayer(
			looper = Looper.getMainLooper(),
			scope = coroutineScope,
			state = state,
			manager = manager,
		)
		val session = MediaSession.Builder(androidContext, player).apply {
			setId(options.notificationId.toString())
			setSessionActivity(options.openIntent)
		}.build()

		state.queue.entry.onEach { item ->
			if (item != null) updateNotification(session, item)
			else if (notifiedNotificationId != null) {
				notificationManager.cancel(notifiedNotificationId!!)
				notifiedNotificationId = null
			}
		}.launchIn(coroutineScope)
	}

	@OptIn(UnstableApi::class)
	private fun updateNotification(session: MediaSession, item: QueueEntry) {
		val notification = NotificationCompat.Builder(androidContext, options.channelId).apply {
			// Set metadata
			setContentTitle(item.metadata.title)
			setContentText(item.metadata.artist)

			// Set actions
			setContentIntent(options.openIntent)
			setStyle(MediaStyleNotificationHelper.MediaStyle(session))

			// Make visible on lock screen
			setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

			// Set flags
			setOnlyAlertOnce(true)
			setOngoing(false)

			// Add branding & art
			setSmallIcon(options.iconSmall)
			item.metadata.artworkUri?.toUri()?.let { artworkUri ->
				setLargeIcon(session.bitmapLoader.loadBitmap(artworkUri).get())
			}
		}.build()

		if (notifiedNotificationId == null) notifiedNotificationId = options.notificationId

		// POST_NOTIFICATIONS permission is not required for media notifications
		@Suppress("MissingPermission")
		notificationManager.notify(notifiedNotificationId!!, notification)
	}
}
