package org.jellyfin.androidtv.util.sdk

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.instantMixApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.extensions.ticks
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SdkPlaybackHelper(
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
	private val playbackLauncher: PlaybackLauncher,
	private val playbackControllerContainer: PlaybackControllerContainer,
) : PlaybackHelper {
	companion object {
		const val ITEM_QUERY_LIMIT = 150
	}

	override fun getItemsToPlay(
		context: Context,
		mainItem: BaseItemDto,
		allowIntros: Boolean,
		shuffle: Boolean,
		outerResponse: Response<List<BaseItemDto>>
	) {
		getScope(context).launch {
			runCatching {
				val items = getItems(mainItem, allowIntros, shuffle)
				if (items.isEmpty() && !mainItem.mediaSources.isNullOrEmpty()) listOf(mainItem)
				else items
			}.fold(
				onSuccess = { items -> outerResponse.onResponse(items) },
				onFailure = { exception ->
					when (exception) {
						is Exception -> outerResponse.onError(exception)
						else -> outerResponse.onError(Exception(exception))
					}
				}
			)
		}
	}

	private suspend fun getItems(
		mainItem: BaseItemDto,
		allowIntros: Boolean,
		shuffle: Boolean,
	): List<BaseItemDto> = withContext(Dispatchers.IO) {
		when (mainItem.type) {
			BaseItemKind.EPISODE -> {
				val seriesId = mainItem.seriesId
				if (userPreferences[UserPreferences.mediaQueuingEnabled] && seriesId != null) {
					val response by api.tvShowsApi.getEpisodes(
						seriesId = seriesId,
						startItemId = mainItem.id,
						isMissing = false,
						limit = ITEM_QUERY_LIMIT,
						fields = ItemRepository.itemFields
					)

					response.items
				} else {
					listOf(mainItem)
				}
			}

			BaseItemKind.SERIES -> {
				val response by api.tvShowsApi.getEpisodes(
					seriesId = mainItem.id,
					isMissing = false,
					sortBy = if (shuffle) ItemSortBy.RANDOM else ItemSortBy.SORT_NAME,
					limit = ITEM_QUERY_LIMIT,
					fields = ItemRepository.itemFields,
				)
				response.items
			}

			BaseItemKind.SEASON -> {
				val response by api.tvShowsApi.getEpisodes(
					seriesId = requireNotNull(mainItem.seriesId),
					seasonId = mainItem.id,
					isMissing = false,
					sortBy = if (shuffle) ItemSortBy.RANDOM else ItemSortBy.SORT_NAME,
					limit = ITEM_QUERY_LIMIT,
					fields = ItemRepository.itemFields,
				)
				response.items
			}

			BaseItemKind.FOLDER -> {
				val response by api.itemsApi.getItems(
					parentId = mainItem.id,
					isMissing = false,
					includeItemTypes = listOf(
						BaseItemKind.EPISODE,
						BaseItemKind.MOVIE,
						BaseItemKind.VIDEO
					),
					sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else listOf(ItemSortBy.SORT_NAME),
					recursive = true,
					limit = ITEM_QUERY_LIMIT,
					fields = ItemRepository.itemFields
				)

				response.items
			}

			BaseItemKind.BOX_SET -> {
				val response by api.itemsApi.getItems(
					parentId = mainItem.id,
					isMissing = false,
					includeItemTypes = listOf(
						BaseItemKind.EPISODE,
						BaseItemKind.MOVIE,
						BaseItemKind.VIDEO
					),
					sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else null,
					recursive = true,
					limit = ITEM_QUERY_LIMIT,
					fields = ItemRepository.itemFields
				)

				response.items
			}

			BaseItemKind.MUSIC_ALBUM -> {
				val response by api.itemsApi.getItems(
					isMissing = false,
					mediaTypes = listOf(MediaType.AUDIO),
					filters = listOf(ItemFilter.IS_NOT_FOLDER),
					sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else listOf(
						ItemSortBy.ALBUM,
						ItemSortBy.PARENT_INDEX_NUMBER,
						ItemSortBy.INDEX_NUMBER,
						ItemSortBy.SORT_NAME
					),
					recursive = true,
					limit = ITEM_QUERY_LIMIT,
					fields = ItemRepository.itemFields,
					albumIds = listOf(mainItem.id)
				)

				response.items
			}

			BaseItemKind.MUSIC_ARTIST -> {
				val response by api.itemsApi.getItems(
					isMissing = false,
					mediaTypes = listOf(MediaType.AUDIO),
					filters = listOf(ItemFilter.IS_NOT_FOLDER),
					sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else listOf(
						ItemSortBy.ALBUM,
						ItemSortBy.PARENT_INDEX_NUMBER,
						ItemSortBy.INDEX_NUMBER,
						ItemSortBy.SORT_NAME
					),
					recursive = true,
					limit = ITEM_QUERY_LIMIT,
					fields = ItemRepository.itemFields,
					artistIds = listOf(mainItem.id)
				)

				response.items
			}

			BaseItemKind.PLAYLIST -> {
				val response by api.itemsApi.getItems(
					parentId = mainItem.id,
					isMissing = false,
					sortBy = if (shuffle) listOf(ItemSortBy.RANDOM) else null,
					recursive = true,
					limit = ITEM_QUERY_LIMIT,
					fields = ItemRepository.itemFields
				)

				response.items
			}

			BaseItemKind.PROGRAM -> {
				val parentId = requireNotNull(mainItem.parentId)
				val channel by api.userLibraryApi.getItem(parentId)
				val channelWithProgramMetadata = channel.copy(
					premiereDate = mainItem.premiereDate,
					endDate = mainItem.endDate,
					officialRating = mainItem.officialRating,
					runTimeTicks = mainItem.runTimeTicks,
				)

				listOf(channelWithProgramMetadata)
			}

			BaseItemKind.TV_CHANNEL -> {
				val channel by api.liveTvApi.getChannel(mainItem.id)
				val currentProgram = channel.currentProgram
				if (currentProgram != null) {
					val channelWithCurrentProgramMetadata = channel.copy(
						premiereDate = currentProgram.premiereDate,
						endDate = currentProgram.endDate,
						officialRating = currentProgram.officialRating,
						runTimeTicks = currentProgram.runTimeTicks,
					)
					listOf(channelWithCurrentProgramMetadata)
				} else {
					listOf(channel)
				}
			}

			else -> {
				val parts = getParts(mainItem)
				val addIntros = allowIntros && userPreferences[UserPreferences.cinemaModeEnabled]

				if (addIntros) {
					val intros = runCatching { api.userLibraryApi.getIntros(mainItem.id).content.items }.getOrNull()
						.orEmpty()
						// Force the type to be trailer as the legacy playback UI uses it to determine if it should show the next up screen
						.map { it.copy(type = BaseItemKind.TRAILER) }

					intros + parts
				} else {
					parts
				}
			}
		}
	}

	private suspend fun getParts(item: BaseItemDto): List<BaseItemDto> = buildList {
		add(item)

		val partCount = item.partCount
		if (partCount != null && partCount > 1) {
			val response by api.videosApi.getAdditionalPart(item.id)
			addAll(response.items)
		}
	}

	override fun retrieveAndPlay(id: UUID, shuffle: Boolean, position: Long?, context: Context) {
		getScope(context).launch {
			val resumeSubtractDuration =
				userPreferences[UserPreferences.resumeSubtractDuration].toIntOrNull()?.seconds
					?: Duration.ZERO

			val item = withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(id).content
			}
			val pos = position?.ticks ?: item.userData?.playbackPositionTicks?.ticks?.minus(
				resumeSubtractDuration
			) ?: Duration.ZERO
			val allowIntros = pos == Duration.ZERO && item.type == BaseItemKind.MOVIE

			val items = getItems(item, allowIntros, shuffle)

			playbackLauncher.launch(
				context,
				items,
				pos.inWholeMilliseconds.toInt(),
				playbackControllerContainer.playbackController?.hasFragment() == true,
				0,
				shuffle,
			)
		}
	}

	override fun playInstantMix(context: Context, item: BaseItemDto) {
		getScope(context).launch {
			val response = withContext(Dispatchers.IO) {
				api.instantMixApi.getInstantMixFromItem(
					itemId = item.id,
					fields = ItemRepository.itemFields
				).content
			}

			val items = response.items
			if (items.isNotEmpty()) {
				playbackLauncher.launch(context, items)
			} else {
				Toast.makeText(context, R.string.msg_no_playable_items, Toast.LENGTH_LONG).show()
			}
		}
	}

	private fun getScope(context: Context? = null) = when (context) {
		is LifecycleOwner -> context.lifecycleScope
		else -> ProcessLifecycleOwner.get().lifecycleScope
	}
}
