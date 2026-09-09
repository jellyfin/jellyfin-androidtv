@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.jellyfin.playback.core.queue

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.PlaybackManagerOptions
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import kotlin.time.Duration.Companion.seconds

class QueueSynchronizationTests : FunSpec({
	test("an old supplier completing after a group playlist replacement cannot repopulate the queue") {
		runTest {
			val queue = createQueue()
			val oldEntry = QueueEntry()
			val newEntry = QueueEntry()
			val pending = CompletableDeferred<QueueEntry>()
			val oldSelection = async {
				queue.synchronize(object : QueueSupplier {
					override val size = 1
					override suspend fun getItem(index: Int): QueueEntry? = if (index == 0) pending.await() else null
				}, 0)
			}
			runCurrent()
			val newSelection = async { queue.synchronize(supplier(newEntry), 0) }
			runCurrent()
			newSelection.isCompleted shouldBe true
			newSelection.await() shouldBe newEntry
			queue.entry.value shouldBe newEntry
			pending.complete(oldEntry)
			oldSelection.await() shouldBe null
			newSelection.await() shouldBe newEntry
			queue.entry.value shouldBe newEntry
			queue.entries.value.shouldContainExactly(newEntry)
		}
	}

	test("clearing while a supplier is loading does not resurrect a stopped group video") {
		runTest {
			val queue = createQueue()
			val pending = CompletableDeferred<QueueEntry>()
			val selection = async {
				queue.synchronize(object : QueueSupplier {
					override val size = 1
					override suspend fun getItem(index: Int): QueueEntry? = if (index == 0) pending.await() else null
				}, 0)
			}
			runCurrent()
			queue.clear()
			pending.complete(QueueEntry())
			selection.await() shouldBe null
			queue.entry.value shouldBe null
			queue.entryIndex.value shouldBe Queue.INDEX_NONE
			queue.entries.value shouldBe emptyList()
		}
	}

	test("metadata-only synchronization keeps the selected entry and updates its index") {
		runTest {
			val queue = createQueue()
			val current = QueueEntry()
			queue.synchronize(supplier(current), 0)
			val previous = QueueEntry()
			queue.synchronize(supplier(previous, current), 1)
			queue.entry.value shouldBe current
			queue.entryIndex.value shouldBe 1
			queue.entries.value.shouldContainExactly(previous, current)
			queue.synchronize(supplier(), Queue.INDEX_NONE)
			queue.entries.value shouldBe emptyList()
			queue.entry.value shouldBe null
		}
	}
})

private fun kotlinx.coroutines.test.TestScope.createQueue(): QueueService {
	val queue = QueueService()
	PlaybackManager(
		backend = mockk(relaxed = true),
		services = mutableListOf(queue),
		options = PlaybackManagerOptions(mockk(relaxed = true), { 10.seconds }, { 10.seconds }),
		parentJob = backgroundScope.coroutineContext[kotlinx.coroutines.Job],
	)
	return queue
}

private fun supplier(vararg entries: QueueEntry) = object : QueueSupplier {
	override val size = entries.size
	override suspend fun getItem(index: Int) = entries.getOrNull(index)
}
