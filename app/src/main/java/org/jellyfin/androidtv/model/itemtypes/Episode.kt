package org.jellyfin.androidtv.model.itemtypes

import android.graphics.Bitmap

data class Episode (
	val id: String,
	//TODO: Chapters: ArrayList<ChapterInfoDto>
	val communityRating: Double,
	val name: String,
	val description: String
)
