package org.jellyfin.androidtv.util.sdk

import java.util.UUID

internal object MusicVideoQueue {
	suspend fun <T> all(pageSize: Int, page: suspend (offset: Int, limit: Int) -> List<T>): List<T> = buildList {
		require(pageSize > 0)
		while (true) {
			val next = page(size, pageSize)
			addAll(next)
			if (next.size < pageSize) break
		}
	}

	fun <T> startAt(items: List<T>, selectedId: UUID, id: (T) -> UUID): List<T> {
		val index = items.indexOfFirst { id(it) == selectedId }
		return if (index < 0) items else items.drop(index) + items.take(index)
	}
}
