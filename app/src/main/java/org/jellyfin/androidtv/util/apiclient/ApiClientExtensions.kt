package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.*
import org.jellyfin.androidtv.querying.StdItemQuery
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.UserItemDataDto
import org.jellyfin.apiclient.model.querying.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Coroutine capable version of the "getNextUpEpisodes" function
 */
suspend fun ApiClient.getNextUpEpisodes(query: NextUpQuery): ItemsResult? = suspendCoroutine { continuation ->
	GetNextUpEpisodesAsync(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) = continuation.resume(response!!)
		override fun onError(exception: Exception?) = continuation.resume(null)
	})
}

/**
 * Coroutine capable version of the "getUserViews" function
 * Uses the userId of the currently signed in user
 */
suspend fun ApiClient.getUserViews(): ItemsResult? = suspendCoroutine { continuation ->
	GetUserViews(currentUserId, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) = continuation.resume(response!!)
		override fun onError(exception: Exception?) = continuation.resume(null)
	})
}

/**
 * Adds a coroutine capable version of the "GetItem" function
 * Uses the userId of the currently signed in user
 */
suspend fun ApiClient.getItem(id: String): BaseItemDto? = suspendCoroutine { continuation ->
	GetItemAsync(id, TvApp.getApplication().currentUser.id, object : Response<BaseItemDto>() {
		override fun onResponse(response: BaseItemDto?) = continuation.resume(response!!)
		override fun onError(exception: Exception?) = continuation.resume(null)
	})
}

suspend fun ApiClient.markPlayed(itemId: String, userId: String, datePlayed: Date?): UserItemDataDto? = suspendCoroutine { continuation ->
	MarkPlayedAsync(itemId, userId, datePlayed, object : Response<UserItemDataDto>() {
		override fun onResponse(response: UserItemDataDto?) {
			continuation.resume(response!!)
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.markUnplayed(itemId: String, userId: String): UserItemDataDto? = suspendCoroutine { continuation ->
	MarkUnplayedAsync(itemId, userId, object : Response<UserItemDataDto>() {
		override fun onResponse(response: UserItemDataDto?) {
			continuation.resume(response!!)
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.updateFavoriteStatus(itemId: String, userId: String, isFavorite: Boolean): UserItemDataDto? = suspendCoroutine { continuation ->
	UpdateFavoriteStatusAsync(itemId, userId, isFavorite, object : Response<UserItemDataDto>() {
		override fun onResponse(response: UserItemDataDto?) {
			continuation.resume(response!!)
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.getSimilarItems(item: BaseItem, limit: Int = 25): List<BaseItem>? = suspendCoroutine { continuation ->
	val query = SimilarItemsQuery().apply {
		id = item.id
		userId = TvApp.getApplication().currentUser.id
		this.limit = limit
		fields = FIELDS_REQUIRED_FOR_LIFT
	}

	GetSimilarItems(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) {
			continuation.resume(response?.items.orEmpty().map { baseItemDto -> baseItemDto.liftToNewFormat() })
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.getSpecialFeatures(item: BaseItem): List<BaseItem>? = suspendCoroutine { continuation ->
	GetSpecialFeaturesAsync(TvApp.getApplication().currentUser.id, item.id, object : Response<Array<BaseItemDto>>() {
		override fun onResponse(response: Array<BaseItemDto>?) {
			continuation.resume(response!!.map { baseItemDto -> baseItemDto.liftToNewFormat() })
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.getLocalTrailers(item: BaseItem): List<LocalTrailer>? = suspendCoroutine { continuation ->
	GetLocalTrailersAsync(TvApp.getApplication().currentUser.id, item.id, object : Response<Array<BaseItemDto>>() {
		override fun onResponse(response: Array<BaseItemDto>?) {
			continuation.resume(response!!.map { baseItemDto -> baseItemDto.liftToNewFormat() as LocalTrailer })
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.getSisterEpisodes(episode: Episode): List<Episode>? = episode.seasonId?.let { getEpisodesOfSeason(episode.seasonId) }
suspend fun ApiClient.getEpisodesOfSeason(season: Season): List<Episode>? = getEpisodesOfSeason(season.id)

private suspend fun ApiClient.getEpisodesOfSeason(seasonId: String): List<Episode>? = suspendCoroutine { continuation ->
	val query = StdItemQuery()
	query.parentId = seasonId
	query.includeItemTypes = arrayOf(BaseItemType.Episode.name)
	query.startIndex = 0
	query.fields = FIELDS_REQUIRED_FOR_LIFT
	query.userId = this.currentUserId

	GetItemsAsync(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) {
			continuation.resume(response?.items?.map { it.liftToNewFormat() as Episode }?.toList())
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.getAlbumsForArtist(artist: Artist): List<Album>? = getAlbumsForArtists(arrayOf(artist.id))

suspend fun ApiClient.getAlbumsForArtists(artists: Array<String>): List<Album>? = suspendCoroutine { continuation ->
	val query = ItemQuery().apply {
		fields = FIELDS_REQUIRED_FOR_LIFT
		userId = currentUserId
		artistIds = artists
		recursive = true
		includeItemTypes = arrayOf(BaseItemType.MusicAlbum.name)
	}

	GetItemsAsync(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) {
			continuation.resume(response?.items?.map { it.liftToNewFormat() as Album }?.toList())
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.getSeasonsForSeries(series: Series): List<Season>? = suspendCoroutine { continuation ->
	val query = SeasonQuery().apply {
		fields = FIELDS_REQUIRED_FOR_LIFT
		userId = currentUserId
		seriesId = series.id
	}

	GetSeasonsAsync(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) {
			continuation.resume(response?.items?.map { it.liftToNewFormat() as Season }?.toList())
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}

suspend fun ApiClient.getSongsForAlbum(albumId: String): List<Audio>? = suspendCoroutine { continuation ->
	val query = ItemQuery().apply {
		fields = FIELDS_REQUIRED_FOR_LIFT
		userId = currentUserId
		parentId = albumId
		recursive = true
		includeItemTypes = arrayOf(BaseItemType.Audio.name)
		sortBy = arrayOf(ItemFields.SortName.name)
	}

	GetItemsAsync(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) {
			continuation.resume(response?.items?.map { it.liftToNewFormat() as Audio }?.toList())
		}

		override fun onError(exception: Exception?) {
			continuation.resume(null)
		}
	})
}
