package org.jellyfin.playback.media3.session

import android.app.PendingIntent
import androidx.annotation.DrawableRes

data class MediaSessionOptions(
	val channelId: String,
	val notificationId: Int,
	@DrawableRes val iconSmall: Int,
	val openIntent: PendingIntent,
)
