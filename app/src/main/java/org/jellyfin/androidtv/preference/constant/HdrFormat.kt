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
		nameRes = R.string.hdr10_override,
		descriptionRes = R.string.hdr10_override_description,
		videoRangeTypes = setOf(VideoRangeType.HDR10),
		preference = UserPreferences.hdr10Override,
	),
	HDR10_PLUS(
		nameRes = R.string.hdr10_plus_override,
		descriptionRes = R.string.hdr10_plus_override_description,
		videoRangeTypes = setOf(VideoRangeType.HDR10_PLUS),
		preference = UserPreferences.hdr10PlusOverride,
	),
	DOVI_PROFILE_5(
		nameRes = R.string.dovi_profile_5,
		descriptionRes = R.string.dovi_profile_5_description,
		videoRangeTypes = setOf(VideoRangeType.DOVI),
		preference = UserPreferences.doviProfile5Override,
	),
	DOVI_PROFILE_7(
		nameRes = R.string.dovi_profile_7,
		descriptionRes = R.string.dovi_profile_7_description,
		videoRangeTypes = setOf(VideoRangeType.DOVI_WITH_EL, VideoRangeType.DOVI_WITH_ELHDR10_PLUS),
		preference = UserPreferences.doviProfile7Override,
	),
	DOVI_PROFILE_8(
		nameRes = R.string.dovi_profile_8,
		descriptionRes = R.string.dovi_profile_8_description,
		videoRangeTypes = setOf(VideoRangeType.DOVI_WITH_HDR10, VideoRangeType.DOVI_WITH_HDR10_PLUS),
		preference = UserPreferences.doviProfile8Override,
	),
}