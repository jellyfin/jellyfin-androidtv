package org.jellyfin.androidtv.preferences.ui

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.LogonCredentials
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.enums.*
import org.jellyfin.androidtv.preferences.ui.dsl.*
import org.jellyfin.androidtv.preferences.ui.preference.EditLongPreference
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper
import timber.log.Timber
import java.io.IOException

class UserPreferencesFragment : LeanbackSettingsFragmentCompat() {
	override fun onPreferenceStartInitialScreen() {
		startPreferenceFragment(InnerUserPreferencesFragment())
	}

	override fun onPreferenceDisplayDialog(caller: PreferenceFragmentCompat, pref: Preference?): Boolean {
		if (pref is ButtonRemapPreference) {
			val fragment = ButtonRemapDialogFragment.newInstance(pref.key).apply {
				setTargetFragment(caller, 0)
			}
			startPreferenceFragment(fragment)

			return true
		}

		return super.onPreferenceDisplayDialog(caller, pref)
	}

	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val fragment = childFragmentManager.fragmentFactory.instantiate(requireActivity().classLoader, pref.fragment).apply {
			setTargetFragment(caller, 0)
			arguments = pref.extras
		}

		val isImmersive = fragment !is PreferenceFragmentCompat && fragment !is PreferenceDialogFragmentCompat

		if (isImmersive) startImmersiveFragment(fragment)
		else startPreferenceFragment(fragment)

		return true
	}

	override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
		val fragment = InnerUserPreferencesFragment().apply {
			arguments = Bundle(1).apply {
				putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
			}
		}

		startPreferenceFragment(fragment)
		return true
	}

	class InnerUserPreferencesFragment : LeanbackPreferenceFragmentCompat() {
		private fun PreferenceScreen.authenticationCategory(
			userPreferences: UserPreferences
		) = category(R.string.pref_authentication_cat) {
			enumPreference<LoginBehavior>(R.string.pref_login_behavior_title) {
				set {
					if (it == LoginBehavior.AUTO_LOGIN) {
						try {
							val credentials = LogonCredentials(TvApp.getApplication().apiClient.serverInfo, TvApp.getApplication().currentUser)
							AuthenticationHelper.saveLoginCredentials(credentials, TvApp.CREDENTIALS_PATH)
						} catch (e: IOException) {
							Timber.e(e, "Unable to save logon credentials")
						}
					}

					userPreferences[UserPreferences.loginBehavior] = it
				}
				get { userPreferences[UserPreferences.loginBehavior] }
				visible {
					val configuredAutoCredentials = TvApp.getApplication().configuredAutoCredentials

					// Auto login is disabled
					userPreferences[UserPreferences.loginBehavior] != LoginBehavior.AUTO_LOGIN
						// Or configured user is set to current user
						|| configuredAutoCredentials.userDto.id == TvApp.getApplication().currentUser.id
				}
			}
			checkboxPreference(R.string.pref_prompt_pw) {
				set { userPreferences[UserPreferences.passwordPromptEnabled] = it }
				get { userPreferences[UserPreferences.passwordPromptEnabled] }
				visible {
					val configuredAutoCredentials = TvApp.getApplication().configuredAutoCredentials

					// Auto login is enabled
					userPreferences[UserPreferences.loginBehavior] == LoginBehavior.AUTO_LOGIN
						// Configured user is set to current user
						&& configuredAutoCredentials.userDto.id == TvApp.getApplication().currentUser.id
						// Configured user contains a password
						&& configuredAutoCredentials.userDto.hasPassword
				}
			}
			checkboxPreference(R.string.pref_alt_pw_entry, R.string.pref_alt_pw_entry_desc) {
				set { userPreferences[UserPreferences.passwordDPadEnabled] = it }
				get { userPreferences[UserPreferences.passwordDPadEnabled] }
			}
			checkboxPreference(R.string.pref_live_tv_mode, R.string.pref_live_tv_mode_desc) {
				set { userPreferences[UserPreferences.liveTvMode] = it }
				get { userPreferences[UserPreferences.liveTvMode] }
			}
		}

		private fun PreferenceScreen.generalCategory(
			userPreferences: UserPreferences
		) = category(R.string.pref_general) {
			enumPreference<AppTheme>(R.string.pref_app_theme) {
				set { userPreferences[UserPreferences.appTheme] = it }
				get { userPreferences[UserPreferences.appTheme] }
			}
			checkboxPreference(R.string.lbl_show_backdrop) {
				set { userPreferences[UserPreferences.backdropEnabled] = it }
				get { userPreferences[UserPreferences.backdropEnabled] }
			}
			checkboxPreference(R.string.lbl_show_premieres, R.string.desc_premieres) {
				set { userPreferences[UserPreferences.premieresEnabled] = it }
				get { userPreferences[UserPreferences.premieresEnabled] }
			}
			enumPreference<GridDirection>(R.string.grid_direction) {
				set { userPreferences[UserPreferences.gridDirection] = it }
				get { userPreferences[UserPreferences.gridDirection] }
			}
			checkboxPreference(R.string.lbl_enable_seasonal_themes, R.string.desc_seasonal_themes) {
				set { userPreferences[UserPreferences.seasonalGreetingsEnabled] = it }
				get { userPreferences[UserPreferences.seasonalGreetingsEnabled] }
			}
			checkboxPreference(R.string.lbl_enable_debug, R.string.desc_debug) {
				set { userPreferences[UserPreferences.debuggingEnabled] = it }
				get { userPreferences[UserPreferences.debuggingEnabled] }
			}
		}

		private fun PreferenceScreen.playbackCategory(
			userPreferences: UserPreferences
		) = category(R.string.pref_playback) {
			val maxBitrateValues = mapOf(
				"0" to "Auto",
				"120" to "120 Mbits/sec",
				"110" to "110 Mbits/sec",
				"100" to "100 Mbits/sec",
				"90" to "90 Mbits/sec",
				"80" to "80 Mbits/sec",
				"70" to "70 Mbits/sec",
				"60" to "60 Mbits/sec",
				"50" to "50 Mbits/sec",
				"40" to "40 Mbits/sec",
				"30" to "30 Mbits/sec",
				"21" to "21 Mbits/sec",
				"15" to "15 Mbits/sec",
				"10" to "10 Mbits/sec",
				"5" to "5 Mbits/sec",
				"3" to "3 Mbits/sec",
				"2" to "2 Mbits/sec",
				"1.5" to "1.5 Mbits/sec",
				"1" to "1 Mbit/sec",
				"0.72" to "720 Kbits/sec",
				"0.42" to "420 Kbits/sec"
			)

			listPreference(R.string.pref_max_bitrate_title, maxBitrateValues) {
				get { userPreferences[UserPreferences.maxBitrate] }
				set { userPreferences[UserPreferences.maxBitrate] = it }
			}

			checkboxPreference(R.string.lbl_tv_queuing, R.string.sum_tv_queuing) {
				get { userPreferences[UserPreferences.mediaQueuingEnabled] }
				set { userPreferences[UserPreferences.mediaQueuingEnabled] = it }
			}

			checkboxPreference(R.string.pref_next_up_enabled_title, R.string.pref_next_up_enabled_summary) {
				get { userPreferences[UserPreferences.nextUpEnabled] }
				set { userPreferences[UserPreferences.nextUpEnabled] = it }
				enabled { userPreferences[UserPreferences.mediaQueuingEnabled] }
			}

			seekbarPreference(R.string.pref_next_up_timeout_title, R.string.pref_next_up_timeout_summary, min = 3000, max = 30000, increment = 1000) {
				get { userPreferences[UserPreferences.nextUpTimeout] }
				set { userPreferences[UserPreferences.nextUpTimeout] = it }
				enabled { userPreferences[UserPreferences.mediaQueuingEnabled] && userPreferences[UserPreferences.nextUpEnabled] }
			}

			val prerollValues = mapOf(
				"0" to getString(R.string.lbl_none),
				"5" to "5 seconds",
				"10" to "10 seconds",
				"20" to "20 seconds",
				"30" to "30 seconds",
				"60" to "1 minute",
				"120" to "2 minutes",
				"300" to "5 minutes"
			)
			listPreference(R.string.lbl_resume_preroll, prerollValues) {
				get { userPreferences[UserPreferences.resumeSubtractDuration] }
				set { userPreferences[UserPreferences.resumeSubtractDuration] = it }
			}

			enumPreference<PreferredVideoPlayer>(R.string.pref_media_player) {
				get { userPreferences[UserPreferences.videoPlayer] }
				set { userPreferences[UserPreferences.videoPlayer] = it }
			}

			checkboxPreference(R.string.lbl_enable_cinema_mode, R.string.sum_enable_cinema_mode) {
				get { userPreferences[UserPreferences.cinemaModeEnabled] }
				set { userPreferences[UserPreferences.cinemaModeEnabled] = it }
				enabled { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}

			enumPreference<AudioBehavior>(R.string.lbl_audio_output) {
				get { userPreferences[UserPreferences.audioBehaviour] }
				set { userPreferences[UserPreferences.audioBehaviour] = it }
				visible { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL && !DeviceUtils.isFireTv() && DeviceUtils.is50() }
			}

			checkboxPreference(R.string.lbl_bitstream_ac3, R.string.desc_bitstream_ac3) {
				get { userPreferences[UserPreferences.ac3Enabled] }
				set { userPreferences[UserPreferences.ac3Enabled] = it }
				visible { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL && !DeviceUtils.is60() }
			}

			checkboxPreference(R.string.lbl_bitstream_dts, R.string.desc_bitstream_ac3) {
				get { userPreferences[UserPreferences.dtsEnabled] }
				set { userPreferences[UserPreferences.dtsEnabled] = it }
				enabled { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}

			checkboxPreference(R.string.lbl_refresh_switching) {
				get { userPreferences[UserPreferences.refreshRateSwitchingEnabled] }
				set { userPreferences[UserPreferences.refreshRateSwitchingEnabled] = it }
				visible { DeviceUtils.is60() }
				enabled { userPreferences[UserPreferences.videoPlayer] != PreferredVideoPlayer.EXTERNAL }
			}

			//TODO Add summary
			//TODO Set inputType to number only
			longPreference(R.string.pref_libvlc_audio_delay_title) {
				get { userPreferences[UserPreferences.libVLCAudioDelay] }
				set { userPreferences[UserPreferences.libVLCAudioDelay] = it }
			}

			checkboxPreference(R.string.pref_use_direct_path_title, R.string.pref_use_direct_path_summary) {
				get { userPreferences[UserPreferences.externalVideoPlayerSendPath] }
				set {
					if (it) {
						AlertDialog.Builder(activity)
							.setTitle(getString(R.string.lbl_warning))
							.setMessage(getString(R.string.msg_external_path))
							.setPositiveButton(R.string.btn_got_it, null)
							.show()
					}

					userPreferences[UserPreferences.externalVideoPlayerSendPath] = it
				}
				enabled { userPreferences[UserPreferences.videoPlayer] == PreferredVideoPlayer.EXTERNAL }
			}
		}

		private fun PreferenceScreen.liveTvCategory(
			userPreferences: UserPreferences
		) = category(R.string.pref_live_tv_cat) {
			enumPreference<PreferredVideoPlayer>(R.string.pref_media_player) {
				get { userPreferences[UserPreferences.liveTvVideoPlayer] }
				set { userPreferences[UserPreferences.liveTvVideoPlayer] = it }
			}
			checkboxPreference(R.string.lbl_direct_stream_live) {
				get { userPreferences[UserPreferences.liveTvDirectPlayEnabled] }
				set { userPreferences[UserPreferences.liveTvDirectPlayEnabled] = it }
			}
		}

		private fun PreferenceScreen.shortcutsCategory(
				userPreferences: UserPreferences
		) = category(R.string.pref_button_remapping_category) {
			shortcutPreference(R.string.pref_audio_track_button) {
				get { userPreferences[UserPreferences.shortcutAudioTrack] }
				set { userPreferences[UserPreferences.shortcutAudioTrack] = it }
			}
			shortcutPreference(R.string.pref_subtitle_track_button) {
				get { userPreferences[UserPreferences.shortcutSubtitleTrack] }
				set { userPreferences[UserPreferences.shortcutSubtitleTrack] = it }
			}
		}

		private fun PreferenceScreen.crashReportingCategory(
				userPreferences: UserPreferences
		) = category(R.string.pref_acra_category) {
			checkboxPreference(R.string.pref_enable_acra, R.string.pref_acra_enabled, R.string.pref_acra_disabled) {
				get { userPreferences[UserPreferences.acraEnabled] }
				set { userPreferences[UserPreferences.acraEnabled] = it }
			}
			checkboxPreference(R.string.pref_acra_alwaysaccept, R.string.pref_acra_alwaysaccept_enabled, R.string.pref_acra_alwaysaccept_disabled) {
				get { userPreferences[UserPreferences.acraNoPrompt] }
				set { userPreferences[UserPreferences.acraNoPrompt] = it }
			}
			checkboxPreference(R.string.pref_acra_syslog, R.string.pref_acra_syslog_enabled, R.string.pref_acra_syslog_disabled) {
				get { userPreferences[UserPreferences.acraIncludeSystemLogs] }
				set { userPreferences[UserPreferences.acraIncludeSystemLogs] = it }
			}
		}

		private fun PreferenceScreen.aboutCategory() = category(R.string.pref_about_title) {
			staticString(R.string.lbl_version, Utils.getVersionString())
			staticString(R.string.pref_device_model, "${Build.MANUFACTURER} ${Build.MODEL}")
		}

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			val userPreferences = TvApp.getApplication().userPreferences

			preferenceScreen = preferenceManager.createPreferenceScreen(preferenceManager.context).apply {
				setTitle(R.string.settings_title)

				authenticationCategory(userPreferences)
				generalCategory(userPreferences)
				playbackCategory(userPreferences)
				liveTvCategory(userPreferences)
				shortcutsCategory(userPreferences)
				crashReportingCategory(userPreferences)
				aboutCategory()
			}
		}

		private fun addCustomBehavior() {
			findPreference<EditLongPreference>("libvlc_audio_delay")?.apply {
				text = TvApp.getApplication().userPreferences[UserPreferences.libVLCAudioDelay].toString()
				summaryProvider = Preference.SummaryProvider<EditLongPreference> {
					"${it.text} ms"
				}
			}
		}
	}
}
