package org.jellyfin.playback.media3.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceException
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException

/**
 * Keeps retrying loads while the server is unreachable, for example when it went to sleep, so
 * playback continues once it is back instead of failing. Other errors fail after the same number
 * of retries as the [DefaultLoadErrorHandlingPolicy].
 */
@OptIn(UnstableApi::class)
class NetworkRetryLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {
	companion object {
		// The retry delay grows to 5 seconds, so this keeps retrying for at least 10 minutes
		const val MAX_NETWORK_ERROR_COUNT = 120

		@JvmStatic
		fun isNetworkError(exception: IOException) = exception is DataSourceException && (
			exception.reason == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
				exception.reason == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
			)
	}

	override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
		val maxErrorCount = when {
			isNetworkError(loadErrorInfo.exception) -> MAX_NETWORK_ERROR_COUNT
			else -> super.getMinimumLoadableRetryCount(loadErrorInfo.mediaLoadData.dataType)
		}

		if (loadErrorInfo.errorCount > maxErrorCount) return C.TIME_UNSET
		return super.getRetryDelayMsFor(loadErrorInfo)
	}

	// Errors are reported to the player once getRetryDelayMsFor stops retrying
	override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE
}
