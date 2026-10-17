package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.UUID

class MusicVideoQueueTests : FunSpec({
	test("collects every page of a large library") {
		val videos = (0 until 501).toList()
		val offsets = mutableListOf<Int>()
		val result = runBlocking {
			MusicVideoQueue.all(150) { offset, limit ->
				offsets += offset
				videos.drop(offset).take(limit)
			}
		}
		result shouldBe videos
		offsets shouldBe listOf(0, 150, 300, 450)
	}

	test("exact page boundary and empty library terminate") {
		val videos = (0 until 300).toList()
		val offsets = mutableListOf<Int>()
		runBlocking {
			MusicVideoQueue.all(150) { offset, limit ->
				offsets += offset
				videos.drop(offset).take(limit)
			}
		} shouldBe videos
		offsets shouldBe listOf(0, 150, 300)
		runBlocking { MusicVideoQueue.all(150) { _, _ -> emptyList<Int>() } } shouldBe emptyList()
	}

	test("page errors propagate without playing a partial queue") {
		shouldThrow<IOException> {
			runBlocking {
				MusicVideoQueue.all(150) { offset, _ ->
					if (offset == 0) (0 until 150).toList() else throw IOException("server error")
				}
			}
		}
	}

	test("selected video starts first and remaining videos keep their order") {
		val ids = (0 until 4).map { UUID.nameUUIDFromBytes(it.toString().toByteArray()) }
		MusicVideoQueue.startAt(ids, ids[2]) { it } shouldBe listOf(ids[2], ids[3], ids[0], ids[1])
		MusicVideoQueue.startAt(ids, UUID.randomUUID()) { it } shouldBe ids
	}
})
