package org.jellyfin.androidtv.constant

enum class QualityProfiles(val quality: String) {
	Quality_0("0"),
	Quality_120("120"),
	Quality_110("110"),
	Quality_100("100"),
	Quality_90("90"),
	Quality_80("80"),
	Quality_70("70"),
	Quality_60("60"),
	Quality_50("50"),
	Quality_40("40"),
	Quality_30("30"),
	Quality_20("20"),
	Quality_15("15"),
	Quality_10("10"),
	Quality_5("5"),
	Quality_3("3"),
	Quality_2("2"),
	Quality_1("1"),
	Quality_072("0.72"),
	Quality_042("0.42");


	companion object {
		private val mapping = values().associateBy(QualityProfiles::quality)
		fun fromPreference(quality: String) = mapping[quality]
	}

}
