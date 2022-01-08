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

	@Suppress("UNCHECKED_CAST")
	override operator fun <T : Any> get(preference: Preference<T>) = when (preference.type) {
		Int::class -> cachedPreferences[preference.key]?.toIntOrNull() ?: preference.defaultValue
		Long::class -> cachedPreferences[preference.key]?.toLongOrNull() ?: preference.defaultValue
		Boolean::class -> cachedPreferences[preference.key]?.toBooleanStrictOrNull()
			?: preference.defaultValue
		String::class -> cachedPreferences[preference.key] ?: preference.defaultValue

		else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
	} as T

	override operator fun <T : Any> set(preference: Preference<T>, value: T) =
		when (preference.type) {
			Int::class -> cachedPreferences[preference.key] = (value as Int).toString()
			Long::class -> cachedPreferences[preference.key] = (value as Long).toString()
			Boolean::class -> cachedPreferences[preference.key] = (value as Boolean).toString()
			String::class -> cachedPreferences[preference.key] = (value as String).toString()
			Enum::class -> cachedPreferences[preference.key] = value.toString()

			else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
		}

	override operator fun <T : Enum<T>> get(preference: Preference<T>): T {
		val stringValue = cachedPreferences[preference.key]

		return if (stringValue.isNullOrBlank()) preference.defaultValue
		else preference.type.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: preference.defaultValue
	}

	override operator fun <T : Enum<T>> set(preference: Preference<T>, value: T) {
		cachedPreferences[preference.key] = when (value) {
			is PreferenceEnum -> value.serializedName
			else -> value.toString()
		}
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
