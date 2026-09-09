@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.jellyfin.playback.core.mediastream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.QueueService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MediaStreamReplacementTests : FunSpec({
	test("replacing a loading item cancels its resolver and starts the authoritative item immediately") {
		runTest(timeout = 10.seconds) {
			val dispatcher = StandardTestDispatcher(testScheduler)
			Dispatchers.setMain(dispatcher)
			val scope = CoroutineScope(SupervisorJob() + dispatcher)
			val oldEntry = QueueEntry()
			val newEntry = QueueEntry()
			val selected = MutableStateFlow<QueueEntry?>(null)
			val oldStarted = CompletableDeferred<Unit>()
			val oldCancelled = CompletableDeferred<Unit>()
			val newPlaying = CompletableDeferred<Unit>()
			val fallbackAttempts = mutableListOf<QueueEntry>()
			val resolver = object : MediaStreamResolver {
				override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream {
					if (queueEntry === oldEntry) {
						oldStarted.complete(Unit)
						try {
							awaitCancellation()
						} finally {
							oldCancelled.complete(Unit)
						}
					}
					return PlayableMediaStream(
						"new", MediaConversionMethod.None, MediaStreamContainer("mp4"), emptyList(), queueEntry, "https://example.test/video"
					)
				}
			}
			val fallback = object : MediaStreamResolver {
				override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream? {
					fallbackAttempts.add(queueEntry)
					return null
				}
			}
			val backend = mockk<PlayerBackend>(relaxed = true)
			val manager = mockk<PlaybackManager>()
			val queue = mockk<QueueService>()
			val service = MediaStreamService(listOf(resolver, fallback), Duration.ZERO)
			mockkObject(service)
			every { service.manager } returns manager
			every { service.coroutineScope } returns scope
			every { manager.backend } returns backend
			every { manager.getService(QueueService::class) } returns queue
			every { queue.entry } returns selected
			every { backend.playItem(newEntry) } answers {
				newPlaying.complete(Unit)
				Unit
			}

			try {
				service.onInitialize()
				runCurrent()
				selected.value = oldEntry
				oldStarted.await()
				selected.value = newEntry
				newPlaying.await()
				oldCancelled.await()
				fallbackAttempts shouldBe emptyList()
				verify(exactly = 0) { backend.playItem(oldEntry) }
				verify(exactly = 1) { backend.playItem(newEntry) }
			} finally {
				scope.cancel()
				runCurrent()
				unmockkObject(service)
				Dispatchers.resetMain()
			}
		}
	}
})
