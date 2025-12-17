package org.jellyfin.androidtv.util.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.model.api.UserConfiguration
import timber.log.Timber
import java.util.UUID

/**
 * Helper class to update user configuration on the Jellyfin server.
 * This functionality is not exposed in the SDK, so we use OkHttp directly.
 */
class UserConfigurationUpdater(
	private val okHttpFactory: OkHttpFactory,
	private val httpClientOptions: org.jellyfin.sdk.api.client.HttpClientOptions,
) {
	companion object {
		private const val TAG = "UserConfigUpdater"
		private const val HTTP_UNAUTHORIZED = 401
		private const val HTTP_FORBIDDEN = 403
		private const val HTTP_NOT_FOUND = 404
		private const val HTTP_SERVER_ERROR = 500
	}

	private val json = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
	}

	suspend fun updateUserConfiguration(
		apiClient: ApiClient,
		userId: UUID,
		configuration: UserConfiguration
	) = withContext(Dispatchers.IO) {
		try {
			val url = apiClient.createUrl("/Users/$userId/Configuration")
			val jsonBody = json.encodeToString(configuration)

			Timber.tag(TAG).d("Updating user configuration: POST $url")

			val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
			val authHeader = AuthorizationHeaderBuilder.buildHeader(
				apiClient.clientInfo.name,
				apiClient.clientInfo.version,
				apiClient.deviceInfo.id,
				apiClient.deviceInfo.name,
				apiClient.accessToken
			)

			val request = okhttp3.Request.Builder()
				.url(url)
				.post(requestBody)
				.addHeader("Authorization", authHeader)
				.build()

			val httpClient = okHttpFactory.createClient(httpClientOptions)

			val response = try {
				httpClient.newCall(request).execute()
			} catch (e: java.io.IOException) {
				Timber.tag(TAG).e(e, "Network error updating user configuration")
				throw UserConfigurationUpdateException("Network error: ${e.message}", e)
			}

			response.use { resp ->
				if (!resp.isSuccessful) {
					val errorBody = resp.body?.string()
					val errorMessage = "Failed to update user configuration: ${resp.code}"
					Timber.tag(TAG).e("$errorMessage - $errorBody")

					val exceptionMessage = when (resp.code) {
						HTTP_UNAUTHORIZED -> "Unauthorized - please re-login"
						HTTP_FORBIDDEN -> "Forbidden - insufficient permissions"
						HTTP_NOT_FOUND -> "User not found"
						HTTP_SERVER_ERROR -> "Server error"
						else -> errorMessage
					}
					throw UserConfigurationUpdateException(exceptionMessage)
				}
				Timber.tag(TAG).d("Successfully updated user configuration")
			}
		} catch (e: UserConfigurationUpdateException) {
			throw e // Re-throw our custom exception
		} catch (e: java.io.IOException) {
			Timber.tag(TAG).e(e, "IO error in updateUserConfiguration")
			throw UserConfigurationUpdateException("Failed to update configuration", e)
		} catch (e: kotlinx.serialization.SerializationException) {
			Timber.tag(TAG).e(e, "Serialization error in updateUserConfiguration")
			throw UserConfigurationUpdateException("Failed to serialize configuration", e)
		} catch (e: IllegalArgumentException) {
			Timber.tag(TAG).e(e, "Invalid argument in updateUserConfiguration")
			throw UserConfigurationUpdateException("Invalid configuration data", e)
		}
	}
}

/**
 * Exception thrown when user configuration update fails.
 */
class UserConfigurationUpdateException(
	message: String,
	cause: Throwable? = null
) : Exception(message, cause)
