package org.jellyfin.playback.jellyfin.syncplay

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.SendCommandType

class SyncPlaySessionTests : FunSpec({
	test("M03 only the active join can establish membership") {
		val session = SyncPlaySession()
		val old = session.beginJoin(groupId)
		val current = session.beginJoin(otherGroupId)
		session.joined(old, group()) shouldBe false
		session.joined(current, group()) shouldBe false
		session.joined(current, group(otherGroupId)) shouldBe true
		session.state.value.group?.groupId shouldBe otherGroupId
	}

	test("M03 duplicate acknowledgment preserves queue and generation") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.updateQueue(generation, groupId, queue())
		session.joined(generation, group()) shouldBe true
		session.state.value.queue shouldBe queue()
	}

	test("M07 leave invalidates in-flight work even when rejoining the same group") {
		val session = SyncPlaySession()
		val old = session.beginJoin(groupId)
		session.joined(old, group())
		session.reset()
		val current = session.beginJoin(groupId)
		session.joined(current, group())
		session.updateQueue(old, groupId, queue()) shouldBe false
		session.acceptCommand(old, command()) shouldBe false
		session.state.value.queue shouldBe null
	}

	test("Q02 preserves repeated media as different occurrences") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.updateQueue(generation, groupId, queue(index = 1)) shouldBe true
		session.state.value.queue?.playlist?.size shouldBe 2
		session.state.value.currentItem?.playlistItemId shouldBe queue().playlist[1].playlistItemId
	}

	test("Q05 rejects stale equal and wrong-group snapshots") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.updateQueue(generation, groupId, queue(1300)) shouldBe true
		session.updateQueue(generation, groupId, queue(1200)) shouldBe false
		session.updateQueue(generation, groupId, queue(1300)) shouldBe false
		session.updateQueue(generation, otherGroupId, queue(1400)) shouldBe false
	}

	test("Q06 rejects invalid index and duplicated occurrence identity") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.updateQueue(generation, groupId, queue(index = 9)) shouldBe false
		session.updateQueue(generation, groupId, queue().copy(playlist = List(2) { queue().playlist[0] })) shouldBe false
		session.state.value.queue shouldBe null
	}

	test("Q04 accepts an empty queue without a playing item") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.updateQueue(generation, groupId, queue(index = -1).copy(playlist = emptyList())) shouldBe true
		session.state.value.currentItem shouldBe null
	}

	test("T10 rejects commands before join and older than accepted command") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.acceptCommand(generation, command()) shouldBe false
		session.joined(generation, group())
		session.acceptCommand(generation, command(emitted = 999)) shouldBe false
		session.acceptCommand(generation, command(emitted = 1400)) shouldBe true
		session.acceptCommand(generation, command(emitted = 1300)) shouldBe false
		session.acceptCommand(generation, command(group = otherGroupId, emitted = 1500)) shouldBe false
	}

	test("T05 retains command before queue arrives but only exposes matching occurrence") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.acceptCommand(generation, command()) shouldBe true
		session.state.value.playbackCommand shouldBe null
		session.updateQueue(generation, groupId, queue())
		session.state.value.playbackCommand shouldBe command()
		session.updateQueue(generation, groupId, queue(1500, 1))
		session.state.value.playbackCommand shouldBe null
	}

	test("T10 equal timestamps retain arrival order and Stop bypasses occurrence check") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.acceptCommand(generation, command())
		val stop = command(SendCommandType.STOP, position = null)
		session.acceptCommand(generation, stop) shouldBe true
		session.state.value.playbackCommand shouldBe stop
	}

	test("T12 missing and negative positions are rejected except nullable Stop") {
		val session = SyncPlaySession()
		val generation = session.beginJoin(groupId)
		session.joined(generation, group())
		session.acceptCommand(generation, command(position = null)) shouldBe false
		session.acceptCommand(generation, command(position = -1)) shouldBe false
		session.acceptCommand(generation, command(position = 0)) shouldBe true
	}
})
