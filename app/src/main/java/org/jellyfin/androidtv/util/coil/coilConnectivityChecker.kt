package org.jellyfin.androidtv.util.coil

import android.content.Context
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ConnectivityChecker

/**
 * Coil uses a "connectivity checker" to skip the network if it thinks there is no available connection. In Android this is sometimes
 * detected by pinging a known host (e.g. google.com) which might fail for users that blackhole these hosts or only use the app on their
 * local network. This implementation of the connectivity checker will always return `true` to force a network request to be made.
 */
@Suppress("UNUSED_PARAMETER")
@ExperimentalCoilApi
fun createCoilConnectivityChecker(context: Context) = ConnectivityChecker { true }
