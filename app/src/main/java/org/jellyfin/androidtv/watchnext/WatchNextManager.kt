package org.jellyfin.androidtv.watchnext

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Entry point used by playback to publish progress into Android TV "Continue Watching".
 */
object WatchNextManager {
    private const val TAG = "WatchNext"

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    private fun client(context: Context) = WatchNextClient(context.applicationContext)

    /**
     * Publish or update a Watch Next program.
     */
    fun publish(context: Context, program: WatchNextProgram) {
        if (!isSupported()) return
        try {
            val c = client(context)
            val channelId = c.ensureChannel()
            if (channelId != 0L) c.upsertProgram(channelId, program)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to publish Watch Next program", t)
        }
    }

    /** Remove a Watch Next program (e.g., when completed). */
    fun remove(context: Context, internalId: String) {
        if (!isSupported()) return
        try {
            client(context).removeProgramByInternalId(internalId)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to remove Watch Next program", t)
        }
    }
}
