package org.jellyfin.androidtv.data.repository

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.localizationApi
import org.jellyfin.sdk.model.api.CultureDto
import timber.log.Timber

/**
 * Repository for caching language list from Jellyfin server.
 * Cached across app lifecycle but cleared on user logout.
 */
class LanguageCacheRepository(
    private val api: ApiClient,
    private val userRepository: UserRepository
) {
    companion object {
        private const val TAG = "LanguageCacheRepository"
    }

    private val _languages = MutableStateFlow<Map<String, String>>(emptyMap())
    val languages: StateFlow<Map<String, String>> = _languages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val scope = ProcessLifecycleOwner.get().lifecycleScope

    init {
        // Clear cache when user logs out
        userRepository.currentUser
            .onEach { user ->
                if (user == null) clearCache()
            }
            .launchIn(scope)
    }

    suspend fun loadLanguages(): Result<Map<String, String>> {
        if (_languages.value.isNotEmpty()) {
            Timber.tag(TAG).d("Using cached languages (${_languages.value.size} entries)")
            return Result.success(_languages.value)
        }

        if (_isLoading.value) {
            Timber.tag(TAG).d("Language loading already in progress")
            return Result.success(emptyMap())
        }

        return withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val languageMap = mutableMapOf<String, String>()
                val cultures = api.localizationApi.getCultures().content

                cultures.forEach { culture: CultureDto ->
                    // Use only the first code (ISO 639-2/T) to avoid duplicates
                    val primaryCode = culture.threeLetterIsoLanguageNames.firstOrNull()
                    if (primaryCode != null) {
                        languageMap[primaryCode] = culture.displayName
                    }
                }

                _languages.value = languageMap
                Timber.tag(TAG).d("Loaded ${languageMap.size} languages from server")
                Result.success(languageMap)
            } catch (e: ApiClientException) {
                Timber.tag(TAG).e(e, "Failed to load languages from server")
                Result.failure(e)
            } catch (e: IllegalStateException) {
                Timber.tag(TAG).e(e, "Failed to load languages from server")
                Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCache() {
        Timber.tag(TAG).d("Clearing language cache")
        _languages.value = emptyMap()
    }
}
