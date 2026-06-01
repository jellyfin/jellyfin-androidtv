package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.Preference
import org.jellyfin.sdk.model.api.VideoRangeType

/**
 * HDR formats that the user can override in the device profile.
 */
enum class HdrFormat(
	val nameRes: Int,
	val descriptionRes: Int,
	val videoRangeTypes: Set<VideoRangeType>,
	val preference: Preference<HdrOverrideMode>,
) {
	HDR10(
		R.string.hdr10_override,
		R.string.hdr10_override_description,
		setOf(VideoRangeType.HDR10),
		UserPreferences.hdr10Override,
	),
	HDR10_PLUS(
		R.string.hdr10_plus_override,
		R.string.hdr10_plus_override_description,
		setOf(VideoRangeType.HDR10_PLUS),
		UserPreferences.hdr10PlusOverride,
	),
	DOVI_PROFILE_5(
		R.string.dovi_profile_5,
		R.string.dovi_profile_5_description,
		setOf(VideoRangeType.DOVI),
		UserPreferences.doviProfile5Override,
	),
	DOVI_PROFILE_7(
		R.string.dovi_profile_7,
		R.string.dovi_profile_7_description,
		setOf(VideoRangeType.DOVI_WITH_EL, VideoRangeType.DOVI_WITH_ELHDR10_PLUS),
		UserPreferences.doviProfile7Override,
	),
	DOVI_PROFILE_8(
		R.string.dovi_profile_8,
		R.string.dovi_profile_8_description,
		setOf(VideoRangeType.DOVI_WITH_HDR10, VideoRangeType.DOVI_WITH_HDR10_PLUS),
		UserPreferences.doviProfile8Override,
	),
}
