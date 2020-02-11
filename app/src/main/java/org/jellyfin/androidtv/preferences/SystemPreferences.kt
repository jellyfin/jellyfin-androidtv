package org.jellyfin.androidtv.preferences

import android.content.Context

/**
 * System preferences are not possible to modify by the user.
 * They are mostly used to store states for various filters and warnings.
 *
 * @param context Context to get the SharedPreferences from
 */
class SystemPreferences(context: Context) : SharedPreferenceStore(context.getSharedPreferences("systemprefs", Context.MODE_PRIVATE)) {
	// Warnings
	/**
	 * Used to track if the warning for the `pref_audio_option` has been shown.
	 */
	var audioWarned by booleanPreference("syspref_audio_warned", false)

	// Live TV - Channel history
	/**
	 * Stores the channel that was active before leaving the app
	 */
	var liveTvLastChannel by stringPreferenceNullable("sys_pref_last_tv_channel", null)

	/**
	 * Also stores the channel that was active before leaving the app I think
	 */
	var liveTvPrevChannel by stringPreferenceNullable("sys_pref_prev_tv_channel", null)

	// Live TV - Guide Filters
	/**
	 * Stores whether the kids filter is active in the channel guide or not
	 */
	var liveTvGuideFilterKids by booleanPreference("guide_filter_kids", false)

	/**
	 * Stores whether the movies filter is active in the channel guide or not
	 */
	var liveTvGuideFilterMovies by booleanPreference("guide_filter_movies", false)

	/**
	 * Stores whether the news filter is active in the channel guide or not
	 */
	var liveTvGuideFilterNews by booleanPreference("guide_filter_news", false)

	/**
	 * Stores whether the premiere filter is active in the channel guide or not
	 */
	var liveTvGuideFilterPremiere by booleanPreference("guide_filter_premiere", false)

	/**
	 * Stores whether the series filter is active in the channel guide or not
	 */
	var liveTvGuideFilterSeries by booleanPreference("guide_filter_series", false)

	/**
	 * Stores whether the sports filter is active in the channel guide or not
	 */
	var liveTvGuideFilterSports by booleanPreference("guide_filter_sports", false)
}
