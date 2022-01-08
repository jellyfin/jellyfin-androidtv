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
			Timber.e(
				err,
				"Unable to save displaypreferences. (displayPreferencesId=$displayPreferencesId, app=$app)"
			)
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
			Timber.e(
				err,
				"Unable to retrieve displaypreferences. (displayPreferencesId=$displayPreferencesId, app=$app)"
			)

			if (displayPreferencesDto == null) {
				Timber.i("Creating an empty DisplayPreferencesDto for next commit.")
				displayPreferencesDto = DisplayPreferencesDto.empty()
			}

			return false
		}
	}

	@Suppress("UNCHECKED_CAST")
	override operator fun <T : Any> get(preference: Preference<T>): T =
		when (preference.defaultValue) {
			is PreferenceVal.IntT -> cachedPreferences[preference.key]?.toIntOrNull()
				?: preference.defaultValue.data

			is PreferenceVal.LongT ->
				cachedPreferences[preference.key]?.toLongOrNull()
					?: preference.defaultValue.data

			is PreferenceVal.BoolT ->
				cachedPreferences[preference.key]?.toBooleanStrictOrNull()
					?: preference.defaultValue.data

			is PreferenceVal.StringT ->
				cachedPreferences[preference.key] ?: preference.defaultValue.data

			is PreferenceVal.EnumT<*> -> getEnum(preference, preference.defaultValue)
		} as T

	private fun <T> getEnum(
		preference: Preference<*>,
		// Require an EnumT param so someone can't call this with the wrong T type
		defaultValue: PreferenceVal.EnumT<*>
	): T {
		val stringValue = cachedPreferences[preference.key]

		if (stringValue.isNullOrBlank()) {
			@Suppress("UNCHECKED_CAST")
			return defaultValue.data as T
		}

		val loadedVal = defaultValue.enumClass.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: defaultValue.data

		@Suppress("UNCHECKED_CAST")
		return loadedVal as T
	}

	override operator fun set(preference: Preference<*>, value: PreferenceVal<*>) {
		when (value) {
			is PreferenceVal.IntT -> cachedPreferences[preference.key] = (value.data).toString()
			is PreferenceVal.LongT -> cachedPreferences[preference.key] = (value.data).toString()
			is PreferenceVal.BoolT -> cachedPreferences[preference.key] = (value.data).toString()
			is PreferenceVal.StringT -> cachedPreferences[preference.key] = (value.data)
			is PreferenceVal.EnumT<*> -> setEnum(preference, value.data)
		}

	}

	private fun <V : Enum<V>> setEnum(preference: Preference<*>, value: Enum<V>) {
		cachedPreferences[preference.key] = when (value) {
			is PreferenceEnum -> value.serializedName
			else -> value.toString()
		}
	}

	override fun delete(preference: Preference<*>) {
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
