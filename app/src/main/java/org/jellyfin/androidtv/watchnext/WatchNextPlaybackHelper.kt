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
        // Threshold for considering an item "completed" (95%)
        private const val COMPLETION_THRESHOLD = 0.95
        // Minimum time change (in ms) to trigger an update
        private const val UPDATE_THRESHOLD_MS = 30_000L
    }
    
    // Track last update time per item to throttle database operations
    // Key: item ID string, Value: Pair(lastUpdateTimeMs, lastUpdatePositionMs)
    // Using ConcurrentHashMap for thread-safety
    private val throttleState = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Long>>()

    /**
     * Update or create a Watch Next program for the current item.
     * Called during playback to keep the Watch Next row updated.
     * Throttles updates to avoid excessive database writes.
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
        
        // Get throttling state for this specific item
        val itemIdStr = item.id.toString()
        val (lastUpdateTimeMs, lastUpdatePositionMs) = throttleState[itemIdStr] ?: (0L to 0L)
        
        // Throttle updates: only update if enough time has passed or position changed significantly
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastUpdateTimeMs
        val positionChange = kotlin.math.abs(positionMs - lastUpdatePositionMs)
        
        if (timeSinceLastUpdate < UPDATE_THRESHOLD_MS && positionChange < UPDATE_THRESHOLD_MS) {
            return
        }

        try {
            val program = createWatchNextProgram(item, positionMs, durationMs)
            WatchNextManager.publish(context, program)
            // Update throttling state for this item
            throttleState[itemIdStr] = currentTime to positionMs
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
            // Clean up throttling state for this item
            throttleState.remove(item.id.toString())
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
        // Note: Using baseUrl as serverId. This is not ideal as baseUrl can change
        // (e.g., IP or port changes), but the SDK's ApiClient doesn't expose a proper
        // server ID. In a multi-server setup, server switching should be handled before
        // Watch Next entries are created.
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
                    if (item.parentIndexNumber != null) append("S%02d".format(item.parentIndexNumber))
                    if (item.indexNumber != null) {
                        if (isNotEmpty()) append(" ")
                        append("E%02d".format(item.indexNumber))
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
