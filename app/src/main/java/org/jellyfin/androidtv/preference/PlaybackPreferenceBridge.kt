package org.jellyfin.androidtv.preference

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * We'll migrate our preference code to use flows eventually, until that's ready we need a bridge
 * to convert our preferences to a flow and update the values at the correct moments.
 */
class PlaybackPreferenceBridge(
	private val userSettingPreferences: UserSettingPreferences,
) {
	var defaultRewindAmount = MutableStateFlow(10.seconds)
	var defaultFastForwardAmount = MutableStateFlow(10.seconds)

	fun update() {
		defaultRewindAmount.value = userSettingPreferences[UserSettingPreferences.skipBackLength].milliseconds
		defaultFastForwardAmount.value = userSettingPreferences[UserSettingPreferences.skipForwardLength].milliseconds
	}
}
