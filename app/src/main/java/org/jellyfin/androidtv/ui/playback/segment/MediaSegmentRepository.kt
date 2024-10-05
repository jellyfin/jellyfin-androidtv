package org.jellyfin.androidtv.ui.playback.segment

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaSegmentsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaSegmentType

interface MediaSegmentRepository {
	companion object {
		/**
		 * All media segments currently supported by the app. The order of these is used for the preferences UI.
		 */
		val SupportedTypes = listOf(
			MediaSegmentType.INTRO,
			MediaSegmentType.OUTRO,
			MediaSegmentType.PREVIEW,
			MediaSegmentType.RECAP,
			MediaSegmentType.COMMERCIAL,
		)
	}

	fun getDefaultSegmentTypeAction(type: MediaSegmentType): MediaSegmentAction
	fun setDefaultSegmentTypeAction(type: MediaSegmentType, action: MediaSegmentAction)

	suspend fun getSegmentsForItem(item: BaseItemDto): List<MediaSegmentDto>
	fun getMediaSegmentAction(segment: MediaSegmentDto): MediaSegmentAction
}

class MediaSegmentRepositoryImpl(
	private val userPreferences: UserPreferences,
	private val api: ApiClient,
) : MediaSegmentRepository {
	private val mediaTypeActions = mutableMapOf<MediaSegmentType, MediaSegmentAction>()

	init {
		restoreMediaTypeActions()
	}

	private fun restoreMediaTypeActions() {
		val restoredMediaTypeActions = userPreferences[UserPreferences.mediaSegmentActions]
			.split(",")
			.mapNotNull {
				runCatching {
					val (type, action) = it.split('=', limit = 2)
					MediaSegmentType.fromName(type) to MediaSegmentAction.valueOf(action)
				}.getOrNull()
			}

		mediaTypeActions.clear()
		mediaTypeActions.putAll(restoredMediaTypeActions)
	}

	private fun saveMediaTypeActions() {
		userPreferences[UserPreferences.mediaSegmentActions] = mediaTypeActions
			.map { "${it.key.serialName}=${it.value.name}" }
			.joinToString(",")
	}

	override fun getDefaultSegmentTypeAction(type: MediaSegmentType): MediaSegmentAction {
		// Always return no action for unsupported types
		if (!MediaSegmentRepository.SupportedTypes.contains(type)) return MediaSegmentAction.NOTHING

		return mediaTypeActions.getOrDefault(type, MediaSegmentAction.NOTHING)
	}

	override fun setDefaultSegmentTypeAction(type: MediaSegmentType, action: MediaSegmentAction) {
		// Don't allow modifying actions for unsupported types
		if (!MediaSegmentRepository.SupportedTypes.contains(type)) return

		mediaTypeActions[type] = action
		saveMediaTypeActions()
	}

	override fun getMediaSegmentAction(segment: MediaSegmentDto): MediaSegmentAction {
		return getDefaultSegmentTypeAction(segment.type)
	}

	override suspend fun getSegmentsForItem(item: BaseItemDto): List<MediaSegmentDto> = runCatching {
		api.mediaSegmentsApi.getItemSegments(
			itemId = item.id,
			includeSegmentTypes = MediaSegmentRepository.SupportedTypes,
		).content.items
	}.getOrDefault(emptyList())
}
