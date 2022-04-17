package org.jellyfin.androidtv.util.sdk

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

fun BaseItemDto.getDisplayName(context: Context): String {
	val seasonNumber = when {
		type == BaseItemKind.EPISODE
			&& parentIndexNumber != null
			&& parentIndexNumber != 0 ->
			context.getString(R.string.lbl_season_number, parentIndexNumber)
		else -> null
	}
	val episodeNumber = when {
		type != BaseItemKind.EPISODE -> indexNumber?.toString()
		parentIndexNumber == 0 -> context.getString(R.string.lbl_special)
		else -> indexNumber?.let { start ->
			indexNumberEnd?.let { end -> context.getString(R.string.lbl_episode_range, start, end) }
				?: context.getString(R.string.lbl_episode_number, start)
		}
	}
	val seasonEpisodeNumbers = listOfNotNull(seasonNumber, episodeNumber).joinToString(":")

	val nameSeparator = when (type) {
		BaseItemKind.EPISODE -> " — "
		else -> ". "
	}

	return listOfNotNull(seasonEpisodeNumbers, name)
		.filter { it.isNotEmpty() }
		.joinToString(nameSeparator)
}
