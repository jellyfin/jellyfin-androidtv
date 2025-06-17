package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Represents a section of media items with a title
 */
data class MediaSection(
    val title: String,
    val items: List<BaseItemDto>
)
