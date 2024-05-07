package org.jellyfin.androidtv.ui.playback

import android.content.Context
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.navigation.fragmentDestination
import org.jellyfin.androidtv.ui.playback.rewrite.PlaybackRewriteFragment
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.constant.MediaType

interface PlaybackLauncher {
	fun useExternalPlayer(itemType: BaseItemKind?): Boolean
	fun getPlaybackDestination(itemType: BaseItemKind?, position: Int): Destination
	fun interceptPlayRequest(context: Context, item: BaseItemDto?): Boolean
}

class GarbagePlaybackLauncher(
	private val userPreferences: UserPreferences
) : PlaybackLauncher {
	override fun useExternalPlayer(itemType: BaseItemKind?) = when (itemType) {
		BaseItemKind.MOVIE,
		BaseItemKind.EPISODE,
		BaseItemKind.VIDEO,
		BaseItemKind.SERIES,
		BaseItemKind.SEASON,
		BaseItemKind.RECORDING,
		-> userPreferences[UserPreferences.videoPlayer] === PreferredVideoPlayer.EXTERNAL
		BaseItemKind.TV_CHANNEL,
		BaseItemKind.PROGRAM,
		-> userPreferences[UserPreferences.liveTvVideoPlayer] === PreferredVideoPlayer.EXTERNAL
		else -> false
	}

	override fun getPlaybackDestination(itemType: BaseItemKind?, position: Int) = when {
		useExternalPlayer(itemType) -> Destinations.externalPlayer(position)
		else -> Destinations.videoPlayer(position)
	}

	override fun interceptPlayRequest(context: Context, item: BaseItemDto?): Boolean = false
}

class RewritePlaybackLauncher(
	private val navigationRepository: NavigationRepository,
) : PlaybackLauncher {
	override fun useExternalPlayer(itemType: BaseItemKind?) = false
	override fun getPlaybackDestination(itemType: BaseItemKind?, position: Int) =
		fragmentDestination<PlaybackRewriteFragment>()

	override fun interceptPlayRequest(context: Context, item: BaseItemDto?): Boolean {
		if (item == null) return false
		if (item.mediaType == MediaType.Audio) return false

		navigationRepository.navigate(fragmentDestination<PlaybackRewriteFragment>(
			PlaybackRewriteFragment.EXTRA_ITEM_ID to item.id.toString()
		))

		return true
	}
}
