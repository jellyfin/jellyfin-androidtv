package org.jellyfin.androidtv.preferences

import android.content.Context

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
		// Warnings
		/**
		 * Used to track if the warning for the `pref_audio_option` has been shown.
		 */
		val audioWarned = Preference.boolean("syspref_audio_warned", false)

		// Live TV - Channel history
		/**
		 * Stores the channel that was active before leaving the app
		 */
		val liveTvLastChannel = Preference.string("sys_pref_last_tv_channel", "")

		/**
		 * Also stores the channel that was active before leaving the app I think
		 */
		val liveTvPrevChannel = Preference.string("sys_pref_prev_tv_channel", "")

		// Live TV - Guide Filters
		/**
		 * Stores whether the kids filter is active in the channel guide or not
		 */
		val liveTvGuideFilterKids = Preference.boolean("guide_filter_kids", false)

		/**
		 * Stores whether the movies filter is active in the channel guide or not
		 */
		val liveTvGuideFilterMovies = Preference.boolean("guide_filter_movies", false)

		/**
		 * Stores whether the news filter is active in the channel guide or not
		 */
		val liveTvGuideFilterNews = Preference.boolean("guide_filter_news", false)

		/**
		 * Stores whether the premiere filter is active in the channel guide or not
		 */
		val liveTvGuideFilterPremiere = Preference.boolean("guide_filter_premiere", false)

		/**
		 * Stores whether the series filter is active in the channel guide or not
		 */
		val liveTvGuideFilterSeries = Preference.boolean("guide_filter_series", false)

		/**
		 * Stores whether the sports filter is active in the channel guide or not
		 */
		val liveTvGuideFilterSports = Preference.boolean("guide_filter_sports", false)
	}
}
