package org.jellyfin.androidtv.watchnext

import android.content.Context
import android.net.Uri
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.primaryImage
import org.jellyfin.androidtv.util.apiclient.seriesPrimaryImage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

/**
 * Helper to integrate Watch Next with playback.
 * Updates Watch Next row on Android TV with playback progress.
 */
class WatchNextPlaybackHelper(
    private val context: Context,
    private val api: ApiClient
) {
    companion object {
        private const val TAG = "WatchNextPlayback"
        // Threshold for considering an item "completed" (95%)
        private const val COMPLETION_THRESHOLD = 0.95
    }

    /**
     * Update or create a Watch Next program for the current item.
     * Called during playback to keep the Watch Next row updated.
     */
    fun updateProgress(item: BaseItemDto, positionMs: Long, durationMs: Long) {
        if (!WatchNextManager.isSupported()) return
        if (!shouldPublishToWatchNext(item)) return

        // Check if item is completed
        val completionRatio = if (durationMs > 0) positionMs.toDouble() / durationMs.toDouble() else 0.0
        if (completionRatio >= COMPLETION_THRESHOLD) {
            // Remove from Watch Next when completed
            removeFromWatchNext(item)
            return
        }

        // Only update if there's meaningful progress (more than 1 minute)
        if (positionMs < 60_000L) return

        try {
            val program = createWatchNextProgram(item, positionMs, durationMs)
            WatchNextManager.publish(context, program)
        } catch (e: Exception) {
            Timber.w(e, "Failed to update Watch Next program for item ${item.id}")
        }
    }

    /**
     * Remove an item from Watch Next (e.g., when marked watched or completed).
     */
    fun removeFromWatchNext(item: BaseItemDto) {
        if (!WatchNextManager.isSupported()) return
        
        try {
            val internalId = createInternalId(item)
            WatchNextManager.remove(context, internalId)
        } catch (e: Exception) {
            Timber.w(e, "Failed to remove Watch Next program for item ${item.id}")
        }
    }

    /**
     * Determine if an item should be published to Watch Next.
     * Only movies and episodes are supported.
     */
    private fun shouldPublishToWatchNext(item: BaseItemDto): Boolean {
        return when (item.type) {
            BaseItemKind.MOVIE, BaseItemKind.EPISODE -> true
            else -> false
        }
    }

    /**
     * Create a WatchNextProgram from a BaseItemDto.
     */
    private fun createWatchNextProgram(
        item: BaseItemDto,
        positionMs: Long,
        durationMs: Long
    ): WatchNextProgram {
        val serverId = api.baseUrl ?: "unknown"
        val itemId = item.id.toString()
        val internalId = createInternalId(item)

        // Get title and subtitle based on item type
        val title = when (item.type) {
            BaseItemKind.EPISODE -> item.seriesName ?: item.name ?: "Unknown"
            else -> item.name ?: "Unknown"
        }

        val subtitle = when (item.type) {
            BaseItemKind.EPISODE -> {
                val seasonEpisode = buildString {
                    if (item.parentIndexNumber != null) append("S${item.parentIndexNumber}")
                    if (item.indexNumber != null) {
                        if (isNotEmpty()) append(" ")
                        append("E${item.indexNumber}")
                    }
                    if (item.name != null) {
                        if (isNotEmpty()) append(": ")
                        append(item.name)
                    }
                }
                seasonEpisode.ifEmpty { null }
            }
            else -> item.overview
        }

        // Get poster image URI using JellyfinImage API
        val posterUri = when (item.type) {
            BaseItemKind.EPISODE -> {
                // For episodes, prefer series primary image
                item.seriesPrimaryImage?.let { image ->
                    Uri.parse(image.getUrl(api, maxWidth = 480))
                } ?: item.primaryImage?.let { image ->
                    Uri.parse(image.getUrl(api, maxWidth = 480))
                }
            }
            else -> {
                item.primaryImage?.let { image ->
                    Uri.parse(image.getUrl(api, maxWidth = 480))
                }
            }
        }

        return WatchNextProgram(
            internalId = internalId,
            serverId = serverId,
            itemId = itemId,
            title = title,
            subtitle = subtitle,
            posterUri = posterUri,
            previewVideoUri = null,
            durationMs = durationMs,
            lastPositionMs = positionMs
        )
    }

    /**
     * Create a stable internal ID for an item.
     */
    private fun createInternalId(item: BaseItemDto): String {
        return "jellyfin_${item.id}"
    }
}
