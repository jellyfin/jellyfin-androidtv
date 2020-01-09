package org.jellyfin.androidtv.util

import android.content.Context

class SystemPreferences(context: Context) : SharedPreferenceStore(context.getSharedPreferences("systemprefs", Context.MODE_PRIVATE)) {
	var configVersion by stringPreference("sys_pref_config_version", "5")
	var audioWarned by booleanPreference("syspref_audio_warned", false)

	// Live TV - Channel history
	var liveTvLastChannel by stringPreference("sys_pref_last_tv_channel", null)
	var liveTvPrevChannel by stringPreference("sys_pref_prev_tv_channel", null)

	// Live TV - Guide Filters
	var liveTvGuideFilterKids by booleanPreference("guide_filter_kids", false)
	var liveTvGuideFilterMovies by booleanPreference("guide_filter_movies", false)
	var liveTvGuideFilterNews by booleanPreference("guide_filter_news", false)
	var liveTvGuideFilterPremiere by booleanPreference("guide_filter_premiere", false)
	var liveTvGuideFilterSeries by booleanPreference("guide_filter_series", false)
	var liveTvGuideFilterSports by booleanPreference("guide_filter_sports", false)
}
