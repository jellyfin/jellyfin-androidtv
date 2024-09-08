package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class PlaybackAdvancedCodecPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by optionsScreen {
		setTitle(R.string.codec)

		category {
			setTitle(R.string.level_warning)

			list {
				setTitle(R.string.user_avc_level)
				entries = mapOf(
					// AVC levels as reported by ffprobe are multiplied by 10, e.g. level 4.1 is 41.
					"auto" to "Auto",
					"62" to "Level 6.2",
					"61" to "Level 6.1",
					"60" to "Level 6.0",
					"52" to "Level 5.2",
					"51" to "Level 5.1",
					"50" to "Level 5.0",
					"42" to "Level 4.2",
					"41" to "Level 4.1",
					"40" to "Level 4.0"
				)
				bind(userPreferences, UserPreferences.userAVCLevel)
			}

			list {
				setTitle(R.string.user_hevc_level)
				entries = mapOf(
					// HEVC levels as reported by ffprobe are multiplied by 30, e.g. level 4.1 is 123.
					"auto" to "Auto",
					"186" to "Level 6.2",
					"183" to "Level 6.1",
					"180" to "Level 6.0",
					"156" to "Level 5.2",
					"153" to "Level 5.1",
					"150" to "Level 5.0",
					"123" to "Level 4.1",
					"120" to "Level 4.0"
				)
				bind(userPreferences, UserPreferences.userHEVCLevel)
			}
		}

		category {
			setTitle(R.string.pref_audio)

			checkbox {
				setTitle(R.string.lbl_bitstream_ac3)
				setContent(R.string.desc_bitstream_ac3)
				bind(userPreferences, UserPreferences.ac3Enabled)
			}
		}
	}
}
