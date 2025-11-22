package org.jellyfin.androidtv.ui.preference.screen

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.shortcut
import org.jellyfin.androidtv.ui.preference.dsl.text
import org.jellyfin.androidtv.util.getQuantityString
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CustomizationPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	private val apiClient: ApiClient by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_customization)

		category {
			setTitle(R.string.pref_theme)

			enum<AppTheme> {
				setTitle(R.string.pref_app_theme)
				bind(userPreferences, UserPreferences.appTheme)
			}

			enum<ClockBehavior> {
				setTitle(R.string.pref_clock_display)
				bind(userPreferences, UserPreferences.clockBehavior)
			}

			enum<WatchedIndicatorBehavior> {
				setTitle(R.string.pref_watched_indicator)
				bind(userPreferences, UserPreferences.watchedIndicatorBehavior)
			}

			checkbox {
				setTitle(R.string.lbl_show_backdrop)
				setContent(R.string.pref_show_backdrop_description)
				bind(userPreferences, UserPreferences.backdropEnabled)
			}

			checkbox {
				setTitle(R.string.lbl_use_series_thumbnails)
				setContent(R.string.lbl_use_series_thumbnails_description)
				bind(userPreferences, UserPreferences.seriesThumbnailsEnabled)
			}

			enum<RatingType> {
				setTitle(R.string.pref_default_rating)
				bind(userPreferences, UserPreferences.defaultRatingType)
			}

			checkbox {
				setTitle(R.string.lbl_show_premieres)
				setContent(R.string.desc_premieres)
				bind(userPreferences, UserPreferences.premieresEnabled)
			}

			checkbox {
				setTitle(R.string.pref_enable_media_management)
				setContent(R.string.pref_enable_media_management_description)
				bind(userPreferences, UserPreferences.mediaManagementEnabled)
			}
		}

		category {
			setTitle(R.string.jellyseerr_pref)

			text {
				setTitle(R.string.jellyseerr_pref_url)
				preferenceKey = UserPreferences.jellyseerrUrl.key
			}

			action {
				setTitle(R.string.jellyseerr_pref_fetch_api_key)

				onActivate = {
					val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
					val url = sharedPreferences.getString(UserPreferences.jellyseerrUrl.key, "").orEmpty().trim()
					val serverBase = apiClient.baseUrl?.trimEnd('/')
					val accessToken = apiClient.accessToken?.takeIf { it.isNotBlank() }

					when {
						url.isBlank() -> Toast.makeText(
							context,
							R.string.jellyseerr_pref_fetch_api_key_missing,
							Toast.LENGTH_LONG,
						).show()
						serverBase.isNullOrBlank() || accessToken == null -> Toast.makeText(
							context,
							R.string.jellyseerr_pref_fetch_api_key_not_authenticated,
							Toast.LENGTH_LONG,
						).show()
						else -> lifecycleScope.launch {
							val result = withContext(Dispatchers.IO) {
								runCatching {
									val authorization = AuthorizationHeaderBuilder.buildHeader(
										apiClient.clientInfo.name,
										apiClient.clientInfo.version,
										apiClient.deviceInfo.id,
										apiClient.deviceInfo.name,
										accessToken,
									)

									val request = Request.Builder()
										.url("$serverBase/plugins/requests/apikey")
										.header("Authorization", authorization)
										.build()

									OkHttpClient().newCall(request).execute().use { response ->
										if (!response.isSuccessful) {
											throw IllegalStateException("HTTP ${response.code}")
										}
										response.body?.string() ?: throw IllegalStateException("Empty response")
									}
								}
							}

							result
								.onSuccess { body ->
									val trimmed = body.trimStart()
									val normalized = trimmed.removePrefix("\uFEFF")
									if (!normalized.startsWith("{")) {
										Toast.makeText(
											context,
											R.string.jellyseerr_pref_fetch_api_key_plugin_disabled,
											Toast.LENGTH_LONG,
										).show()
										return@onSuccess
									}

									val data = runCatching { JSONObject(normalized) }.getOrNull()
									if (data == null) {
										Toast.makeText(
											context,
											R.string.jellyseerr_pref_fetch_api_key_plugin_disabled,
											Toast.LENGTH_LONG,
										).show()
										return@onSuccess
									}

									val success = data.optBoolean("success", false)
									val key = data.optString("apiKey")
									val message = data.optString("message")

									if (success && key.isNotBlank()) {
										userPreferences[UserPreferences.jellyseerrApiKey] = key
										preferenceScreen
											?.findPreference<EditTextPreference>(UserPreferences.jellyseerrApiKey.key)
											?.text = key
										Toast.makeText(
											context,
											R.string.jellyseerr_pref_fetch_api_key_success,
											Toast.LENGTH_LONG,
										).show()
									} else {
										Toast.makeText(
											context,
											getString(
												R.string.jellyseerr_pref_fetch_api_key_failure,
												message.ifBlank { context.getString(R.string.jellyseerr_pref_fetch_api_key_plugin_disabled) },
											),
											Toast.LENGTH_LONG,
										).show()
									}
								}
								.onFailure { error ->
									Toast.makeText(
										context,
										getString(
											R.string.jellyseerr_pref_fetch_api_key_failure,
											error.message ?: context.getString(R.string.jellyseerr_pref_fetch_api_key_unknown_error),
										),
										Toast.LENGTH_LONG,
									).show()
								}
						}
					}
				}
			}

			text {
				setTitle(R.string.jellyseerr_pref_api_key)
				preferenceKey = UserPreferences.jellyseerrApiKey.key
			}

			action {
				setTitle(R.string.jellyseerr_pref_test_connection)

				onActivate = {
					val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
					val url = sharedPreferences.getString(UserPreferences.jellyseerrUrl.key, "").orEmpty().trim()
					val apiKey = sharedPreferences.getString(UserPreferences.jellyseerrApiKey.key, "").orEmpty().trim()

					if (url.isBlank() || apiKey.isBlank()) {
						Toast.makeText(
							context,
							R.string.jellyseerr_pref_test_connection_missing,
							Toast.LENGTH_LONG,
						).show()
					} else {
						lifecycleScope.launch {
							val result = withContext(Dispatchers.IO) {
								runCatching {
									val baseUrl = url.trimEnd('/')
									val request = Request.Builder()
										.url("$baseUrl/api/v1/auth/me")
										.header("X-API-Key", apiKey)
										.build()

									OkHttpClient().newCall(request).execute().use { response ->
										if (!response.isSuccessful) {
											throw IllegalStateException("HTTP ${response.code}")
										}
									}
								}
							}

							result
								.onSuccess {
									Toast.makeText(
										context,
										R.string.jellyseerr_pref_test_connection_success,
										Toast.LENGTH_LONG,
									).show()
								}
								.onFailure { error ->
									Toast.makeText(
										context,
										getString(
											R.string.jellyseerr_pref_test_connection_failure,
											error.message ?: "Unknown error",
										),
										Toast.LENGTH_LONG,
									).show()
								}
						}
					}
				}
			}
		}

		category {
			setTitle(R.string.pref_browsing)

			link {
				setTitle(R.string.home_prefs)
				setContent(R.string.pref_home_description)
				icon = R.drawable.ic_house
				withFragment<HomePreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_libraries)
				setContent(R.string.pref_libraries_description)
				icon = R.drawable.ic_grid
				withFragment<LibrariesPreferencesScreen>()
			}
		}

		category {
			setTitle(R.string.pref_screensaver)

			checkbox {
				setTitle(R.string.pref_screensaver_inapp_enabled)
				setContent(R.string.pref_screensaver_inapp_enabled_description)
				bind(userPreferences, UserPreferences.screensaverInAppEnabled)
			}

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.pref_screensaver_inapp_timeout)

				entries = mapOf(
					30.seconds to context.getQuantityString(R.plurals.seconds, 30),
					1.minutes to context.getQuantityString(R.plurals.minutes, 1),
					2.5.minutes to context.getQuantityString(R.plurals.minutes, 2.5),
					5.minutes to context.getQuantityString(R.plurals.minutes, 5),
					10.minutes to context.getQuantityString(R.plurals.minutes, 10),
					15.minutes to context.getQuantityString(R.plurals.minutes, 15),
					30.minutes to context.getQuantityString(R.plurals.minutes, 30),
				).mapKeys { it.key.inWholeMilliseconds.toString() }

				bind {
					get { userPreferences[UserPreferences.screensaverInAppTimeout].toString() }
					set { value -> userPreferences[UserPreferences.screensaverInAppTimeout] = value.toLong() }
					default { UserPreferences.screensaverInAppTimeout.defaultValue.toString() }
				}

				depends { userPreferences[UserPreferences.screensaverInAppEnabled] }
			}

			checkbox {
				setTitle(R.string.pref_screensaver_ageratingrequired_title)
				setContent(
					R.string.pref_screensaver_ageratingrequired_enabled,
					R.string.pref_screensaver_ageratingrequired_disabled,
				)

				bind(userPreferences, UserPreferences.screensaverAgeRatingRequired)
			}

			list {
				setTitle(R.string.pref_screensaver_ageratingmax)

				// Note: Must include 13 (default value)
				// We may want to fetch this mapping from the server in the future
				@Suppress("MagicNumber")
				val ages = setOf(5, 10, 13, 14, 16, 18, 21)

				entries = buildMap {
					put("0", getString(R.string.pref_screensaver_ageratingmax_zero))
					ages.forEach { age -> put(age.toString(), getString(R.string.pref_screensaver_ageratingmax_entry, age)) }
					put("-1", getString(R.string.pref_screensaver_ageratingmax_unlimited))
				}

				bind {
					get { userPreferences[UserPreferences.screensaverAgeRatingMax].toString() }
					set { value -> userPreferences[UserPreferences.screensaverAgeRatingMax] = value.toInt() }
					default { UserPreferences.screensaverAgeRatingMax.defaultValue.toString() }
				}
			}
		}

		category {
			setTitle(R.string.pref_behavior)

			shortcut {
				setTitle(R.string.pref_audio_track_button)
				bind(userPreferences, UserPreferences.shortcutAudioTrack)
			}

			shortcut {
				setTitle(R.string.pref_subtitle_track_button)
				bind(userPreferences, UserPreferences.shortcutSubtitleTrack)
			}
		}
	}
}
