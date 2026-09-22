package org.jellyfin.androidtv.util.sdk

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.jellyfin.sdk.model.extensions.inWholeTicks
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val STORED_POSITION_SECONDS = 30
private const val RESUME_SUBTRACT_SECONDS = 5
private const val SUPPLIED_POSITION_SECONDS = 12

class SdkPlaybackHelperTests : FunSpec({
	test("remote lookup requests playback fields and keeps the supplied launch arguments") {
		val supplied = SUPPLIED_POSITION_SECONDS.seconds
		val first = libraryItem(BaseItemKind.MOVIE)
		val second = libraryItem(BaseItemKind.EPISODE, mediaSources = null)
		val items = listOf(first, second)

		withHelper(items = items, hasFragment = true) { fixture ->
			fixture.play(
				itemIds = items.map { it.id },
				shuffle = true,
				position = supplied.inWholeTicks,
				index = 1,
			)

			fixture.launches.single() shouldBe LaunchRequest(
				context = fixture.context,
				items = items,
				position = supplied.inWholeMilliseconds.toInt(),
				replace = true,
				index = 1,
				shuffle = true,
			)
			fixture.requestedIds shouldBe items.map { it.id }
			fixture.requestedFields shouldBe ItemRepository.itemFields
		}
	}

	test("remote lookup resumes the first item when start position and index are omitted") {
		val stored = STORED_POSITION_SECONDS.seconds
		val subtract = RESUME_SUBTRACT_SECONDS.seconds
		val itemId = UUID.randomUUID()
		val item = libraryItem(
			kind = BaseItemKind.MOVIE,
			userData = userData(itemId, stored),
		)

		withHelper(
			items = listOf(item),
			resumeSubtractSeconds = RESUME_SUBTRACT_SECONDS,
		) { fixture ->
			fixture.play(itemIds = listOf(itemId))

			fixture.launches.single() shouldBe LaunchRequest(
				context = fixture.context,
				items = listOf(item),
				position = (stored - subtract).inWholeMilliseconds.toInt(),
				replace = false,
				index = 0,
				shuffle = false,
			)
		}
	}

	test("a supplied start position of zero is not replaced by the resume point") {
		val itemId = UUID.randomUUID()
		val item = libraryItem(
			kind = BaseItemKind.MOVIE,
			userData = userData(itemId, STORED_POSITION_SECONDS.seconds),
		)

		withHelper(
			items = listOf(item),
			resumeSubtractSeconds = RESUME_SUBTRACT_SECONDS,
		) { fixture ->
			fixture.play(itemIds = listOf(itemId), position = 0L)

			fixture.launches.single().position shouldBe 0
			fixture.launches.single().index shouldBe 0
		}
	}

	test("an empty remote lookup does not read the first item or launch playback") {
		val requestedId = UUID.randomUUID()

		withHelper(items = emptyList()) { fixture ->
			fixture.play(itemIds = listOf(requestedId))

			fixture.launches shouldBe emptyList()
			fixture.requestedIds shouldBe listOf(requestedId)
			fixture.requestedFields shouldBe ItemRepository.itemFields
			verify(exactly = 1) {
				Toast.makeText(fixture.context, R.string.msg_no_playable_items, Toast.LENGTH_LONG)
			}
			verify(exactly = 1) { fixture.toast.show() }
		}
	}
})

private data class LaunchRequest(
	val context: Context,
	val items: List<BaseItemDto>,
	val position: Int?,
	val replace: Boolean,
	val index: Int,
	val shuffle: Boolean,
)

private class PlaybackFixture(
	val context: Context,
	val toast: Toast,
	val helper: SdkPlaybackHelper,
	private val recorded: RecordedPlayback,
) {
	val launches: List<LaunchRequest>
		get() = recorded.launches

	val requestedIds: Collection<UUID>
		get() = recorded.idsSlot.captured

	val requestedFields: Collection<ItemFields>
		get() = recorded.fieldsSlot.captured

	suspend fun play(
		itemIds: List<UUID>,
		shuffle: Boolean = false,
		position: Long? = null,
		index: Int? = null,
	) {
		helper.retrieveAndPlay(itemIds, shuffle, position, index, context)
		check(recorded.done.await(5, TimeUnit.SECONDS)) { "Remote playback did not finish" }
		recorded.supervisor.children.toList().forEach { it.join() }
		recorded.errors.firstOrNull()?.let { throw it }
	}
}

private class RecordedPlayback(
	val launches: MutableList<LaunchRequest>,
	val idsSlot: CapturingSlot<Collection<UUID>>,
	val fieldsSlot: CapturingSlot<Collection<ItemFields>>,
	val done: CountDownLatch,
	val supervisor: Job,
	val errors: List<Throwable>,
)

private suspend fun withHelper(
	items: List<BaseItemDto>,
	resumeSubtractSeconds: Int = 0,
	hasFragment: Boolean = false,
	block: suspend (PlaybackFixture) -> Unit,
) {
	val done = CountDownLatch(1)
	val errors = mutableListOf<Throwable>()
	val supervisor = SupervisorJob()
	val handler = CoroutineExceptionHandler { _, throwable ->
		errors += throwable
		done.countDown()
	}
	val context = mockk<LifecycleContext>(relaxed = true)
	val scope = mockk<LifecycleCoroutineScope>()
	every { scope.coroutineContext } returns supervisor + Dispatchers.Unconfined + handler

	val toast = mockk<Toast>(relaxed = true)
	val launches = mutableListOf<LaunchRequest>()
	val idsSlot = slot<Collection<UUID>>()
	val fieldsSlot = slot<Collection<ItemFields>>()
	val itemsApi = mockk<ItemsApi>()
	val api = mockk<ApiClient>()
	val userPreferences = mockk<UserPreferences>()
	val playbackLauncher = mockk<PlaybackLauncher>()
	val container = PlaybackControllerContainer()
	if (hasFragment) {
		container.playbackController = mockk<PlaybackController> {
			every { hasFragment() } returns true
		}
	}

	mockkStatic(Toast::class)
	mockkStatic("androidx.lifecycle.LifecycleOwnerKt")
	try {
		every { Toast.makeText(any<Context>(), any<Int>(), any<Int>()) } returns toast
		every { toast.show() } answers { done.countDown() }
		every { context.lifecycleScope } returns scope
		every { userPreferences[UserPreferences.resumeSubtractDuration] } returns resumeSubtractSeconds.toString()
		every { api.getOrCreateApi(ItemsApi::class, any()) } returns itemsApi
		coEvery {
			itemsApi.getItems(ids = capture(idsSlot), fields = capture(fieldsSlot))
		} returns itemsResponse(items)
		recordLaunches(playbackLauncher, launches, done)

		val fixture = PlaybackFixture(
			context = context,
			toast = toast,
			helper = SdkPlaybackHelper(api, userPreferences, playbackLauncher, container),
			recorded = RecordedPlayback(launches, idsSlot, fieldsSlot, done, supervisor, errors),
		)
		block(fixture)
	} finally {
		supervisor.cancel()
		unmockkStatic(Toast::class)
		unmockkStatic("androidx.lifecycle.LifecycleOwnerKt")
	}
}

private fun recordLaunches(
	playbackLauncher: PlaybackLauncher,
	launches: MutableList<LaunchRequest>,
	done: CountDownLatch,
) {
	every {
		playbackLauncher.launch(any(), any(), any(), any(), any(), any())
	} answers {
		launches += LaunchRequest(
			context = invocation.args[0] as Context,
			items = invocation.args[1].asItemList(),
			position = invocation.args[2] as Int?,
			replace = invocation.args[3] as Boolean,
			index = invocation.args[4] as Int,
			shuffle = invocation.args[5] as Boolean,
		)
		done.countDown()
	}
}

private fun Any?.asItemList(): List<BaseItemDto> {
	val values = this as List<*>
	return values.map { it as BaseItemDto }
}

private fun itemsResponse(items: List<BaseItemDto>) = Response(
	content = BaseItemDtoQueryResult(
		items = items,
		totalRecordCount = items.size,
		startIndex = 0,
	),
	status = 200,
	headers = emptyMap(),
)

private fun libraryItem(
	kind: BaseItemKind,
	mediaSources: List<MediaSourceInfo>? = listOf(mediaSource()),
	userData: UserItemDataDto? = null,
) = BaseItemDto(
	id = userData?.itemId ?: UUID.randomUUID(),
	type = kind,
	mediaType = MediaType.VIDEO,
	mediaSources = mediaSources,
	userData = userData,
)

private fun userData(itemId: UUID, position: Duration) = UserItemDataDto(
	playbackPositionTicks = position.inWholeTicks,
	playCount = 0,
	isFavorite = false,
	played = false,
	key = "resume",
	itemId = itemId,
)

private fun mediaSource() = MediaSourceInfo(
	protocol = MediaProtocol.FILE,
	type = MediaSourceType.DEFAULT,
	isRemote = false,
	readAtNativeFramerate = false,
	ignoreDts = false,
	ignoreIndex = false,
	genPtsInput = false,
	supportsTranscoding = true,
	supportsDirectStream = true,
	supportsDirectPlay = true,
	isInfiniteStream = false,
	requiresOpening = false,
	requiresClosing = false,
	requiresLooping = false,
	supportsProbing = true,
	transcodingSubProtocol = MediaStreamProtocol.HTTP,
	hasSegments = false,
)

private abstract class LifecycleContext : Context(), LifecycleOwner
