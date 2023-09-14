package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.apiclient.model.querying.ItemFields
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.android.ext.android.inject

class SuggestedMoviesFragment : EnhancedBrowseFragment() {
	private val api by inject<ApiClient>()

	init {
		showViews = false
	}

	override fun setupQueries(rowLoader: RowLoader) {
		lifecycleScope.launch {
			val response by api.itemsApi.getItemsByUserId(
				parentId = mFolder.id,
				includeItemTypes = setOf(BaseItemKind.MOVIE),
				sortOrder = setOf(SortOrder.DESCENDING),
				sortBy = setOf("DatePlayed"),
				limit = 8,
				recursive = true,
			)

			for (item in response.items.orEmpty()) {
				// TODO: Migrate ItemRowAdapter to SDK
				//	val similar = GetSimilarItemsRequest(
				//		itemId = item.id,
				//		fields = setOf(
				//			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
				//			ItemFields.OVERVIEW,
				//			ItemFields.CHILD_COUNT,
				//			ItemFields.MEDIA_STREAMS,
				//			ItemFields.MEDIA_SOURCES
				//		),
				//		limit = 7,
				//	)
				val similar = SimilarItemsQuery()
				similar.id = item.id.toString()
				similar.fields = arrayOf(
					ItemFields.PrimaryImageAspectRatio,
					ItemFields.Overview,
					ItemFields.ChildCount,
					ItemFields.MediaStreams,
					ItemFields.MediaSources
				)
				similar.limit = 7
				mRows.add(BrowseRowDef(getString(R.string.because_you_watched, item.name), similar, QueryType.SimilarMovies))
			}

			rowLoader.loadRows(mRows)
		}
	}
}
