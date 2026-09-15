@file:OptIn(
	kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi::class,
	kotlinx.coroutines.InternalCoroutinesApi::class,
)

package org.jellyfin.androidtv.util.sdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.api.sockets.SocketConnection
import org.jellyfin.sdk.api.sockets.SocketConnectionFactory
import org.jellyfin.sdk.api.sockets.SocketConnectionState
import timber.log.Timber

/** Prevents adjacent WebSocket frames from being conflated by the SDK's StateFlow transport. */
class LosslessSocketConnectionFactory(private val factory: OkHttpFactory) : SocketConnectionFactory {
	override fun create(clientOptions: HttpClientOptions, scope: CoroutineScope): SocketConnection =
		LosslessSocketConnection(factory.createClient(clientOptions))
}

private class LosslessSocketConnection(private val client: OkHttpClient) : SocketConnection {
	private var webSocket: WebSocket? = null
	private val mutableState = BufferedSocketState(SocketConnectionState.Disconnected())
	override val state: StateFlow<SocketConnectionState> = mutableState

	private val listener = object : WebSocketListener() {
		override fun onMessage(webSocket: WebSocket, text: String) {
			mutableState.update(SocketConnectionState.Message(text))
		}

		override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
			if (this@LosslessSocketConnection.webSocket == webSocket) {
				this@LosslessSocketConnection.webSocket = null
				mutableState.update(SocketConnectionState.Disconnected())
			}
		}

		override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
			if (this@LosslessSocketConnection.webSocket == webSocket) {
				this@LosslessSocketConnection.webSocket = null
				mutableState.update(SocketConnectionState.Disconnected(t))
			}
		}
	}

	override suspend fun connect(
		url: String,
		clientName: String,
		clientVersion: String,
		deviceId: String,
		deviceName: String,
		accessToken: String,
	): Boolean {
		disconnect()
		val authorization = AuthorizationHeaderBuilder.buildHeader(
			clientName = clientName,
			clientVersion = clientVersion,
			deviceId = deviceId,
			deviceName = deviceName,
			accessToken = accessToken,
		)
		val request = Request.Builder().url(url)
			.header("Authorization", authorization)
			.header("User-Agent", "$clientName/$clientVersion via jellyfin-sdk-kotlin (OkHttp/${OkHttp.VERSION})")
			.build()
		mutableState.update(SocketConnectionState.Connecting)
		webSocket = client.newWebSocket(request, listener)
		return state.first { it != SocketConnectionState.Connecting } !is SocketConnectionState.Disconnected
	}

	override suspend fun send(message: String): Boolean = webSocket?.send(message) ?: false

	override suspend fun disconnect() {
		webSocket?.close(1000, null)
		webSocket = null
	}
}

@Suppress("OPT_IN_USAGE")
internal class BufferedSocketState(initial: SocketConnectionState) : StateFlow<SocketConnectionState> {
	private val events = MutableSharedFlow<SocketConnectionState>(
		replay = 1,
		extraBufferCapacity = Int.MAX_VALUE,
	)
	@Volatile private var current = initial

	init {
		events.tryEmit(initial)
	}

	override val value: SocketConnectionState get() = current
	override val replayCache: List<SocketConnectionState> get() = events.replayCache

	fun update(value: SocketConnectionState) {
		current = value
		if (!events.tryEmit(value)) Timber.w("Unable to buffer Jellyfin WebSocket state")
	}

	override suspend fun collect(collector: FlowCollector<SocketConnectionState>): Nothing = events.collect(collector)
}
