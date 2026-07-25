package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

/**
 * Whether this item is part of a group of alternate versions. Every version is a separate item that
 * is also exposed as a media source of the other versions in the group, so an item with multiple
 * media sources always has alternate versions.
 */
val BaseItemDto.hasAlternateVersions: Boolean
	get() = (mediaSources?.size ?: 0) > 1

/**
 * The media source of this item itself. The id of a media source is the id of the item owning it,
 * although it is formatted without dashes and can therefore not be compared directly.
 */
val BaseItemDto.ownMediaSource: MediaSourceInfo?
	get() = mediaSources?.firstOrNull { it.id?.toUUIDOrNull() == id }

/**
 * The id of the media source to play to get the version this item represents, or null when the item
 * has no alternate versions and the server can pick the media source itself.
 */
val BaseItemDto.versionMediaSourceId: String?
	get() = if (hasAlternateVersions) ownMediaSource?.id else null

/**
 * Find the version of this item named [name] to continue playing in the same version as another
 * item. Returns null when this item has no alternate versions or when none of them match.
 */
fun BaseItemDto.findVersionByName(name: String?): MediaSourceInfo? {
	if (name.isNullOrEmpty() || !hasAlternateVersions) return null

	return mediaSources?.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
