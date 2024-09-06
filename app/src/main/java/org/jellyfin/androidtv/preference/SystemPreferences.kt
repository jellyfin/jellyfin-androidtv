package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

/**
 * System preferences are not possible to modify by the user.
 * They are mostly used to store states for various filters and warnings.
 *
 * @param context Context to get the SharedPreferences from
 */
class SystemPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("systemprefs", Context.MODE_PRIVATE)
) {
	companion object {
		// Live TV - Channel history
		/**
		 * Stores the channel that was active before leaving the app
		 */
		val liveTvLastChannel = stringPreference("sys_pref_last_tv_channel", "")

		/**
		 * Also stores the channel that was active before leaving the app I think
		 */
		val liveTvPrevChannel = stringPreference("sys_pref_prev_tv_channel", "")

		// Live TV - Guide Filters
		/**
		 * Stores whether the kids filter is active in the channel guide or not
		 */
		val liveTvGuideFilterKids = booleanPreference("guide_filter_kids", false)

		/**
		 * Stores whether the movies filter is active in the channel guide or not
		 */
		val liveTvGuideFilterMovies = booleanPreference("guide_filter_movies", false)

		/**
		 * Stores whether the news filter is active in the channel guide or not
		 */
		val liveTvGuideFilterNews = booleanPreference("guide_filter_news", false)

		/**
		 * Stores whether the premiere filter is active in the channel guide or not
		 */
		val liveTvGuideFilterPremiere = booleanPreference("guide_filter_premiere", false)

		/**
		 * Stores whether the series filter is active in the channel guide or not
		 */
		val liveTvGuideFilterSeries = booleanPreference("guide_filter_series", false)

		/**
		 * Stores whether the sports filter is active in the channel guide or not
		 */
		val liveTvGuideFilterSports = booleanPreference("guide_filter_sports", false)

		// Other persistent variables
		/**
		 * The version name for the latest dismissed beta notification or empty if none.
		 */
		val dismissedBetaNotificationVersion = stringPreference("dismissed_beta_notification_version", "")

		/**
		 * Whether to disable the "UI mode" warning that shows when using the app on non TV devices.
		 */
		val disableUiModeWarning = booleanPreference("disable_ui_mode_warning", false)
	}
}
