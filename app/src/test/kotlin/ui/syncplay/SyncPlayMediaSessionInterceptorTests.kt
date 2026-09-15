package org.jellyfin.androidtv.ui.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jellyfin.playback.core.PlaybackSeekCommand
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayClient
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayRequest
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayStatus
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupStateType
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.PlayQueueUpdateReason
import org.jellyfin.sdk.model.api.SyncPlayQueueItem
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SyncPlayMediaSessionInterceptorTests : FunSpec({
	test("solo media session commands are not intercepted") {
		runTest {
			val client = InterceptorClient(joined = false)
			val interceptor = SyncPlayMediaSessionInterceptor(client, this)

			interceptor.setPlaying(true) shouldBe false
			interceptor.seek(3.seconds, PlaybackSeekCommand.POSITION) shouldBe false
			client.requests.size shouldBe 0
		}
	}

	test("joined media session commands are routed to the group") {
		runTest {
			val client = InterceptorClient(joined = true)
			val interceptor = SyncPlayMediaSessionInterceptor(client, this)

			interceptor.setPlaying(false) shouldBe true
			interceptor.setPlaying(true) shouldBe true
			interceptor.seek(3.seconds, PlaybackSeekCommand.POSITION) shouldBe true
			interceptor.seek(0.seconds, PlaybackSeekCommand.NEXT) shouldBe true
			interceptor.stop() shouldBe true
			advanceUntilIdle()

			client.requests[0] shouldBe SyncPlayRequest.Pause
			client.requests[1] shouldBe SyncPlayRequest.Unpause
			client.requests[2] shouldBe SyncPlayRequest.Seek(30_000_000L)
			client.requests[3].shouldBeInstanceOf<SyncPlayRequest.Next>()
			client.haltCount shouldBe 1
		}
	}

	test("local speed repeat and shuffle mutations are blocked while joined") {
		runTest {
			val interceptor = SyncPlayMediaSessionInterceptor(InterceptorClient(joined = true), this)

			interceptor.setSpeed(2f) shouldBe true
			interceptor.setRepeat(true) shouldBe true
			interceptor.setShuffle(true) shouldBe true
		}
	}
})

private class InterceptorClient(joined: Boolean) : SyncPlayClient {
	private val occurrence = UUID.randomUUID()
	override val state = MutableStateFlow(
		SyncPlayStatus(
			group = if (joined) GroupInfoDto(
				UUID.randomUUID(), "Group", GroupStateType.PLAYING, emptyList(), LocalDateTime.MIN
			) else null,
			queue = if (joined) PlayQueueUpdate(
				PlayQueueUpdateReason.NEW_PLAYLIST,
				LocalDateTime.MIN,
				listOf(SyncPlayQueueItem(UUID.randomUUID(), occurrence)),
				0,
				0,
				true,
				org.jellyfin.sdk.model.api.GroupShuffleMode.SORTED,
				org.jellyfin.sdk.model.api.GroupRepeatMode.REPEAT_NONE,
			) else null,
			following = joined,
		)
	)
	val requests = mutableListOf<SyncPlayRequest>()
	var haltCount = 0

	override suspend fun groups() = emptyList<GroupInfoDto>()
	override suspend fun create(name: String) = false
	override suspend fun join(id: UUID) = false
	override suspend fun leave() = Unit
	override suspend fun halt() { haltCount++ }
	override suspend fun resume() = Unit
	override suspend fun request(request: SyncPlayRequest): Boolean { requests += request; return true }
	override fun close() = Unit
}
