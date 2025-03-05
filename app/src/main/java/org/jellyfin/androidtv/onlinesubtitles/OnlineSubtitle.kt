package org.jellyfin.androidtv.onlinesubtitles

data class OnlineSubtitle(
	val localSubtitleId : Int,
	val localFileName : String,
	val language : String,
	val title : String,
	val type: OnlineSubtitleType,
	val downloadParamStr1: String? = null,
	val downloadParamLong1: Long?= null,
	val downloadParamInt1: Int?= null,
	val offset: Long = 0,
)
