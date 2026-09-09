package org.jellyfin.playback.jellyfin.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.WebSocket
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.sockets.SocketApi
import org.jellyfin.sdk.api.sockets.SocketApiState
import org.jellyfin.sdk.model.api.ActivityLogEntryMessage
import org.jellyfin.sdk.model.api.ActivityLogEntryStartMessage
import org.jellyfin.sdk.model.api.ActivityLogEntryStopMessage
import org.jellyfin.sdk.model.api.InboundWebSocketMessage
import org.jellyfin.sdk.model.api.OutboundWebSocketMessage
import org.jellyfin.sdk.model.api.ScheduledTasksInfoMessage
import org.jellyfin.sdk.model.api.ScheduledTasksInfoStartMessage
import org.jellyfin.sdk.model.api.ScheduledTasksInfoStopMessage
import org.jellyfin.sdk.model.api.SessionsMessage
import org.jellyfin.sdk.model.api.SessionsStartMessage
import org.jellyfin.sdk.model.api.SessionsStopMessage
import org.jellyfin.sdk.model.socket.PeriodicListenerPeriod
import timber.log.Timber
import kotlin.reflect.KClass

/**
 * SocketApi-compatible lifecycle around one ordered transport for the entire authenticated session.
 * Its message flow never passes through StateFlow; only connection status and credentials do.
 * Subscription types and their default period match SDK 1.8.12's DefaultSocketApi.
 */
class ReliableSocketApi(
	private val api: ApiClient,
	factory: WebSocket.Factory,
	private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : SocketApi {
	private val connection = ReliableSocketConnection(api, factory)
	override val state = connection.state
	private data class Credentials(val generation: Long, val identity: List<Any?>?)
	private val credentials = MutableStateFlow(Credentials(0, credentials()))
	private val subscriptions = mutableMapOf<Subscription, Int>()
	private val policy = api.httpClientOptions.socketReconnectPolicy

	private data class ReceivedMessage(val identity: Credentials, val message: OutboundWebSocketMessage)

	private val messages = channelFlow {
		try {
			credentials.collectLatest { identity ->
				connection.close()
				if (identity.identity == null) return@collectLatest
				policy.notifyUpdated()
				coroutineScope {
					// Register before connecting, and cancel the receiver with its credentials.
					launch(start = CoroutineStart.UNDISPATCHED) {
						connection.messages.collect { send(ReceivedMessage(identity, it)) }
					}
					while (isActive) {
						maintainConnection(this)
						policy.notifyDisconnected()
						val retryDelay = policy.getReconnectDelay() ?: awaitCancellation()
						delay(retryDelay)
					}
				}
			}
		} finally {
			connection.close()
		}
	}.shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS))

	fun notifyApiClientUpdate() {
		val identity = credentials()
		credentials.update { previous ->
			if (previous.identity == identity) previous else Credentials(previous.generation + 1, identity)
		}
	}

	@Suppress("TooGenericExceptionCaught") // A failed network attempt uses the SDK's reconnect policy.
	private suspend fun maintainConnection(parentScope: CoroutineScope) {
		try {
			withTimeout(CONNECT_TIMEOUT_MILLIS) { connection.connect(parentScope) }
			policy.notifyConnected()
			synchronized(subscriptions) {
				for (type in subscriptions.keys) connection.send(type.start())
			}
			state.first { it is SocketApiState.Disconnected }
		} catch (exception: TimeoutCancellationException) {
			Timber.w(exception, "WebSocket connection timed out")
		} catch (exception: CancellationException) {
			throw exception
		} catch (exception: Exception) {
			Timber.w(exception, "WebSocket connection failed")
		} finally {
			connection.close()
		}
	}

	override fun subscribeAll(): Flow<OutboundWebSocketMessage> = subscribe(Subscription.entries.toSet())

	override fun <T : OutboundWebSocketMessage> subscribe(messageType: KClass<T>): Flow<T> =
		subscribe(Subscription.entries.filter { it.messageType == messageType }.toSet()).filterIsInstance(messageType)

	private fun subscribe(types: Set<Subscription>): Flow<OutboundWebSocketMessage> = flow {
		updateSubscriptions(types, add = true)
		try {
			messages.collect {
				// Buffered messages must not cross a server/account change.
				if (it.identity == credentials.value) emit(it.message)
			}
		} finally {
			updateSubscriptions(types, add = false)
		}
	}

	private fun updateSubscriptions(types: Set<Subscription>, add: Boolean) = synchronized(subscriptions) {
		for (type in types) {
			val before = subscriptions[type] ?: 0
			val after = before + if (add) 1 else -1
			if (after > 0) subscriptions[type] = after else subscriptions.remove(type)
			if (state.value is SocketApiState.Connected) {
				if (before == 0 && after > 0) connection.send(type.start())
				if (before > 0 && after == 0) connection.send(type.stop())
			}
		}
	}

	private fun credentials(): List<Any?>? {
		if (api.baseUrl == null || api.accessToken == null) return null
		return listOf(api.baseUrl, api.accessToken, api.clientInfo, api.deviceInfo)
	}

	private enum class Subscription(val messageType: KClass<out OutboundWebSocketMessage>) {
		SESSIONS(SessionsMessage::class),
		ACTIVITY_LOG(ActivityLogEntryMessage::class),
		SCHEDULED_TASKS(ScheduledTasksInfoMessage::class);

		fun start(): InboundWebSocketMessage {
			val period = PeriodicListenerPeriod().toString()
			return when (this) {
				SESSIONS -> SessionsStartMessage(period)
				ACTIVITY_LOG -> ActivityLogEntryStartMessage(period)
				SCHEDULED_TASKS -> ScheduledTasksInfoStartMessage(period)
			}
		}

		fun stop(): InboundWebSocketMessage = when (this) {
			SESSIONS -> SessionsStopMessage()
			ACTIVITY_LOG -> ActivityLogEntryStopMessage()
			SCHEDULED_TASKS -> ScheduledTasksInfoStopMessage()
		}
	}

	private companion object {
		const val CONNECT_TIMEOUT_MILLIS = 10_000L
		const val STOP_TIMEOUT_MILLIS = 5_000L
	}
}
