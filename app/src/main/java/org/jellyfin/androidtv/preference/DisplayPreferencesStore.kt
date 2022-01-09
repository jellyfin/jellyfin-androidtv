package org.jellyfin.androidtv.preference

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.displayPreferencesApi
import org.jellyfin.sdk.model.api.DisplayPreferencesDto
import org.jellyfin.sdk.model.api.ScrollDirection
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber

abstract class DisplayPreferencesStore(
	protected var displayPreferencesId: String,
	protected var app: String = "jellyfin-androidtv",
	private val api: ApiClient,
) : AsyncPreferenceStore, BasicPreferenceStore() {
	private var displayPreferencesDto: DisplayPreferencesDto? = null
	private var cachedPreferences: MutableMap<String, String?> = mutableMapOf()
	override val shouldUpdate: Boolean
		get() = displayPreferencesDto == null

	override suspend fun commit(): Boolean {
		if (displayPreferencesDto == null) return false

		try {
			api.displayPreferencesApi.updateDisplayPreferences(
				displayPreferencesId = displayPreferencesId,
				client = app,
				data = displayPreferencesDto!!.copy(
					customPrefs = cachedPreferences
				)
			)
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to save displaypreferences. (displayPreferencesId=$displayPreferencesId, app=$app)")
			return false
		}

		return true
	}

	/**
	 * Clear local copy of display preferences and require an update for new modifications.
	 */
	fun clearCache(): Boolean {
		if (displayPreferencesDto == null) return false

		displayPreferencesDto = null
		cachedPreferences.clear()

		return true
	}

	override suspend fun update(): Boolean {
		try {
			val result by api.displayPreferencesApi.getDisplayPreferences(
				displayPreferencesId = displayPreferencesId,
				client = app
			)
			displayPreferencesDto = result
			cachedPreferences = result.customPrefs.toMutableMap()

			return true
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to retrieve displaypreferences. (displayPreferencesId=$displayPreferencesId, app=$app)")

			if (displayPreferencesDto == null) {
				Timber.i("Creating an empty DisplayPreferencesDto for next commit.")
				displayPreferencesDto = DisplayPreferencesDto.empty()
			}

			return false
		}
	}

	override fun getInt(keyName: String, defaultValue: Int) =
		cachedPreferences[keyName]?.toIntOrNull() ?: defaultValue

	override fun getLong(keyName: String, defaultValue: Long) =
		cachedPreferences[keyName]?.toLongOrNull() ?: defaultValue

	override fun getBool(keyName: String, defaultValue: Boolean) =
		cachedPreferences[keyName]?.toBooleanStrictOrNull() ?: defaultValue

	override fun getString(keyName: String, defaultValue: String) =
		cachedPreferences[keyName] ?: defaultValue

	override fun setInt(keyName: String, value: Int) {
		cachedPreferences[keyName] = value.toString()
	}

	override fun setLong(keyName: String, value: Long) {
		cachedPreferences[keyName] = value.toString()
	}

	override fun setBool(keyName: String, value: Boolean) {
		cachedPreferences[keyName] = value.toString()
	}

	override fun setString(keyName: String, value: String) {
		cachedPreferences[keyName] = value
	}

	override fun <T : Preference<V>, V : Any> delete(preference: T) {
		cachedPreferences.remove(preference.key)
	}

	/**
	 * Create an empty [DisplayPreferencesDto] with default values.
	 */
	private fun DisplayPreferencesDto.Companion.empty() = DisplayPreferencesDto(
		primaryImageHeight = 0,
		primaryImageWidth = 0,
		customPrefs = emptyMap(),
		rememberIndexing = false,
		scrollDirection = ScrollDirection.HORIZONTAL,
		rememberSorting = false,
		showBackdrop = false,
		showSidebar = false,
		sortOrder = SortOrder.ASCENDING
	)
}
