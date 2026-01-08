package org.jellyfin.androidtv.watchnext

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import org.jellyfin.androidtv.BuildConfig

/**
 * Small wrapper around TvContractCompat channels/programs for Android TV "Watch Next".
 *
 * Notes:
 * - Watch Next is implemented using the platform Preview Channel + Preview Programs. Marking
 *   programs as watchNextType makes them appear in the Watch Next row.
 * - Requires Android O (API 26) for TvProvider.
 */
@RequiresApi(Build.VERSION_CODES.O)
class WatchNextClient(private val context: Context) {

    companion object {
        private const val TAG = "WatchNext"
        // A stable internal provider id for the singleton channel.
        private const val CHANNEL_INTERNAL_PROVIDER_ID = "jellyfin_watch_next_channel"
    }

    private val resolver = context.contentResolver

    fun ensureChannel(): Long {
        // Try find by internal provider id
        val cursor = resolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            arrayOf(TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID),
            "${TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID}=?",
            arrayOf(CHANNEL_INTERNAL_PROVIDER_ID),
            null
        )

        cursor.use {
            if (it != null && it.moveToFirst()) {
                return it.getLong(0)
            }
        }

        val channelValues = ContentValues().apply {
            put(TvContractCompat.Channels.COLUMN_TYPE, TvContractCompat.Channels.TYPE_PREVIEW)
            put(TvContractCompat.Channels.COLUMN_DISPLAY_NAME, "Jellyfin")
            put(TvContractCompat.Channels.COLUMN_APP_LINK_INTENT_URI, WatchNextIntents.appLinkUri().toString())
            put(TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID, CHANNEL_INTERNAL_PROVIDER_ID)
        }

        val channelUri = resolver.insert(TvContractCompat.Channels.CONTENT_URI, channelValues)
        val channelId = channelUri?.lastPathSegment?.toLongOrNull() ?: 0L
        if (channelId == 0L) {
            Log.w(TAG, "Failed to insert Watch Next channel")
            return 0L
        }

        // Request it to be browsable in launcher.
        TvContractCompat.requestChannelBrowsable(context, channelId)
        return channelId
    }

    fun upsertProgram(channelId: Long, program: WatchNextProgram): Long {
        val existingId = findProgramIdByInternalId(program.internalId)

        val previewProgram = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
            .setTitle(program.title)
            .setDescription(program.subtitle)
            .setPosterArtUri(program.posterUri)
            .setPreviewVideoUri(program.previewVideoUri)
            .setIntentUri(WatchNextIntents.playbackUri(program).toString())
            .setInternalProviderId(program.internalId)
            .setLastPlaybackPositionMillis(program.lastPositionMs)
            .setDurationMillis(program.durationMs)
            .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
            .build()

        return if (existingId != null) {
            resolver.update(
                TvContractCompat.PreviewPrograms.CONTENT_URI,
                previewProgram.toContentValues(),
                "${TvContractCompat.PreviewPrograms._ID}=?",
                arrayOf(existingId.toString())
            )
            existingId
        } else {
            val uri = resolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, previewProgram.toContentValues())
            uri?.lastPathSegment?.toLongOrNull() ?: 0L
        }
    }

    fun removeProgramByInternalId(internalId: String) {
        val id = findProgramIdByInternalId(internalId) ?: return
        resolver.delete(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            "${TvContractCompat.PreviewPrograms._ID}=?",
            arrayOf(id.toString())
        )
    }

    fun removeAll() {
        resolver.delete(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            "${TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID} LIKE ?",
            arrayOf("%")
        )
    }

    @SuppressLint("Recycle")
    private fun findProgramIdByInternalId(internalId: String): Long? {
        val cursor = resolver.query(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            arrayOf(TvContractCompat.PreviewPrograms._ID, TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID),
            "${TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID}=?",
            arrayOf(internalId),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return null
    }
}
