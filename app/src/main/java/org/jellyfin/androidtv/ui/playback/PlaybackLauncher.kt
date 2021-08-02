package org.jellyfin.androidtv.ui.playback

import android.app.Activity
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.apiclient.model.dto.BaseItemType

interface PlaybackLauncher {
	fun useExternalPlayer(itemType: BaseItemType?): Boolean
	fun getPlaybackActivityClass(itemType: BaseItemType?): Class<out Activity>
}

class GarbagePlaybackLauncher(
	private val userPreferences: UserPreferences
) : PlaybackLauncher {
	override fun useExternalPlayer(itemType: BaseItemType?) = when (itemType) {
		BaseItemType.Movie,
		BaseItemType.Episode,
		BaseItemType.Video,
		BaseItemType.Series,
		BaseItemType.Recording,
		-> userPreferences[UserPreferences.videoPlayer] === PreferredVideoPlayer.EXTERNAL
		BaseItemType.TvChannel,
		BaseItemType.Program,
		-> userPreferences[UserPreferences.liveTvVideoPlayer] === PreferredVideoPlayer.EXTERNAL
		else -> false
	}

	override fun getPlaybackActivityClass(itemType: BaseItemType?) = when {
		useExternalPlayer(itemType) -> ExternalPlayerActivity::class.java
		else -> PlaybackOverlayActivity::class.java
	}
}
