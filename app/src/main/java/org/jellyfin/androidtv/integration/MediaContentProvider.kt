package org.jellyfin.androidtv.integration

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.integration.provider.ImageProvider
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.sdk.isUsable
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.ImageType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZoneId

class MediaContentProvider : ContentProvider(), KoinComponent {
	companion object {
		private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.content"
		private const val SUGGEST_PATH = "suggestions"
		private const val SEARCH_SUGGEST = 1
		private const val TICKS_IN_MILLISECOND = 10000
		private const val DEFAULT_LIMIT = 10
		private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
			addURI(AUTHORITY, "$SUGGEST_PATH/${SearchManager.SUGGEST_URI_PATH_QUERY}", SEARCH_SUGGEST)
			addURI(AUTHORITY, "$SUGGEST_PATH/${SearchManager.SUGGEST_URI_PATH_QUERY}/*", SEARCH_SUGGEST)
		}
	}

	private val api by inject<ApiClient>()
	private val imageHelper by inject<ImageHelper>()

	override fun onCreate(): Boolean = api.isUsable

	override fun query(
		uri: Uri,
		projection: Array<String>?,
		selection: String?,
		selectionArgs: Array<String>?,
		sortOrder: String?
	): Cursor? {
		Timber.d("Query: %s", uri)

		when (uriMatcher.match(uri)) {
			SEARCH_SUGGEST -> {
				val query = selectionArgs?.firstOrNull() ?: uri.lastPathSegment ?: return null
				Timber.d("Search query: $query")

				val limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)?.toIntOrNull()
					?: DEFAULT_LIMIT
				return runBlocking { getSuggestions(query, limit) }
			}

			else -> throw IllegalArgumentException("Unknown Uri: $uri")
		}
	}

	/**
	 * Gets the resumable items or returns null
	 */
	private suspend fun searchItems(query: String, limit: Int): BaseItemDtoQueryResult? = try {
		val items by api.itemsApi.getItems(
			searchTerm = query,
			recursive = true,
			limit = limit,
			fields = ItemRepository.itemFields
		)

		items
	} catch (err: ApiClientException) {
		Timber.e(err, "Unable to query API for search results")
		null
	}

	private suspend fun getSuggestions(query: String, limit: Int) = withContext(Dispatchers.IO) {
		val searchResult = searchItems(query, limit)
		if (searchResult != null) Timber.d("Query resulted in %d items", searchResult.totalRecordCount)

		val columns = arrayOf(
			BaseColumns._ID,
			SearchManager.SUGGEST_COLUMN_DURATION,
			SearchManager.SUGGEST_COLUMN_IS_LIVE,
			SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT,
			SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
			SearchManager.SUGGEST_COLUMN_QUERY,
			SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
			SearchManager.SUGGEST_COLUMN_TEXT_1,
			SearchManager.SUGGEST_COLUMN_TEXT_2,
			SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
			SearchManager.SUGGEST_COLUMN_INTENT_DATA,
		)

		MatrixCursor(columns).also { cursor ->
			searchResult?.items?.forEach { item ->
				val imageUri = ImageProvider.getImageUri(
					item.itemImages[ImageType.PRIMARY]?.getUrl(api)
						?: imageHelper.getResourceUrl(context!!, R.drawable.tile_land_tv)
				)

				cursor.newRow().apply {
					add(BaseColumns._ID, item.id)
					add(SearchManager.SUGGEST_COLUMN_DURATION, item.runTimeTicks?.run { div(TICKS_IN_MILLISECOND) })
					add(SearchManager.SUGGEST_COLUMN_IS_LIVE, if (item.isLive == true) 1 else 0)
					val lastAccess = item.userData?.lastPlayedDate?.atZone(ZoneId.systemDefault())?.toEpochSecond()
					add(SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT, lastAccess)
					add(SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR, item.premiereDate?.year)
					add(SearchManager.SUGGEST_COLUMN_QUERY, item.name)
					add(SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE, imageUri)
					add(SearchManager.SUGGEST_COLUMN_TEXT_1, item.name)
					add(SearchManager.SUGGEST_COLUMN_TEXT_2, item.taglines?.firstOrNull())
					add(SearchManager.SUGGEST_COLUMN_INTENT_ACTION, Intent.ACTION_VIEW)
					add(SearchManager.SUGGEST_COLUMN_INTENT_DATA, item.id)
				}
			}
		}
	}

	override fun getType(p0: Uri): String = SearchManager.SUGGEST_MIME_TYPE
	override fun insert(p0: Uri, p1: ContentValues?): Uri? = null
	override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int = 0
	override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = 0
}
