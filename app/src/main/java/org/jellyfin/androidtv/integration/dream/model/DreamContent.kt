package org.jellyfin.androidtv.integration.dream.model

import android.graphics.Bitmap
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.LyricDto

sealed interface DreamContent {
	data object Logo : DreamContent
	data class LibraryShowcase(val item: BaseItemDto, val backdrop: Bitmap, val logo: Bitmap?) : DreamContent
	data class NowPlaying(val item: BaseItemDto, val lyrics: LyricDto?) : DreamContent
}
