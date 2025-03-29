package org.jellyfin.androidtv.preference.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.preference.Preference
import org.jellyfin.preference.PreferenceEnum
import org.jellyfin.preference.migration.MigrationContext
import org.jellyfin.preference.store.AsyncPreferenceStore
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.displayPreferencesApi
import org.jellyfin.sdk.model.api.DisplayPreferencesDto
import org.jellyfin.sdk.model.api.ScrollDirection
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber

@Suppress("TooManyFunctions")
abstract class DisplayPreferencesStore(
	protected var displayPreferencesId: String,
	protected var app: String = "jellyfin-androidtv",
	private val api: ApiClient,
) : AsyncPreferenceStore<Unit, Unit>() {
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
			val result = withContext(Dispatchers.IO) {
				api.displayPreferencesApi.getDisplayPreferences(
					displayPreferencesId = displayPreferencesId,
					client = app
				).content
			}
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

	override fun getInt(key: String, defaultValue: Int) =
		cachedPreferences[key]?.toIntOrNull() ?: defaultValue

	override fun getLong(key: String, defaultValue: Long) =
		cachedPreferences[key]?.toLongOrNull() ?: defaultValue

	override fun getFloat(key: String, defaultValue: Float) =
		cachedPreferences[key]?.toFloatOrNull() ?: defaultValue

	override fun getBool(key: String, defaultValue: Boolean) =
		cachedPreferences[key]?.toBooleanStrictOrNull() ?: defaultValue

	override fun getString(key: String, defaultValue: String) =
		cachedPreferences[key] ?: defaultValue

	override fun setInt(key: String, value: Int) {
		cachedPreferences[key] = value.toString()
	}

	override fun setLong(key: String, value: Long) {
		cachedPreferences[key] = value.toString()
	}

	override fun setFloat(key: String, value: Float) {
		cachedPreferences[key] = value.toString()
	}

	override fun setBool(key: String, value: Boolean) {
		cachedPreferences[key] = value.toString()
	}

	override fun setString(key: String, value: String) {
		cachedPreferences[key] = value
	}

	override fun <T : Any> delete(preference: Preference<T>) {
		cachedPreferences.remove(preference.key)
	}

	override fun <T : Enum<T>> getEnum(preference: Preference<T>): T {
		val stringValue = cachedPreferences[preference.key]
		return if (stringValue.isNullOrBlank()) preference.defaultValue
		else preference.type.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: preference.defaultValue
	}

	override fun <V : Enum<V>> setEnum(preference: Preference<*>, value: Enum<V>) =
		setString(
			preference.key, when (value) {
				is PreferenceEnum -> value.serializedName ?: value.name
				else -> value.name
			}
		)

	override fun runMigrations(body: MigrationContext<Unit, Unit>.() -> Unit) {
		TODO("The DisplayPreferencesStore does not support migrations")
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
