package org.jellyfin.androidtv.content

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import io.ktor.util.*
import kotlinx.coroutines.*
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.di.systemApiClient
import org.jellyfin.apiclient.model.querying.ItemSortBy
import org.jellyfin.sdk.api.client.KtorClient
import org.jellyfin.sdk.api.operations.ImageApi
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.*
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.text.SimpleDateFormat


@KoinApiExtension
class MediaContentProvider : ContentProvider(), KoinComponent {

	companion object {
		const val AUTHORITY = "org.jellyfin.androidtv.content"
		const val SUGGEST_PATH = "suggestions"
		private const val TICKS_IN_MILLISECOND = 10000
	}

	private val apiClient by inject<KtorClient>(systemApiClient)
	private val itemsApi by lazy { ItemsApi(apiClient) }
	private val imageApi by lazy { ImageApi(apiClient) }

	private val SEARCH_SUGGEST = 1

	private val sURIMatcher = UriMatcher(UriMatcher.NO_MATCH)

	init {
		sURIMatcher.addURI(AUTHORITY, SUGGEST_PATH + "/" + SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST)
		sURIMatcher.addURI(AUTHORITY, SUGGEST_PATH + "/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST)
	}

	override fun onCreate(): Boolean {
		return true
	}

	override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
					   sortOrder: String?): Cursor? {
		Timber.d("Query: %s", uri)
		// Use the UriMatcher to see what kind of query we have and format the db query accordingly
		when (sURIMatcher.match(uri)) {
			SEARCH_SUGGEST -> {
				Timber.d("search suggest: ${selectionArgs?.get(0)} URI: $uri")
				var limit = 10
				if (uri.getQueryParameter("limit") != null)
					limit = uri.getQueryParameter("limit")!!.toInt()
				return uri.lastPathSegment?.let { getSuggestions(it.toLowerCase(), limit) }
			}
			else -> throw IllegalArgumentException("Unknown Uri: $uri")
		}
	}

	/**
	 * Gets the resumable items or returns null
	 */
	private suspend fun searchItems(query: String, qLimit: Int): BaseItemDtoQueryResult? {
		// Get user or return if no user is found (not authenticated)
		val userId = TvApp.getApplication().currentUser?.id?.toUUIDOrNull() ?: return null

		return itemsApi.getItems(
				recursive = true,
				enableTotalRecordCount = true,
				collapseBoxSetItems = false,
				excludeLocationTypes = listOf(LocationType.VIRTUAL),
				sortBy = listOf(ItemSortBy.DateCreated),
				sortOrder = listOf(SortOrder.DESCENDING),
				searchTerm = query,

				userId = userId,
				imageTypeLimit = 1,
				limit = qLimit,
				fields = listOf(ItemFields.DATE_CREATED, ItemFields.STUDIOS, ItemFields.GENRES, ItemFields.TAGLINES, ItemFields.PROVIDER_IDS, ItemFields.MEDIA_STREAMS)
		).content
	}

	private fun getSuggestions(query: String, limit: Int): Cursor = runBlocking {
		val searchResult = async { searchItems(query, limit) }.await()
		if (searchResult != null) {
			Timber.d("Search returned %d items", searchResult.totalRecordCount)
		}

		val menuCols = arrayOf(
				BaseColumns._ID,
				SearchManager.SUGGEST_COLUMN_TEXT_1,
				SearchManager.SUGGEST_COLUMN_TEXT_2,
				SearchManager.SUGGEST_COLUMN_ICON_1,
				SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
				SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
				SearchManager.SUGGEST_COLUMN_INTENT_DATA,
				SearchManager.SUGGEST_COLUMN_VIDEO_WIDTH,
				SearchManager.SUGGEST_COLUMN_VIDEO_HEIGHT,
				SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
				SearchManager.SUGGEST_COLUMN_DURATION,
				SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
		)
		val mc = MatrixCursor(menuCols)

		searchResult?.items?.forEach { item ->
			val imageUri = Uri.parse(imageApi.getItemImageUrl(
					itemId = item.id,
					imageType = ImageType.PRIMARY,
					format = ImageFormat.PNG,
					maxHeight = 512,
					maxWidth = 512
			))
			val duration = when {
				item.runTimeTicks != null -> (item.runTimeTicks!! / TICKS_IN_MILLISECOND).toInt()
				else -> 0
			}
			val release = when {
				item.premiereDate != null -> item.premiereDate!!.year.toString()
				else -> ""
			}

			Timber.d("Result %s/%s - %d / %s", item.name, item.id, duration, release)
			mc.addRow(arrayOf<Any>(
					item.id,
					item.name!!,
					when {
						item.taglines != null && item.taglines!!.size > 0 -> item.taglines!![0]
						else -> ""
					},
					imageUri,
					imageUri,
					Intent.ACTION_VIEW,
					item.id,
					0,
					0,
					release,
					duration,
					-1
			))
		}
		return@runBlocking mc
	}

	override fun getType(p0: Uri): String? {
		TODO("Not yet implemented")
	}

	override fun insert(p0: Uri, p1: ContentValues?): Uri? {
		TODO("Not yet implemented")
	}

	override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
		TODO("Not yet implemented")
	}

	override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
		TODO("Not yet implemented")
	}

}
