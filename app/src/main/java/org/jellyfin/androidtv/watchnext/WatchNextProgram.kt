package org.jellyfin.androidtv.watchnext

import android.net.Uri

/** Domain model used to publish Watch Next entries via TvProvider. */
data class WatchNextProgram(
    val internalId: String,
    val serverId: String,
    val itemId: String,
    val title: String,
    val subtitle: String? = null,
    val posterUri: Uri? = null,
    val previewVideoUri: Uri? = null,
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
)
