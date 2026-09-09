package org.jellyfin.androidtv.ui.playback.rewrite

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlayerState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayService
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupRepeatMode
import org.jellyfin.sdk.model.api.GroupShuffleMode
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import java.util.UUID

class SyncPlayAudioControlsTests : FunSpec({
	test("queueing audio in a group waits for the server queue instead of appending locally") {
		val manager = mockk<PlaybackManager>()
		val syncPlay = mockk<SyncPlayService>()
		val item = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.AUDIO)
		every { manager.getService(SyncPlayService::class) } returns syncPlay
		every { syncPlay.group } returns MutableStateFlow(mockk<GroupInfoDto>())
		every { syncPlay.queueItems(listOf(item.id)) } returns Unit

		RewriteMediaManager(mockk(), manager).addToAudioQueue(listOf(item))

		verify(exactly = 1) { syncPlay.queueItems(listOf(item.id)) }
		// The strict manager mock rejects all local queue and player accesses.
	}

	test("repeat toggle can disable the server mode while local repeat stays disabled") {
		val controls = groupedControls(GroupRepeatMode.REPEAT_ONE, GroupShuffleMode.SORTED)
		controls.media.isRepeatMode shouldBe true

		controls.media.toggleRepeat()

		verify { controls.state.setRepeatMode(RepeatMode.NONE) }
	}

	test("shuffle toggle can disable the server mode while local order stays default") {
		val controls = groupedControls(GroupRepeatMode.REPEAT_NONE, GroupShuffleMode.SHUFFLE)
		controls.media.isShuffleMode shouldBe true

		controls.media.shuffleAudioQueue()

		verify { controls.state.setPlaybackOrder(PlaybackOrder.DEFAULT) }
	}

	test("disabled server modes can be enabled through the existing audio controls") {
		val controls = groupedControls(GroupRepeatMode.REPEAT_NONE, GroupShuffleMode.SORTED)

		controls.media.toggleRepeat()
		controls.media.shuffleAudioQueue()

		verify { controls.state.setRepeatMode(RepeatMode.REPEAT_ENTRY_INFINITE) }
		verify { controls.state.setPlaybackOrder(PlaybackOrder.SHUFFLE) }
	}
})

private data class GroupedControls(val media: RewriteMediaManager, val state: PlayerState)

private fun groupedControls(repeatMode: GroupRepeatMode, shuffleMode: GroupShuffleMode): GroupedControls {
	val manager = mockk<PlaybackManager>()
	val state = mockk<PlayerState>(relaxed = true)
	val syncPlay = mockk<SyncPlayService>()
	val queue = mockk<PlayQueueUpdate>()
	every { manager.getService(SyncPlayService::class) } returns syncPlay
	every { manager.state } returns state
	every { state.repeatMode } returns MutableStateFlow(RepeatMode.NONE)
	every { state.playbackOrder } returns MutableStateFlow(PlaybackOrder.DEFAULT)
	every { syncPlay.group } returns MutableStateFlow(mockk<GroupInfoDto>())
	every { syncPlay.queueState } returns MutableStateFlow(queue)
	every { queue.repeatMode } returns repeatMode
	every { queue.shuffleMode } returns shuffleMode
	return GroupedControls(RewriteMediaManager(mockk(), manager), state)
}
