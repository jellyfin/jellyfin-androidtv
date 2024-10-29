package org.jellyfin.androidtv.data.compat

import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaSourceInfo
import java.util.UUID

open class AudioOptions {
	var enableDirectPlay = true
	var enableDirectStream = true
	var itemId: UUID? = null
	var mediaSources: List<MediaSourceInfo>? = null
	var profile: DeviceProfile? = null

	/**
	 * Optional. Only needed if a specific AudioStreamIndex or SubtitleStreamIndex are requested.
	 */
	var mediaSourceId: String? = null

	/**
	 * Allows an override of supported number of audio channels
	 * Example: DeviceProfile supports five channel, but user only has stereo speakers
	 */
	var maxAudioChannels: Int? = null
}
