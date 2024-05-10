package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.sdk.model.api.BaseItemKind

interface PlaybackLauncher {
	fun useExternalPlayer(itemType: BaseItemKind?): Boolean
	fun getPlaybackDestination(itemType: BaseItemKind?, position: Int): Destination
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
}

class RewritePlaybackLauncher : PlaybackLauncher {
	override fun useExternalPlayer(itemType: BaseItemKind?) = false
	override fun getPlaybackDestination(itemType: BaseItemKind?, position: Int) = Destinations.playbackRewritePlayer(position)
}
