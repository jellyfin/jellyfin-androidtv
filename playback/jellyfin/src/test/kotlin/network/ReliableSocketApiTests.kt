@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.jellyfin.playback.jellyfin.network

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.sockets.SocketApiState
import org.jellyfin.sdk.api.sockets.SocketReconnectPolicy
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.OutboundWebSocketMessage
import org.jellyfin.sdk.model.api.SyncPlayCommandMessage
import kotlin.time.Duration.Companion.seconds

class ReliableSocketApiTests : FunSpec({
	test("buffered commands cannot cross logout and login even when the credentials are identical") {
		runTest {
			val fixture = ApiSocketFixture(this)
			val gate = CompletableDeferred<Unit>()
			val received = mutableListOf<Long?>()
			backgroundScope.launch {
				fixture.socket.subscribe(SyncPlayCommandMessage::class).collect {
					received += it.data?.positionTicks
					if (received.size == 1) gate.await()
				}
			}
			runCurrent()
			fixture.deliver(API_KEEP_ALIVE)
			fixture.deliver(apiCommand(1))
			runCurrent()
			repeat(10) { fixture.deliver(apiCommand(it + 2)) }
			runCurrent()
			val token = fixture.token
			fixture.token = null
			fixture.socket.notifyApiClientUpdate()
			fixture.token = token
			fixture.socket.notifyApiClientUpdate()
			runCurrent()
			fixture.listeners.size shouldBe 2
			fixture.deliver(API_KEEP_ALIVE)
			fixture.deliver(apiCommand(99))
			runCurrent()
			gate.complete(Unit)
			runCurrent()
			received shouldBe listOf(1L, 99L)
		}
	}

	test("general and SyncPlay subscribers share one socket and retain every command in a burst") {
		runTest {
			val fixture = ApiSocketFixture(this)
			val all = mutableListOf<OutboundWebSocketMessage>()
			val sync = mutableListOf<SyncPlayCommandMessage>()
			val general = backgroundScope.launch { fixture.socket.subscribeAll().collect { all += it } }
			val group = backgroundScope.launch { fixture.socket.subscribe(SyncPlayCommandMessage::class).collect { sync += it } }
			runCurrent()
			fixture.listeners.size shouldBe 1
			fixture.deliver(API_KEEP_ALIVE)
			repeat(40) { fixture.deliver(apiCommand(it)) }
			runCurrent()
			all.size shouldBe 40
			sync.map { it.data?.positionTicks } shouldBe (0L until 40L).toList()
			verify(exactly = 1) { fixture.webSocket.send(match<String> { it.contains("SessionsStart") && it.contains("0,1000") }) }
			group.cancel()
			runCurrent()
			verify(exactly = 0) { fixture.webSocket.cancel() }
			general.cancel()
			runCurrent()
			advanceTimeBy(5000)
			runCurrent()
			verify { fixture.webSocket.send(match<String> { it.contains("SessionsStop") }) }
			verify { fixture.webSocket.cancel() }
		}
	}

	test("credentials start the shared socket and changing or clearing them closes the old connection") {
		runTest {
			val fixture = ApiSocketFixture(this, authenticated = false)
			backgroundScope.launch { fixture.socket.subscribeAll().collect {} }
			runCurrent()
			fixture.listeners.size shouldBe 0
			fixture.token = "first-user"
			fixture.socket.notifyApiClientUpdate()
			runCurrent()
			fixture.deliver(API_KEEP_ALIVE)
			runCurrent()
			fixture.listeners.size shouldBe 1
			val old = fixture.listeners.last()
			fixture.token = "next-user"
			fixture.socket.notifyApiClientUpdate()
			runCurrent()
			fixture.listeners.size shouldBe 2
			fixture.deliver(API_KEEP_ALIVE)
			runCurrent()
			old.onFailure(fixture.webSocket, IllegalStateException("old session"), null)
			fixture.socket.state.value shouldBe SocketApiState.Connected
			fixture.token = null
			fixture.socket.notifyApiClientUpdate()
			runCurrent()
			advanceTimeBy(10000)
			runCurrent()
			fixture.listeners.size shouldBe 2
			fixture.socket.state.value shouldBe SocketApiState.Disconnected()
		}
	}

	test("connection failure reconnects once and keeps existing typed subscriptions") {
		runTest {
			val fixture = ApiSocketFixture(this)
			val received = mutableListOf<SyncPlayCommandMessage>()
			backgroundScope.launch { fixture.socket.subscribe(SyncPlayCommandMessage::class).collect { received += it } }
			runCurrent()
			fixture.deliver(API_KEEP_ALIVE)
			runCurrent()
			fixture.listeners.last().onFailure(fixture.webSocket, IllegalStateException("network lost"), null)
			runCurrent()
			advanceTimeBy(1000)
			runCurrent()
			fixture.listeners.size shouldBe 2
			fixture.deliver(API_KEEP_ALIVE)
			fixture.deliver(apiCommand(7))
			runCurrent()
			received.single().data?.positionTicks shouldBe 7L
		}
	}
})

private class ApiSocketFixture(scope: TestScope, authenticated: Boolean = true) {
	private val api = mockk<ApiClient>()
	private val factory = mockk<WebSocket.Factory>()
	val webSocket = mockk<WebSocket>(relaxed = true)
	val listeners = mutableListOf<WebSocketListener>()
	var token: String? = if (authenticated) "test-token" else null
	val socket: ReliableSocketApi

	init {
		every { api.baseUrl } returns "https://jellyfin.example.test"
		every { api.createUrl("/socket", any(), any(), any()) } returns "https://jellyfin.example.test/socket"
		every { api.clientInfo } returns ClientInfo("TV", "test")
		every { api.deviceInfo } returns DeviceInfo("tv-device", "Test TV")
		every { api.accessToken } answers { token }
		every { api.httpClientOptions } returns HttpClientOptions(socketReconnectPolicy = SocketReconnectPolicy.DelayReconnect(1.seconds))
		every { factory.newWebSocket(any(), any()) } answers {
			listeners += secondArg<WebSocketListener>()
			webSocket
		}
		every { webSocket.send(any<String>()) } returns true
		socket = ReliableSocketApi(api, factory, scope.backgroundScope)
	}

	fun deliver(raw: String) = listeners.last().onMessage(webSocket, raw)
}

private const val API_KEEP_ALIVE = """{"MessageType":"ForceKeepAlive","Data":60,"MessageId":"00000000000000000000000000000002"}"""
private fun apiCommand(position: Int) = """{
  "MessageType":"SyncPlayCommand","MessageId":"00000000000000000000000000000002",
  "Data":{
    "GroupId":"00000000000000000000000000000001","PlaylistItemId":"00000000000000000000000000000000",
    "When":"2026-09-09T19:42:22.435Z","PositionTicks":$position,"Command":"Stop","EmittedAt":"2026-09-09T19:42:22.435Z"
  }
}"""
