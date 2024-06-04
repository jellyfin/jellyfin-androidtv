package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.LiveTvOption
import org.jellyfin.androidtv.data.querying.AdditionalPartsQuery
import org.jellyfin.androidtv.data.querying.SpecialsQuery
import org.jellyfin.androidtv.data.querying.TrailersQuery
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import org.jellyfin.sdk.model.api.request.GetSeasonsRequest
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import org.jellyfin.sdk.model.api.request.GetUpcomingEpisodesRequest
import timber.log.Timber
import kotlin.math.min

fun <T : Any> ItemRowAdapter.setItems(
	items: Array<T>,
	transform: (T, Int) -> BaseRowItem?,
) {
	Timber.d("Creating items from $itemsLoaded existing and ${items.size} new, adapter size is ${size()}")

	val allItems = buildList {
		// Add current items before loaded items
		repeat(itemsLoaded) {
			add(this@setItems.get(it))
		}

		// Add loaded items
		val mappedItems = items.mapIndexedNotNull { index, item ->
			transform(item, itemsLoaded + index)
		}
		mappedItems.forEach { add(it) }

		// Add current items after loaded items
		repeat(min(totalItems, size()) - itemsLoaded - mappedItems.size) {
			add(this@setItems.get(it + itemsLoaded + mappedItems.size))
		}
	}

	replaceAll(allItems)
	itemsLoaded = allItems.size
}

fun ItemRowAdapter.retrieveResumeItems(api: ApiClient, query: GetResumeItemsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.itemsApi.getResumeItems(query)

			setItems(
				items = response.items.orEmpty().toTypedArray(),
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						preferParentThumb,
						isStaticHeight
					)
				}
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveNextUpItems(api: ApiClient, query: GetNextUpRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.tvShowsApi.getNextUp(query)

			// Some special flavor for series, used in FullDetailsFragment
			val firstNextUp = response.items?.firstOrNull()
			if (query.seriesId != null && response.items?.size == 1 && firstNextUp?.seasonId != null && firstNextUp.indexNumber != null) {
				// If we have exactly 1 episode returned, the series is currently partially watched
				// we want to query the server for all episodes in the same season starting from
				// this one to create a list of all unwatched episodes
				val episodesResponse by api.itemsApi.getItems(
					parentId = firstNextUp.seasonId,
					startIndex = firstNextUp.indexNumber,
				)

				// Combine the next up episode with the additionally retrieved episodes
				val items = buildList {
					add(firstNextUp)
					episodesResponse.items?.let { addAll(it) }
				}.toTypedArray()

				setItems(
					items = items,
					transform = { item, _ ->
						BaseItemDtoBaseRowItem(
							item,
							preferParentThumb,
							false
						)
					}
				)

				if (items.isEmpty()) removeRow()
			} else {
				setItems(
					items = response.items.orEmpty().toTypedArray(),
					transform = { item, _ ->
						BaseItemDtoBaseRowItem(
							item,
							preferParentThumb,
							isStaticHeight
						)
					}
				)

				if (response.items.isNullOrEmpty()) removeRow()
			}
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLatestMedia(api: ApiClient, query: GetLatestMediaRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.userLibraryApi.getLatestMedia(query)

			setItems(
				items = response.toTypedArray(),
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						preferParentThumb,
						isStaticHeight,
						BaseRowItemSelectAction.ShowDetails,
						preferParentThumb,
					)
				}
			)

			if (response.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveSpecialFeatures(api: ApiClient, query: SpecialsQuery) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.userLibraryApi.getSpecialFeatures(query.itemId)

			setItems(
				items = response.toTypedArray(),
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(item, preferParentThumb, false)
				}
			)

			if (response.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveAdditionalParts(api: ApiClient, query: AdditionalPartsQuery) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.videosApi.getAdditionalPart(query.itemId)

			setItems(
				items = response.items.orEmpty().toTypedArray(),
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveUserViews(api: ApiClient, userViewsRepository: UserViewsRepository) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.userViewsApi.getUserViews()

			val filteredItems = response.items.orEmpty()
				.filter { userViewsRepository.isSupported(it.collectionType) }
				.map { it.copy(displayPreferencesId = it.id.toString()) }

			setItems(
				items = filteredItems.toTypedArray(),
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (filteredItems.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveSeasons(api: ApiClient, query: GetSeasonsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.tvShowsApi.getSeasons(query)

			setItems(
				items = response.items.orEmpty().toTypedArray(),
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveUpcomingEpisodes(api: ApiClient, query: GetUpcomingEpisodesRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.tvShowsApi.getUpcomingEpisodes(query)

			setItems(
				items = response.items.orEmpty().toTypedArray(),
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveSimilarItems(api: ApiClient, query: GetSimilarItemsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.libraryApi.getSimilarItems(query)

			setItems(
				items = response.items.orEmpty().toTypedArray(),
				transform = { item, _ -> BaseItemDtoBaseRowItem(item) }
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveTrailers(api: ApiClient, query: TrailersQuery) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.userLibraryApi.getLocalTrailers(itemId = query.itemId)

			setItems(
				items = response.toTypedArray(),
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						preferParentThumb,
						false,
						BaseRowItemSelectAction.Play,
						false
					)
				}
			)

			if (response.isEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLiveTvRecommendedPrograms(
	api: ApiClient,
	query: GetRecommendedProgramsRequest
) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.liveTvApi.getRecommendedPrograms(query)

			setItems(
				items = response.items.orEmpty().toTypedArray(),
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						false,
						isStaticHeight,
					)
				}
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLiveTvRecordings(api: ApiClient, query: GetRecordingsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.liveTvApi.getRecordings(query)

			setItems(
				items = response.items.orEmpty().toTypedArray(),
				transform = { item, _ ->
					BaseItemDtoBaseRowItem(
						item,
						false,
						isStaticHeight,
					)
				}
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

fun ItemRowAdapter.retrieveLiveTvSeriesTimers(
	api: ApiClient,
	context: Context,
	canManageRecordings: Boolean
) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			val response by api.liveTvApi.getSeriesTimers()

			setItems(
				items = buildList {
					add(
						GridButton(
							LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID,
							context.getString(R.string.lbl_recorded_tv)
						)
					)

					if (canManageRecordings) {
						add(
							GridButton(
								LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID,
								context.getString(R.string.lbl_schedule)
							)
						)

						add(
							GridButton(
								LiveTvOption.LIVE_TV_SERIES_OPTION_ID,
								context.getString(R.string.lbl_series)
							)
						)
					}

					addAll(response.items.orEmpty())
				}.toTypedArray(),
				transform = { item, _ ->
					when (item) {
						is GridButton -> GridButtonBaseRowItem(item)
						is SeriesTimerInfoDto -> SeriesTimerInfoDtoBaseRowItem(item)
						else -> error("Unknown type for item")
					}
				}
			)

			if (response.items.isNullOrEmpty()) removeRow()
		}.fold(
			onSuccess = { notifyRetrieveFinished() },
			onFailure = { error -> notifyRetrieveFinished(error as? Exception) }
		)
	}
}

@JvmOverloads
fun ItemRowAdapter.refreshItem(
	api: ApiClient,
	lifecycleOwner: LifecycleOwner,
	currentBaseRowItem: BaseRowItem,
	callback: () -> Unit = {}
) {
	if (currentBaseRowItem !is BaseItemDtoBaseRowItem) return
	val currentBaseItem = currentBaseRowItem.baseItem ?: return

	lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
		runCatching {
			api.userLibraryApi.getItem(itemId = currentBaseItem.id).content
		}.fold(
			onSuccess = { refreshedBaseItem ->
				withContext(Dispatchers.Main) {
					set(
						index = indexOf(currentBaseRowItem),
						element = BaseItemDtoBaseRowItem(
							item = refreshedBaseItem,
							preferParentThumb = currentBaseRowItem.preferParentThumb,
							staticHeight = currentBaseRowItem.staticHeight,
							selectAction = currentBaseRowItem.selectAction,
							preferSeriesPoster = currentBaseRowItem.preferSeriesPoster
						)
					)
				}
			},
			onFailure = { err ->
				if (err is InvalidStatusException && err.status == 404) withContext(Dispatchers.Main) {
					remove(currentBaseRowItem)
				} else Timber.e(err, "Failed to refresh item")
			}
		)

		callback()
	}
}
