package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.content.Intent
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.activityDestination
import org.jellyfin.androidtv.ui.playback.rewrite.PlaybackForwardingActivity
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

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

class RewritePlaybackLauncher : PlaybackLauncher {
	override fun useExternalPlayer(itemType: BaseItemKind?) = false
	override fun getPlaybackDestination(itemType: BaseItemKind?, position: Int) =
		activityDestination<PlaybackForwardingActivity>()

	override fun interceptPlayRequest(context: Context, item: BaseItemDto?): Boolean {
		if (item == null) return false

		val intent = Intent(context, PlaybackForwardingActivity::class.java)
		intent.putExtra(PlaybackForwardingActivity.EXTRA_ITEM_ID, item.id)
		context.startActivity(intent)

		return true
	}
}
