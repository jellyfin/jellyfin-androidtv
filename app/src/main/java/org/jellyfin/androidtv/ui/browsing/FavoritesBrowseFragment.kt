package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.sdk.model.api.BaseItemKind

class FavoritesBrowseFragment : BrowseFolderFragment() {
	private val changeTriggers = arrayOf(
		ChangeTriggerType.LibraryUpdated,
		ChangeTriggerType.FavoriteUpdate,
	)

	private val favoriteRowDefinitions = listOf(
		BaseItemKind.MOVIE to R.string.lbl_movies,
		BaseItemKind.EPISODE to R.string.lbl_episodes,
		BaseItemKind.SERIES to R.string.lbl_series,
		BaseItemKind.SEASON to R.string.lbl_seasons,
		BaseItemKind.BOX_SET to R.string.lbl_collections,
		BaseItemKind.MUSIC_ARTIST to R.string.lbl_artists,
		BaseItemKind.MUSIC_ALBUM to R.string.lbl_albums,
		BaseItemKind.AUDIO to R.string.lbl_songs,
		BaseItemKind.VIDEO to R.string.lbl_videos,
	)

		override suspend fun setupQueries(rowLoader: RowLoader) {
			val browseRows = favoriteRowDefinitions.mapTo(mutableListOf<BrowseRowDef>()) { (kind, titleRes) ->
				BrowseRowDef(
					getString(titleRes),
					BrowsingUtils.createFavoritesRequest(includeItemTypes = setOf(kind)),
					60,
					false,
					true,
					changeTriggers,
				)
			}

		rowLoader.loadRows(browseRows)
	}
}
