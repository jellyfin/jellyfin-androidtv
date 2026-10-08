package org.jellyfin.androidtv.auth

import okhttp3.Interceptor
import okhttp3.Response
import org.jellyfin.androidtv.auth.repository.CustomHeadersRepository

/**
 * OkHttp interceptor that attaches the per-server custom headers resolved by
 * [CustomHeadersRepository] to every outgoing request. Installed on the shared OkHttp client so it
 * applies to SDK API calls, image loading and media playback alike.
 */
class CustomHeadersInterceptor(
	private val customHeadersRepository: CustomHeadersRepository,
) : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val headers = customHeadersRepository.headersFor(request.url)
		if (headers.isEmpty()) return chain.proceed(request)

		val builder = request.newBuilder()
		for ((name, value) in headers) builder.header(name, value)
		return chain.proceed(builder.build())
	}
}
