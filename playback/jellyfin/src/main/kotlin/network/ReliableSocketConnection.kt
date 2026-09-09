package org.jellyfin.playback.jellyfin.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.api.sockets.SocketApiState
import org.jellyfin.sdk.model.api.ForceKeepAliveMessage
import org.jellyfin.sdk.model.api.InboundKeepAliveMessage
import org.jellyfin.sdk.model.api.InboundWebSocketMessage
import org.jellyfin.sdk.model.api.OutboundKeepAliveMessage
import org.jellyfin.sdk.model.api.OutboundWebSocketMessage
import timber.log.Timber
import java.io.IOException

/**
 * The SDK 1.8.12 socket stores incoming frames in StateFlow, which drops messages arriving in
 * the same burst (notably GroupJoined followed by Stop). SyncPlay requires every frame in order.
 * The session socket retains the SDK's authentication and typed message decoder,
 * with a bounded channel installed before opening the connection. Overflow disconnects explicitly
 * instead of continuing with a partial group state. All app events share this one connection:
 * the server sends each session message to its most recently active socket only.
 */
class ReliableSocketConnection(
	private val api: ApiClient,
	private val factory: WebSocket.Factory = OkHttpFactory().createClient(api.httpClientOptions),
) {
	private val _messages = MutableSharedFlow<OutboundWebSocketMessage>()
	val messages = _messages.asSharedFlow()
	private val _state = MutableStateFlow<SocketApiState>(SocketApiState.Disconnected())
	val state = _state.asStateFlow()
	@Volatile private var connection: Connection? = null

	fun send(message: InboundWebSocketMessage): Boolean =
		connection?.socket?.send(ApiSerializer.encodeSocketMessage(message)) == true

	/** Return after the server's first frame, before making the REST Create/Join request. */
	@Suppress("TooGenericExceptionCaught") // Decoding is the boundary of an ordered network stream.
	suspend fun connect(parentScope: CoroutineScope) {
		close()
		val session = Connection(CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])))
		connection = session
		_state.value = SocketApiState.Connecting
		session.scope.launch(start = CoroutineStart.UNDISPATCHED) {
			try {
				for (raw in session.frames) {
					val message = ApiSerializer.decodeSocketMessage(raw)
					if (connection !== session) break
					_state.value = SocketApiState.Connected
					session.connected.complete(Unit)
					resetWatchdog(session)
					when (message) {
						is ForceKeepAliveMessage -> startKeepAlive(session, message.data)
						is OutboundKeepAliveMessage -> Unit
						else -> _messages.emit(message)
					}
				}
			} catch (exception: CancellationException) {
				throw exception
			} catch (exception: Exception) {
				// A malformed frame invalidates this ordered stream; never continue after a gap.
				fail(session, exception)
			}
		}
		val request = Request.Builder()
			.url(api.createUrl("/socket"))
			.header("Authorization", AuthorizationHeaderBuilder.buildHeader(
				clientName = api.clientInfo.name,
				clientVersion = api.clientInfo.version,
				deviceId = api.deviceInfo.id,
				deviceName = api.deviceInfo.name,
				accessToken = requireNotNull(api.accessToken),
			))
			.build()
		session.socket = factory.newWebSocket(request, listener(session))
		if (connection !== session) session.socket?.cancel()
		session.connected.await()
	}

	@Synchronized
	fun close() {
		val old = connection
		connection = null
		old?.connected?.cancel()
		old?.frames?.cancel()
		old?.scope?.cancel()
		old?.socket?.cancel()
		_state.value = SocketApiState.Disconnected()
	}

	private fun listener(session: Connection) = object : WebSocketListener() {
		override fun onMessage(webSocket: WebSocket, text: String) {
			if (connection === session && session.frames.trySend(text).isFailure) {
				fail(session, IOException("SyncPlay socket message buffer exhausted"))
			}
		}

		override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
			webSocket.close(code, reason)
			fail(session, IOException("SyncPlay socket closing ($code)"))
		}

		override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
			fail(session, IOException("SyncPlay socket closed ($code)"))
		}

		override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
			fail(session, t)
		}
	}

	private fun startKeepAlive(session: Connection, timeoutSeconds: Int) {
		session.replyTimeoutMillis = timeoutSeconds.coerceAtLeast(1) * MILLIS_PER_DOUBLE_SECOND
		resetWatchdog(session)
		session.keepAlive?.cancel()
		session.keepAlive = session.scope.launch {
			val message = ApiSerializer.encodeSocketMessage(InboundKeepAliveMessage())
			while (isActive) {
				if (session.socket?.send(message) == false) {
					fail(session, IOException("SyncPlay keepalive could not be sent"))
					break
				}
				delay(timeoutSeconds.coerceAtLeast(1) * MILLIS_PER_HALF_SECOND)
			}
		}
	}

	private fun resetWatchdog(session: Connection) {
		session.watchdog?.cancel()
		val timeout = session.replyTimeoutMillis ?: return
		session.watchdog = session.scope.launch {
			delay(timeout)
			fail(session, IOException("SyncPlay keepalive reply timed out"))
		}
	}

	@Synchronized
	private fun fail(session: Connection, error: Throwable) {
		if (connection !== session) return
		connection = null
		Timber.w(error, "SyncPlay socket disconnected")
		session.connected.completeExceptionally(error)
		session.frames.cancel()
		session.scope.cancel()
		session.socket?.cancel()
		_state.value = SocketApiState.Disconnected(error)
	}

	private class Connection(val scope: CoroutineScope) {
		val frames = Channel<String>(FRAME_CAPACITY)
		val connected = CompletableDeferred<Unit>()
		@Volatile var socket: WebSocket? = null
		var keepAlive: Job? = null
		var watchdog: Job? = null
		var replyTimeoutMillis: Long? = null
	}

	private companion object {
		const val FRAME_CAPACITY = 256
		const val MILLIS_PER_HALF_SECOND = 500L
		const val MILLIS_PER_DOUBLE_SECOND = 2_000L
	}
}
