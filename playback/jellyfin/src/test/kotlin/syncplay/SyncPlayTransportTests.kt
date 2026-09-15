package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.HttpMethod
import org.jellyfin.sdk.api.client.RawResponse
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.api.sockets.SocketApi
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.SetShuffleModeRequestDto
import org.jellyfin.sdk.model.api.SetRepeatModeRequestDto
import org.jellyfin.sdk.model.api.RemoveFromPlaylistRequestDto
import org.jellyfin.sdk.model.api.ReadyRequestDto
import org.jellyfin.sdk.model.api.QueueRequestDto
import org.jellyfin.sdk.model.api.PlayRequestDto
import org.jellyfin.sdk.model.api.MovePlaylistItemRequestDto
import org.jellyfin.sdk.model.api.GroupShuffleMode
import org.jellyfin.sdk.model.api.GroupRepeatMode
import org.jellyfin.sdk.model.api.GroupQueueMode
import org.jellyfin.sdk.model.api.OutboundWebSocketMessage
import org.jellyfin.sdk.model.api.SyncPlayCommandMessage
import org.jellyfin.sdk.model.api.SyncPlayGroupJoinedUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupUpdateMessage
import java.io.IOException

class SyncPlayTransportTests : FunSpec({
	test("W01 pause resume stop and seek use Jellyfin endpoints with 64-bit positions") {
		val api = RecordingApiClient()
		val transport = SdkSyncPlayTransport(api)
		transport.send(SyncPlayRequest.Pause)
		transport.send(SyncPlayRequest.Unpause)
		transport.send(SyncPlayRequest.Stop)
		transport.send(SyncPlayRequest.Seek(30_000_000_000_001))
		api.requests.map { it.path } shouldBe listOf("/SyncPlay/Pause", "/SyncPlay/Unpause", "/SyncPlay/Stop", "/SyncPlay/Seek")
		api.requests.all { it.method == HttpMethod.POST } shouldBe true
		api.requests.last().body shouldBe """{"PositionTicks":30000000000001}"""
		api.createUrl(api.requests.last().path) shouldBe "https://example.test/jellyfin/SyncPlay/Seek"
	}

	test("W01 queue navigation uses occurrence IDs and halt uses IgnoreWait rather than Stop") {
		val api = RecordingApiClient()
		val transport = SdkSyncPlayTransport(api)
		transport.send(SyncPlayRequest.Next(occurrenceId))
		transport.send(SyncPlayRequest.Previous(occurrenceId))
		transport.send(SyncPlayRequest.IgnoreWait(true))
		api.requests.map { it.path } shouldBe listOf("/SyncPlay/NextItem", "/SyncPlay/PreviousItem", "/SyncPlay/SetIgnoreWait")
		api.requests.first().body shouldBe """{"PlaylistItemId":"00000000-0000-0000-0000-000000000004"}"""
		api.requests.last().body shouldBe """{"IgnoreWait":true}"""
	}

	test("W01 readiness reports corrected time actual state and zero position") {
		val api = RecordingApiClient()
		val transport = SdkSyncPlayTransport(api)
		transport.send(SyncPlayRequest.Ready(date(2000), 0, false, occurrenceId))
		val request = api.requests.single()
		request.path shouldBe "/SyncPlay/Ready"
		val dto = ApiSerializer.decodeResponseBody<ReadyRequestDto>(requireNotNull(request.body))
		dto.positionTicks shouldBe 0L
		dto.isPlaying shouldBe false
		dto.playlistItemId shouldBe occurrenceId
		dto.`when`.toSyncPlayInstant().toEpochMilli() shouldBe 2000L
	}

	test("W01 create and list deserialize SDK responses and join leave have distinct requests") {
		val api = RecordingApiClient()
		val transport = SdkSyncPlayTransport(api)
		val groupJson = requireNotNull(ApiSerializer.encodeRequestBody(group()))
		api.response = groupJson
		transport.createGroup("Watch together") shouldBe group()
		api.requests.last().body shouldBe """{"GroupName":"Watch together"}"""
		api.response = "[$groupJson]"
		transport.groups() shouldBe listOf(group())
		api.response = ""
		transport.joinGroup(groupId)
		transport.leaveGroup()
		api.requests.map { it.path } shouldBe listOf("/SyncPlay/New", "/SyncPlay/List", "/SyncPlay/Join", "/SyncPlay/Leave")
	}

	test("M05 transport propagates failure without retrying a mutation") {
		val api = RecordingApiClient().apply { failure = IOException("offline") }
		shouldThrow<IOException> { SdkSyncPlayTransport(api).send(SyncPlayRequest.Next(occurrenceId)) }
		api.requests.size shouldBe 1
	}

	test("W02 decodes SDK wire messages and preserves group-command delivery order") {
		val joined = ApiSerializer.decodeSocketMessage("""{
			"MessageType":"SyncPlayGroupUpdate","MessageId":"00000000000000000000000000000010",
			"Data":{"GroupId":"00000000000000000000000000000001","Type":"GroupJoined",
			"Data":{"GroupId":"00000000000000000000000000000001","GroupName":"Watch together",
			"State":"Paused","Participants":["Alex"],"LastUpdatedAt":"1970-01-01T00:00:01Z"}}}
		""")
		joined.shouldBeTypeOf<SyncPlayGroupUpdateMessage>().data.shouldBeTypeOf<SyncPlayGroupJoinedUpdate>()
		val command = ApiSerializer.decodeSocketMessage("""{
			"MessageType":"SyncPlayCommand","MessageId":"00000000000000000000000000000011",
			"Data":{"GroupId":"00000000000000000000000000000001","PlaylistItemId":"00000000000000000000000000000004",
			"When":"1970-01-01T00:00:02Z","EmittedAt":"1970-01-01T00:00:01.200Z","Command":"Unpause","PositionTicks":0}}
		""")
		command.shouldBeTypeOf<SyncPlayCommandMessage>().data shouldBe command()
		val api = RecordingApiClient()
		every { api.webSocket.subscribe(OutboundWebSocketMessage::class) } returns flowOf(joined, command)
		val events = SdkSyncPlayTransport(api).events.toList()
		events[0].shouldBeTypeOf<SyncPlayEvent.GroupUpdate>()
		events[1].shouldBeTypeOf<SyncPlayEvent.Command>()
	}
	test("W01 remaining queue operations buffering and ping map to the pinned SDK contract") {
		val api = RecordingApiClient()
		val transport = SdkSyncPlayTransport(api)
		val requests = listOf(
			SyncPlayRequest.Select(occurrenceId) to "/SyncPlay/SetPlaylistItem",
			SyncPlayRequest.SetQueue(PlayRequestDto(listOf(itemId, itemId), 1, 0)) to "/SyncPlay/SetNewQueue",
			SyncPlayRequest.Queue(QueueRequestDto(listOf(itemId), GroupQueueMode.QUEUE_NEXT)) to "/SyncPlay/Queue",
			SyncPlayRequest.Remove(RemoveFromPlaylistRequestDto(listOf(occurrenceId), false, false)) to "/SyncPlay/RemoveFromPlaylist",
			SyncPlayRequest.Move(MovePlaylistItemRequestDto(occurrenceId, 0)) to "/SyncPlay/MovePlaylistItem",
			SyncPlayRequest.Repeat(SetRepeatModeRequestDto(GroupRepeatMode.REPEAT_ALL)) to "/SyncPlay/SetRepeatMode",
			SyncPlayRequest.Shuffle(SetShuffleModeRequestDto(GroupShuffleMode.SHUFFLE)) to "/SyncPlay/SetShuffleMode",
			SyncPlayRequest.Buffering(date(2000), 0, false, occurrenceId) to "/SyncPlay/Buffering",
			SyncPlayRequest.Ping(20) to "/SyncPlay/Ping",
		)
		for ((request, path) in requests) {
			transport.send(request)
			api.requests.last().path shouldBe path
			api.requests.last().method shouldBe HttpMethod.POST
		}
		api.requests.last().body shouldBe """{"Ping":20}"""
		val play = ApiSerializer.decodeResponseBody<PlayRequestDto>(requireNotNull(api.requests[1].body))
		play.playingQueue shouldBe listOf(itemId, itemId)
		play.playingItemPosition shouldBe 1
		play.startPositionTicks shouldBe 0L
	}

	test("W01 server time uses the root endpoint and retains both timestamps") {
		val api = RecordingApiClient().apply {
			response = """{"RequestReceptionTime":"1970-01-01T00:00:01.120Z","ResponseTransmissionTime":"1970-01-01T00:00:01.130Z"}"""
		}
		val result = SdkSyncPlayTransport(api).time()
		result.requestReceptionTime.toSyncPlayInstant().toEpochMilli() shouldBe 1120L
		result.responseTransmissionTime.toSyncPlayInstant().toEpochMilli() shouldBe 1130L
		api.requests.single().path shouldBe "/GetUtcTime"
		api.requests.single().method shouldBe HttpMethod.GET
	}

})

private class RecordingApiClient : ApiClient() {
	data class Request(val method: HttpMethod, val path: String, val body: String?)
	val requests = mutableListOf<Request>()
	var response = ""
	var failure: IOException? = null
	override val baseUrl = "https://example.test/jellyfin"
	override val accessToken = "test-token"
	override val clientInfo = ClientInfo("Tests", "1")
	override val deviceInfo = DeviceInfo("tests", "Tests")
	override val httpClientOptions = HttpClientOptions()
	override val webSocket = mockk<SocketApi>()
	override fun update(baseUrl: String?, accessToken: String?, clientInfo: ClientInfo, deviceInfo: DeviceInfo) = Unit
	override suspend fun request(
		method: HttpMethod,
		pathTemplate: String,
		pathParameters: Map<String, Any?>,
		queryParameters: Map<String, Any?>,
		requestBody: Any?,
	): RawResponse {
		requests += Request(method, pathTemplate, ApiSerializer.encodeRequestBody(requestBody))
		failure?.let { throw it }
		return RawResponse(response.toByteArray(), if (response.isEmpty()) 204 else 200, emptyMap())
	}
}
