package org.jellyfin.androidtv.util

import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.LiveTvOption
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.displayPreferencesApi
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.UUID

/**
 * Helper class to handle Live TV default view preferences
 */
object LiveTvDefaultViewHelper {
    /**
     * Gets the user's preferred default Live TV screen from server settings
     *
     * @param api The API client to use for the request
     * @return The LiveTvOption ID to navigate to, or null if we should show the default selection screen
     */
    suspend fun getDefaultLiveTvView(api: ApiClient): Int? {
        try {
            // Get the current user ID from the UserRepository
            val userRepository = KoinJavaComponent.get<UserRepository>(UserRepository::class.java)
            val userId = userRepository.currentUser.value?.id

            if (userId == null) {
                return null
            }

            var displayPreferences = api.displayPreferencesApi.getDisplayPreferences(
                displayPreferencesId = "usersettings",
                userId = userId,
                client = "emby"
            ).content

            // Check for the "landing-livetv" preference
            var defaultView = displayPreferences?.customPrefs?.get("landing-livetv")

            // Map the server preference to our LiveTvOption constants
            val result = when (defaultView?.lowercase()) {
                "guide" -> LiveTvOption.LIVE_TV_GUIDE_OPTION_ID
                "recordings" -> LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID
                "schedule" -> LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID
                "seriestimers" -> LiveTvOption.LIVE_TV_SERIES_OPTION_ID
                else -> null
            }

            return result
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Java-friendly wrapper for getDefaultLiveTvView
     * This method blocks the current thread until the result is available
     */
    @JvmStatic
    fun getDefaultLiveTvViewBlocking(api: ApiClient): Int? {
        return kotlinx.coroutines.runBlocking {
            getDefaultLiveTvView(api)
        }
    }
}
