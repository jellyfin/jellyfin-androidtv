package org.jellyfin.androidtv.watchnext

import android.net.Uri

/**
 * Deep link contracts for Watch Next.
 */
object WatchNextIntents {
    private const val SCHEME = "jellyfin"
    private const val HOST = "watchnext"

    fun appLinkUri(): Uri = Uri.Builder()
        .scheme(SCHEME)
        .authority(HOST)
        .appendPath("open")
        .build()

    fun playbackUri(program: WatchNextProgram): Uri = Uri.Builder()
        .scheme(SCHEME)
        .authority(HOST)
        .appendPath("play")
        .appendQueryParameter("itemId", program.itemId)
        .appendQueryParameter("serverId", program.serverId)
        .appendQueryParameter("positionMs", program.lastPositionMs.toString())
        .build()
}
