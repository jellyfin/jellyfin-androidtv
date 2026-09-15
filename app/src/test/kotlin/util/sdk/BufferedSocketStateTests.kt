package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jellyfin.sdk.api.sockets.SocketConnectionState

@OptIn(ExperimentalCoroutinesApi::class)
class BufferedSocketStateTests : FunSpec({
	test("adjacent socket messages are delivered without StateFlow conflation") {
		runTest {
			val initial = SocketConnectionState.Disconnected()
			val state = BufferedSocketState(initial)
			val received = mutableListOf<SocketConnectionState>()
			val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
				state.take(4).collect(received::add)
			}

			val command = SocketConnectionState.Message("command")
			val update = SocketConnectionState.Message("state-update")
			val keepAlive = SocketConnectionState.Message("keep-alive")
			state.update(command)
			state.update(update)
			state.update(keepAlive)
			collector.join()

			received shouldBe listOf(initial, command, update, keepAlive)
		}
	}
})
