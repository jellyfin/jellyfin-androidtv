package org.jellyfin.androidtv.ui.livetv

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.androidtv.preference.constant.LiveTvChannelOrder
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsItemCheckbox
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.preference.store.PreferenceStore
import org.koin.android.ext.android.inject

class GuideOptionsScreen : OptionsFragment() {
	private val liveTvPreferences: LiveTvPreferences by inject()

	override val stores: Array<PreferenceStore<*, *>>
		get() = arrayOf(liveTvPreferences)

	override val screen by optionsScreen {
		setTitle(R.string.live_tv_preferences)

		category {
			enum<LiveTvChannelOrder> {
				title = getString(R.string.lbl_sort_by)

				bind {
					get { LiveTvChannelOrder.fromString(liveTvPreferences[LiveTvPreferences.channelOrder]) }
					set { liveTvPreferences[LiveTvPreferences.channelOrder] = it.stringValue }
					default { LiveTvChannelOrder.fromString(liveTvPreferences.getDefaultValue(LiveTvPreferences.channelOrder)) }
				}
			}

			checkbox {
				title = getString(R.string.lbl_start_favorites)
				bind(liveTvPreferences, LiveTvPreferences.favsAtTop)
			}

			checkbox {
				title = getString(R.string.lbl_colored_backgrounds)
				bind(liveTvPreferences, LiveTvPreferences.colorCodeGuide)
			}
		}

		category {
			title = getString(R.string.lbl_show_indicators)

			setOf(
				LiveTvPreferences.showHDIndicator to getString(R.string.lbl_hd_programs),
				LiveTvPreferences.showLiveIndicator to getString(R.string.lbl_live_broadcasts),
				LiveTvPreferences.showNewIndicator to getString(R.string.lbl_new_episodes),
				LiveTvPreferences.showPremiereIndicator to getString(R.string.lbl_premieres),
				LiveTvPreferences.showRepeatIndicator to getString(R.string.lbl_repeat_episodes),
			).forEach { (preference, label) ->
				checkbox {
					title = label
					type = OptionsItemCheckbox.Type.SWITCH
					bind(liveTvPreferences, preference)
				}
			}
		}
	}
}
