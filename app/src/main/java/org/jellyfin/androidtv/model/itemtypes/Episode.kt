package org.jellyfin.androidtv.model.itemtypes

import android.graphics.Bitmap

data class Episode (
	val id: String,
	val canDelete: Boolean,
	val canDownload: Boolean,
	//TODO: Chapters: ArrayList<ChapterInfoDto>
	val communityRating: Float,
	val name: String,
	val overview: String,
	val primaryImage: Bitmap?
)
