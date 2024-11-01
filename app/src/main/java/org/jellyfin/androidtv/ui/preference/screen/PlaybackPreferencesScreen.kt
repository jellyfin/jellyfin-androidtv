package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.preference.custom.DurationSeekBarPreference
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.colorList
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.seekbar
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.koin.android.ext.android.inject

class PlaybackPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	private val mediaSegmentRepository: MediaSegmentRepository by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_playback)

		category {
			setTitle(R.string.pref_customization)

			enum<NextUpBehavior> {
				setTitle(R.string.pref_next_up_behavior_title)
				bind(userPreferences, UserPreferences.nextUpBehavior)
				depends { userPreferences[UserPreferences.mediaQueuingEnabled] }
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.pref_next_up_timeout_title)
				setContent(R.string.pref_next_up_timeout_summary)
				min = 0 // value of 0 disables timer
				max = 30_000
				increment = 1_000
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = when (value) {
						NEXTUP_TIMER_DISABLED -> getString(R.string.pref_next_up_timeout_disabled)
						else -> "${value / 1000}s"
					}
				}
				bind(userPreferences, UserPreferences.nextUpTimeout)
				depends {
					userPreferences[UserPreferences.mediaQueuingEnabled]
						&& userPreferences[UserPreferences.nextUpBehavior] != NextUpBehavior.DISABLED
				}
			}

			checkbox {
				setTitle(R.string.lbl_enable_cinema_mode)
				setContent(R.string.sum_enable_cinema_mode)
				bind(userPreferences, UserPreferences.cinemaModeEnabled)
			}
		}

		category {
			setTitle(R.string.pref_audio)

			enum<AudioBehavior> {
				setTitle(R.string.lbl_audio_output)
				bind(userPreferences, UserPreferences.audioBehaviour)
			}
		}

		category {
			setTitle(R.string.pref_subtitles)

			@Suppress("MagicNumber")
			colorList {
				setTitle(R.string.lbl_subtitle_text_color)
				entries = mapOf(
					0xFFFFFFFFL to context.getString(R.string.color_white),
					0XFF000000L to context.getString(R.string.color_black),
					0xFF7F7F7FL to context.getString(R.string.color_darkgrey),
					0xFFC80000L to context.getString(R.string.color_red),
					0xFF00C800L to context.getString(R.string.color_green),
					0xFF0000C8L to context.getString(R.string.color_blue),
					0xFFEEDC00L to context.getString(R.string.color_yellow),
					0xFFD60080L to context.getString(R.string.color_pink),
					0xFF009FDAL to context.getString(R.string.color_cyan),
				)
				bind(userPreferences, UserPreferences.subtitlesTextColor)
			}

			colorList {
				setTitle(R.string.lbl_subtitle_background_color)
				entries = mapOf(
					0x00FFFFFFL to context.getString(R.string.lbl_none),
					0xFFFFFFFFL to context.getString(R.string.color_white),
					0XFF000000L to context.getString(R.string.color_black),
					0xFF7F7F7FL to context.getString(R.string.color_darkgrey),
					0xFFC80000L to context.getString(R.string.color_red),
					0xFF00C800L to context.getString(R.string.color_green),
					0xFF0000C8L to context.getString(R.string.color_blue),
					0xFFEEDC00L to context.getString(R.string.color_yellow),
					0xFFD60080L to context.getString(R.string.color_pink),
					0xFF009FDAL to context.getString(R.string.color_cyan),
				)
				bind(userPreferences, UserPreferences.subtitlesBackgroundColor)
			}

			colorList {
				setTitle(R.string.lbl_subtitle_text_stroke_color)
				entries = mapOf(
					0x00FFFFFFL to context.getString(R.string.lbl_none),
					0xFFFFFFFFL to context.getString(R.string.color_white),
					0XFF000000L to context.getString(R.string.color_black),
					0xFF7F7F7FL to context.getString(R.string.color_darkgrey),
					0xFFC80000L to context.getString(R.string.color_red),
					0xFF00C800L to context.getString(R.string.color_green),
					0xFF0000C8L to context.getString(R.string.color_blue),
					0xFFEEDC00L to context.getString(R.string.color_yellow),
					0xFFD60080L to context.getString(R.string.color_pink),
					0xFF009FDAL to context.getString(R.string.color_cyan),
				)
				bind(userPreferences, UserPreferences.subtitleTextStrokeColor)
			}

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.pref_subtitles_size)
				entries = mapOf(
					0.25f to context.getString(R.string.pref_subtitles_size_very_small),
					0.5f to context.getString(R.string.pref_subtitles_size_small),
					1.0f to context.getString(R.string.pref_subtitles_size_normal),
					1.5f to context.getString(R.string.pref_subtitles_size_large),
					2.0f to context.getString(R.string.pref_subtitles_size_very_large),
				).mapKeys { it.key.toString() }

				bind {
					get { userPreferences[UserPreferences.subtitlesTextSize].toString() }
					set { value -> userPreferences[UserPreferences.subtitlesTextSize] = value.toFloat() }
					default { UserPreferences.subtitlesTextSize.defaultValue.toString() }
				}
			}
		}

		category {
			setTitle(R.string.pref_mediasegment_actions)

			for (segmentType in MediaSegmentRepository.SupportedTypes) {
				enum<MediaSegmentAction> {
					when (segmentType) {
						MediaSegmentType.UNKNOWN -> R.string.segment_type_unknown
						MediaSegmentType.COMMERCIAL -> R.string.segment_type_commercial
						MediaSegmentType.PREVIEW -> R.string.segment_type_preview
						MediaSegmentType.RECAP -> R.string.segment_type_recap
						MediaSegmentType.OUTRO -> R.string.segment_type_outro
						MediaSegmentType.INTRO -> R.string.segment_type_intro
					}.let(::setTitle)

					bind {
						get { mediaSegmentRepository.getDefaultSegmentTypeAction(segmentType) }
						set { value -> mediaSegmentRepository.setDefaultSegmentTypeAction(segmentType, value) }
						default { MediaSegmentAction.NOTHING }
					}
				}
			}
		}

		category {
			setTitle(R.string.advanced_settings)

			link {
				setTitle(R.string.pref_playback_advanced)
				icon = R.drawable.ic_more
				withFragment<PlaybackAdvancedPreferencesScreen>()
			}
		}
	}
}
