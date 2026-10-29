package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import java.util.UUID

internal object MusicVideoQueue {
	suspend fun libraryFor(item: BaseItemDto, ancestors: suspend () -> List<BaseItemDto>): BaseItemDto? {
		if (item.collectionType == CollectionType.MUSICVIDEOS) return item
		if (item.type != BaseItemKind.MUSIC_VIDEO && item.type != BaseItemKind.VIDEO) return null
		return ancestors().firstOrNull { it.collectionType == CollectionType.MUSICVIDEOS }
	}

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
