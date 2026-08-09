package org.jellyfin.androidtv.util

import android.media.MediaDrm
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Watches the HDCP level of the connected display and reports when the link drops.
 *
 * Some televisions keep HDMI hotplug detect asserted while powered off, so the device never sees
 * a display state change, a hotplug event or a CEC standby message and playback continues into a
 * dark room. The HDCP link does drop in that situation, which is why DRM protected apps stop on
 * their own. This gives unprotected playback the same signal.
 *
 * The poll interval is deliberately short. It bounds how long audio keeps playing after the
 * television is switched off.
 */
class HdcpMonitor(
	private val pollInterval: Duration = 5.seconds,
	private val onHdcpLost: () -> Unit,
) {
	companion object {
		private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)

		/**
		 * True once a real protected level has been observed. Prevents stopping playback on
		 * displays that never negotiate HDCP at all, where the level reads as none from the start.
		 *
		 * Process wide rather than per instance: a monitor created while the display link is
		 * already down would otherwise start with this unset and never stop playback, which is
		 * precisely the situation it needs to act on.
		 */
		private val sawProtectedLevel = AtomicBoolean(false)

		/**
		 * Consecutive readings of no protection required before playback is stopped. Changing
		 * video mode renegotiates HDCP, so a single reading can be a false positive.
		 */
		private const val REQUIRED_CONSECUTIVE_LOST = 2
	}

	private var job: Job? = null
	private val fired = AtomicBoolean(false)
	private var consecutiveLost = 0

	fun start(scope: CoroutineScope) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			Timber.i("HDCP monitor unavailable below API 28, not starting")
			return
		}

		if (job != null) return

		job = scope.launch(Dispatchers.IO) { watch() }
	}

	@RequiresApi(Build.VERSION_CODES.P)
	private suspend fun watch() {
		val drm = try {
			MediaDrm(WIDEVINE_UUID)
		} catch (error: Exception) {
			Timber.w(error, "Unable to open MediaDrm, HDCP monitoring disabled")
			return
		}

		try {
			while (currentCoroutineContext().isActive) {
				poll(drm)
				// Stop as soon as playback has been ended rather than waiting for the view to be
				// destroyed, so the session is always released even if teardown does not run.
				if (fired.get()) break
				delay(pollInterval)
			}
		} finally {
			Timber.i("HDCP monitor stopped, releasing session")
			drm.close()
		}
	}

	@RequiresApi(Build.VERSION_CODES.P)
	private suspend fun poll(drm: MediaDrm) {
		val level = try {
			drm.connectedHdcpLevel
		} catch (error: Exception) {
			Timber.w(error, "Unable to read connected HDCP level")
			return
		}

		val lost = level == MediaDrm.HDCP_NONE
		Timber.i(
			"HDCP level=%d lost=%b consecutiveLost=%d sawProtectedLevel=%b",
			level, lost, consecutiveLost, sawProtectedLevel.get()
		)

		if (!lost) {
			consecutiveLost = 0
			if (level != MediaDrm.HDCP_LEVEL_UNKNOWN) sawProtectedLevel.set(true)
			return
		}

		consecutiveLost++

		if (!sawProtectedLevel.get()) return
		if (consecutiveLost < REQUIRED_CONSECUTIVE_LOST) return
		if (!fired.compareAndSet(false, true)) return

		Timber.i("HDCP link lost, stopping playback")
		withContext(Dispatchers.Main) { onHdcpLost() }
	}

	fun stop() {
		job?.cancel()
		job = null
	}
}
