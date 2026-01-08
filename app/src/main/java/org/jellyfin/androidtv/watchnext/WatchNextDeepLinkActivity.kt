package org.jellyfin.androidtv.watchnext

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import org.jellyfin.androidtv.ui.playback.PlaybackManager

/**
 * Handles deep links from Android TV Watch Next into playback.
 *
 * URI format:
 * jellyfin://watchnext/play?serverId=...&itemId=...&positionMs=...
 */
class WatchNextDeepLinkActivity : Activity() {

    companion object {
        private const val TAG = "WatchNextDeepLink"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data == null) {
            finish()
            return
        }

        try {
            val path = data.pathSegments.firstOrNull()
            if (path == "open") {
                // Just open app main entry
                startActivity(packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
                finish()
                return
            }

            if (path != "play") {
                finish(); return
            }

            val serverId = data.getQueryParameter("serverId")
            val itemId = data.getQueryParameter("itemId")
            val positionMs = data.getQueryParameter("positionMs")?.toLongOrNull() ?: 0L

            if (serverId.isNullOrBlank() || itemId.isNullOrBlank()) {
                finish(); return
            }

            // Delegate to existing playback stack.
            PlaybackManager.getInstance(this).playFromWatchNext(serverId, itemId, positionMs)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to handle Watch Next deep link", t)
        } finally {
            finish()
        }
    }
}
