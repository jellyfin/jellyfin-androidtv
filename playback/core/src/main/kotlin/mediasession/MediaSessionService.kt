package org.jellyfin.playback.core.mediasession

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import androidx.media2.common.VideoSize
import androidx.media2.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.item.QueueEntry

class MediaSessionService(
	private val androidContext: Context,
	private val options: MediaSessionOptions,
) : PlayerService() {
	private val notificationManager = NotificationManagerCompat.from(androidContext)
	private var notifiedNotificationId: Int? = null

	override suspend fun onInitialize() {
		val glue = MediaSessionPlayerGlue(CoroutineScope(coroutineScope.coroutineContext), state)
		val callback = SessionCallback()
		val session = MediaSession.Builder(androidContext, glue).apply {
			setSessionCallback(Dispatchers.IO.asExecutor(), callback)
			setId(options.notificationId.toString())
			setSessionActivity(options.openIntent)
		}.build()

		coroutineScope.launch {
			state.playState.collect {
				glue.notifyCallbacks { onPlayerStateChanged(glue, glue.playerState) }
			}
		}

		coroutineScope.launch {
			state.videoSize.collect { videoSize ->
				glue.notifyCallbacks { onVideoSizeChanged(glue, VideoSize(videoSize.width, videoSize.height)) }
			}
		}

		coroutineScope.launch {
			state.queue.entry.collect { item ->
				val mediaItem = withContext(Dispatchers.IO) { item?.metadata?.toMediaItemWithBitmaps() }
				glue.currentMediaItem = mediaItem
				glue.notifyCallbacks { onCurrentMediaItemChanged(glue, mediaItem) }

				if (item != null) session.updateNotification(item)
				else if (notifiedNotificationId != null) {
					notificationManager.cancel(notifiedNotificationId!!)
					notifiedNotificationId = null
				}
			}
		}
	}

	private fun MediaSession.updateNotification(item: QueueEntry) {
		val stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(androidContext, PlaybackStateCompat.ACTION_STOP)
		val notification = NotificationCompat.Builder(androidContext, options.channelId).apply {
			// Set metadata
			setContentTitle(item.metadata.title)
			setContentText(item.metadata.artist)
			setSubText(item.metadata.displayDescription)

			// Set actions
			setContentIntent(options.openIntent)
			setDeleteIntent(stopIntent)

			// Make visible on lock screen
			setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

			// Add branding
			setSmallIcon(options.iconSmall)

			// Use MediaStyle
			setStyle(MediaStyle().also { style ->
				style.setMediaSession(sessionCompatToken)

				style.setShowCancelButton(true)
				style.setCancelButtonIntent(stopIntent)
			})
		}.build()

		if (notifiedNotificationId == null) notifiedNotificationId = options.notificationId

		// POST_NOTIFICATIONS permission is not required for media notifications
		@Suppress("MissingPermission")
		notificationManager.notify(notifiedNotificationId!!, notification)
	}

	inner class SessionCallback : MediaSession.SessionCallback()
}
