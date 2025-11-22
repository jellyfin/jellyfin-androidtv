package org.jellyfin.androidtv.data.repository

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.model.AudioSubtitlePreferences
import org.jellyfin.androidtv.util.sdk.UserConfigurationUpdateException
import org.jellyfin.androidtv.util.sdk.UserConfigurationUpdater
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode
import org.jellyfin.sdk.model.api.UserDto
import timber.log.Timber

class AudioSubtitlePreferencesRepositoryImpl(
    private val api: ApiClient,
    private val userRepository: UserRepository,
    private val userConfigurationUpdater: UserConfigurationUpdater,
) : AudioSubtitlePreferencesRepository {

    companion object {
        private const val TAG = "AudioSubtitlePrefsRepo"
    }

    private val _preferences = MutableStateFlow(AudioSubtitlePreferences())
    override val preferences: StateFlow<AudioSubtitlePreferences> = _preferences.asStateFlow()

    private val scope = ProcessLifecycleOwner.get().lifecycleScope

    // Mutex to prevent race conditions when updating configuration
    private val updateMutex = Mutex()

    init {
        // Load initial preferences synchronously from current user
        val currentUser = userRepository.currentUser.value
        if (currentUser != null) {
            refreshFromUser(currentUser)
            Timber.tag(TAG).d("Loaded initial audio/subtitle preferences from current user")
        }

        // Subscribe to user changes to automatically refresh preferences
        userRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    refreshFromUser(user)
                } else {
                    // User logged out, reset to defaults
                    _preferences.value = AudioSubtitlePreferences()
                }
            }
            .launchIn(scope)
    }

    override suspend fun refreshPreferences() {
        val user = userRepository.currentUser.value ?: run {
            Timber.tag(TAG).w("Cannot refresh preferences: no user logged in")
            return
        }

        try {
            val refreshedUser by api.userApi.getCurrentUser()
            refreshFromUser(refreshedUser)
        } catch (e: ApiClientException) {
            Timber.tag(TAG).e(e, "Failed to refresh audio/subtitle preferences from server")
        }
    }

    override suspend fun updateAudioLanguage(language: String?) = withContext(Dispatchers.IO) {
        // Update local state immediately for UI responsiveness (synchronous)
        _preferences.value = _preferences.value.copy(audioLanguagePreference = language)

        // Then update server in background
        try {
            updateConfiguration { config ->
                config.copy(audioLanguagePreference = language)
            }
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).e(e, "Failed to update audio language on server")
            // Don't rethrow - error is already logged
        } catch (e: UserConfigurationUpdateException) {
            Timber.tag(TAG).e(e, "Failed to update audio language on server")
            // Don't rethrow - error is already logged
        }
    }

    override suspend fun updateSubtitleLanguage(language: String?) = withContext(Dispatchers.IO) {
        // Update local state immediately for UI responsiveness (synchronous)
        _preferences.value = _preferences.value.copy(subtitleLanguagePreference = language)

        // Then update server in background
        try {
            updateConfiguration { config ->
                config.copy(subtitleLanguagePreference = language)
            }
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).e(e, "Failed to update subtitle language on server")
            // Don't rethrow - error is already logged
        } catch (e: UserConfigurationUpdateException) {
            Timber.tag(TAG).e(e, "Failed to update subtitle language on server")
            // Don't rethrow - error is already logged
        }
    }

    override suspend fun updateSubtitleMode(mode: SubtitlePlaybackMode) = withContext(Dispatchers.IO) {
        // Update local state immediately for UI responsiveness (synchronous)
        _preferences.value = _preferences.value.copy(subtitleMode = mode)

        // Then update server in background
        try {
            updateConfiguration { config ->
                config.copy(subtitleMode = mode)
            }
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).e(e, "Failed to update subtitle mode on server")
            // Don't rethrow - error is already logged
        } catch (e: UserConfigurationUpdateException) {
            Timber.tag(TAG).e(e, "Failed to update subtitle mode on server")
            // Don't rethrow - error is already logged
        }
    }

    /**
     * Extract preferences from a UserDto and update the local state.
     */
    private fun refreshFromUser(user: UserDto) {
        val config = user.configuration
        _preferences.value = AudioSubtitlePreferences(
            audioLanguagePreference = config?.audioLanguagePreference,
            subtitleLanguagePreference = config?.subtitleLanguagePreference,
            subtitleMode = config?.subtitleMode ?: SubtitlePlaybackMode.DEFAULT,
        )

        Timber.tag(TAG).d(
            "Loaded audio/subtitle preferences: audio=%s, subtitle=%s, mode=%s",
            config?.audioLanguagePreference,
            config?.subtitleLanguagePreference,
            config?.subtitleMode
        )
    }

    /**
     * Update the user configuration on the server with a new configuration.
     * Uses the current user configuration from the server as base.
     * Protected by mutex to prevent race conditions.
     */
    private suspend fun updateConfiguration(
        transform: (org.jellyfin.sdk.model.api.UserConfiguration) -> org.jellyfin.sdk.model.api.UserConfiguration
    ) = withContext(Dispatchers.IO) {
        updateMutex.withLock {
            val currentUser = userRepository.currentUser.value ?: run {
                Timber.tag(TAG).w("Cannot update configuration: no user logged in")
                error("No user logged in")
            }

            val serverConfig = currentUser.configuration ?: run {
                Timber.tag(TAG).w("Cannot update configuration: user has no configuration")
                error("User configuration not available")
            }

            // Apply transform directly to server config
            // The transform contains the specific change requested
            val updatedConfig = transform(serverConfig)

            try {
                userConfigurationUpdater.updateUserConfiguration(api, currentUser.id, updatedConfig)
                Timber.tag(TAG).d("Successfully updated user configuration on server")

                // Note: We don't refresh immediately after update because:
                // 1. The optimistic local update already shows the correct value
                // 2. Refreshing too quickly can overwrite the optimistic update with stale server data
                // 3. The user change subscription (init block) will refresh when needed
            } catch (e: UserConfigurationUpdateException) {
                Timber.tag(TAG).e(e, "Failed to update user configuration on server")
                // Rethrow so caller can handle (will be caught in update methods above)
                throw e
            } catch (e: IllegalStateException) {
                Timber.tag(TAG).e(e, "Failed to update user configuration on server")
                // Rethrow so caller can handle (will be caught in update methods above)
                throw e
            }
        }
    }
}
