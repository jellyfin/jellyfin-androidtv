package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer

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

		/**
		 * Chosen player for play with button. Changes every time user chooses a player with "play with" button.
		 */
		var chosenPlayer = Preference.enum("chosen_player", PreferredVideoPlayer.VLC)
	}
}
