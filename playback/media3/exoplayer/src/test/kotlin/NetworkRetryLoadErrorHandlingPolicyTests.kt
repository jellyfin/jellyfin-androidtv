package org.jellyfin.playback.media3.exoplayer

import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class NetworkRetryLoadErrorHandlingPolicyTests : FunSpec({
	val policy = NetworkRetryLoadErrorHandlingPolicy()
	val dataSpec = mockk<DataSpec>()

	fun retryDelayFor(exception: IOException, errorCount: Int) = policy.getRetryDelayMsFor(
		LoadErrorHandlingPolicy.LoadErrorInfo(
			mockk<LoadEventInfo>(),
			MediaLoadData(C.DATA_TYPE_MEDIA),
			exception,
			errorCount,
		)
	)

	fun networkError(cause: IOException) = HttpDataSource.HttpDataSourceException.createForIOException(
		cause,
		dataSpec,
		HttpDataSource.HttpDataSourceException.TYPE_OPEN,
	)

	test("Unreachable server is retried until MAX_NETWORK_ERROR_COUNT") {
		for (exception in listOf(networkError(ConnectException()), networkError(SocketTimeoutException()))) {
			retryDelayFor(exception, 1) shouldBe 0
			retryDelayFor(exception, 10) shouldBe 5000
			retryDelayFor(exception, NetworkRetryLoadErrorHandlingPolicy.MAX_NETWORK_ERROR_COUNT) shouldBe 5000
			retryDelayFor(exception, NetworkRetryLoadErrorHandlingPolicy.MAX_NETWORK_ERROR_COUNT + 1) shouldBe C.TIME_UNSET
		}
	}

	test("Other errors stop retrying after the default retry count") {
		val exception = HttpDataSource.InvalidResponseCodeException(500, null, null, emptyMap(), dataSpec, byteArrayOf())

		retryDelayFor(exception, 3) shouldNotBe C.TIME_UNSET
		retryDelayFor(exception, 4) shouldBe C.TIME_UNSET
	}

	test("Errors are only reported to the player once retrying stops") {
		policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA) shouldBe Int.MAX_VALUE
	}
})
