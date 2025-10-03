package org.jellyfin.androidtv.util.profile

import android.content.Context
import android.content.res.Resources
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.profile.model.*
import org.jellyfin.sdk.model.api.CodecProfile
import org.jellyfin.sdk.model.api.ContainerProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.TranscodingProfile
import timber.log.Timber
import java.io.IOException

class DeviceProfileOverrides (
	private val context: Context
) {
	private val jsonParser = Json { ignoreUnknownKeys = true }

	private val overrides: List<Profiles> by lazy {
		try {
			val resource = context.resources.openRawResource(R.raw.profile_overrides)
			val json = resource.use { stream -> stream.bufferedReader().use { it.readText() } }
			val decoded = jsonParser.decodeFromString<List<OverrideRule>>(json)

			val loadedOverrides = decoded
				.filter { it.devices.any { device -> device.matchesCurrentDevice() } }
				.map { it.profiles }

			Timber.d("Loaded device profile overrides: %s", loadedOverrides.joinToString())

			loadedOverrides
		} catch (e: Exception) {
			when (e) {
				is Resources.NotFoundException ->
					Timber.e(e, "Device profile overrides resource not found.")
				is IOException ->
					Timber.e(e, "Failed to read device profile overrides resource.")
				is SerializationException ->
					Timber.e(e, "Failed to decode device profile overrides JSON.")
				else ->
					Timber.e(e, "An unexpected error occurred while loading device profile overrides.")
			}
			emptyList()
		}
	}

	fun getTranscodingOverrides(): List<TranscodingProfile> =
		getProfileOverrides { it.transcodingProfiles }

	fun getDirectPlayOverrides(): List<DirectPlayProfile> =
		getProfileOverrides { it.directPlayProfiles }

	fun getContainerOverrides(): List<ContainerProfile> =
		getProfileOverrides { it.containerProfiles }

	fun getCodecOverrides(): List<CodecProfile> =
		getProfileOverrides { it.codecProfiles }

	private inline fun <T> getProfileOverrides(selector: (Profiles) -> List<T>?): List<T> {
		return overrides.flatMap { selector(it) ?: emptyList() }
	}
}
