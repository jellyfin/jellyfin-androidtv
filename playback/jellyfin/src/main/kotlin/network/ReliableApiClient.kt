package org.jellyfin.playback.jellyfin.network

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.ApiClientFactory
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.HttpMethod
import org.jellyfin.sdk.api.client.RawResponse
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.api.sockets.SocketApi
import org.jellyfin.sdk.api.sockets.SocketConnectionFactory
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo

/** Keep SDK HTTP behavior while using a single lossless socket for this session's subscribers. */
class ReliableApiClient internal constructor(
	private val delegate: ApiClient,
	createSocketApi: (ApiClient) -> ReliableSocketApi,
) : ApiClient() {
	override val baseUrl get() = delegate.baseUrl
	override val accessToken get() = delegate.accessToken
	override val clientInfo get() = delegate.clientInfo
	override val deviceInfo get() = delegate.deviceInfo
	override val httpClientOptions get() = delegate.httpClientOptions

	private val socket = lazy { createSocketApi(this) }
	override val webSocket: SocketApi get() = socket.value

	override fun update(baseUrl: String?, accessToken: String?, clientInfo: ClientInfo, deviceInfo: DeviceInfo) {
		delegate.update(baseUrl, accessToken, clientInfo, deviceInfo)
		if (socket.isInitialized()) socket.value.notifyApiClientUpdate()
	}

	override suspend fun request(
		method: HttpMethod,
		pathTemplate: String,
		pathParameters: Map<String, Any?>,
		queryParameters: Map<String, Any?>,
		requestBody: Any?,
	): RawResponse = delegate.request(method, pathTemplate, pathParameters, queryParameters, requestBody)
}

class ReliableApiClientFactory(private val factory: OkHttpFactory) : ApiClientFactory {
	override fun create(
		baseUrl: String?,
		accessToken: String?,
		clientInfo: ClientInfo,
		deviceInfo: DeviceInfo,
		httpClientOptions: HttpClientOptions,
		socketConnectionFactory: SocketConnectionFactory,
	): ApiClient = ReliableApiClient(
		factory.create(baseUrl, accessToken, clientInfo, deviceInfo, httpClientOptions, socketConnectionFactory),
	) { api -> ReliableSocketApi(api, factory.createClient(httpClientOptions)) }
}
