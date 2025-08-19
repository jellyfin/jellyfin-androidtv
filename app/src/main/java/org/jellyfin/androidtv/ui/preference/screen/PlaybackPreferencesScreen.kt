package org.jellyfin.androidtv.ui.preference.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.preference.constant.StillWatchingBehavior
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.preference.custom.DurationSeekBarPreference
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.colorList
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.seekbar
import org.jellyfin.preference.store.PreferenceStore
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

class PlaybackPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	private val userSettingPreferences: UserSettingPreferences by inject()
	private val mediaSegmentRepository: MediaSegmentRepository by inject()

	override val stores: Array<PreferenceStore<*, *>>
		get() = arrayOf(userSettingPreferences)

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

			enum<StillWatchingBehavior> {
				setTitle(R.string.pref_still_watching_behavior_title)
				bind(userPreferences, UserPreferences.stillWatchingBehavior)
				depends { userPreferences[UserPreferences.mediaQueuingEnabled] }
			}

			checkbox {
				setTitle(R.string.lbl_enable_cinema_mode)
				setContent(R.string.sum_enable_cinema_mode)
				bind(userPreferences, UserPreferences.cinemaModeEnabled)
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.skip_forward_length)
				setContent(R.string.skip_forward_length)
				min = 5_000
				max = 30_000
				increment = 5_000
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int) = "${value / 1000}s"
				}
				bind(userSettingPreferences, UserSettingPreferences.skipForwardLength)
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
					Color(0xFFFFFFFFL) to context.getString(R.string.color_white),
					Color(0XFF000000L) to context.getString(R.string.color_black),
					Color(0xFF7F7F7FL) to context.getString(R.string.color_darkgrey),
					Color(0xFFC80000L) to context.getString(R.string.color_red),
					Color(0xFF00C800L) to context.getString(R.string.color_green),
					Color(0xFF0000C8L) to context.getString(R.string.color_blue),
					Color(0xFFEEDC00L) to context.getString(R.string.color_yellow),
					Color(0xFFD60080L) to context.getString(R.string.color_pink),
					Color(0xFF009FDAL) to context.getString(R.string.color_cyan),
				)

				bind {
					get { Color(userPreferences[UserPreferences.subtitlesTextColor]) }
					set { value -> userPreferences[UserPreferences.subtitlesTextColor] = value.toArgb().toLong() }
					default { Color(UserPreferences.subtitlesTextColor.defaultValue) }
				}
			}

			colorList {
				setTitle(R.string.lbl_subtitle_background_color)
				entries = mapOf(
					Color(0x00FFFFFFL) to context.getString(R.string.lbl_none),
					Color(0xFFFFFFFFL) to context.getString(R.string.color_white),
					Color(0XFF000000L) to context.getString(R.string.color_black),
					Color(0xFF7F7F7FL) to context.getString(R.string.color_darkgrey),
					Color(0xFFC80000L) to context.getString(R.string.color_red),
					Color(0xFF00C800L) to context.getString(R.string.color_green),
					Color(0xFF0000C8L) to context.getString(R.string.color_blue),
					Color(0xFFEEDC00L) to context.getString(R.string.color_yellow),
					Color(0xFFD60080L) to context.getString(R.string.color_pink),
					Color(0xFF009FDAL) to context.getString(R.string.color_cyan),
				)

				// Keep the alpha of the saved color as we're using a separate option to change alpha
				bind {
					get {
						Color(userPreferences[UserPreferences.subtitlesBackgroundColor])
							.let { it.copy(alpha = if (it.alpha == 0f) 0f else 1f) }
					}
					set { value ->
						userPreferences[UserPreferences.subtitlesBackgroundColor] = value
							.let {
								val currentAlpha = Color(userPreferences[UserPreferences.subtitlesBackgroundColor]).alpha
									.takeIf { alpha -> alpha != 0f }
									?: 1f
								it.copy(alpha = if (it.alpha == 0f) 0f else currentAlpha)
							}
							.toArgb()
							.toLong()
					}
					default { Color(UserPreferences.subtitlesBackgroundColor.defaultValue) }
				}
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.pref_subtitles_background_opacity)
				min = 20
				max = 100
				increment = 10
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = "$value%"
				}

				// Only set the alpha on the existing color, which is changed with the option above
				bind {
					get { (Color(userPreferences[UserPreferences.subtitlesBackgroundColor]).alpha * 100f).roundToInt() }
					set { value ->
						userPreferences[UserPreferences.subtitlesBackgroundColor] =
							Color(userPreferences[UserPreferences.subtitlesBackgroundColor])
								.copy(alpha = value / 100f)
								.toArgb()
								.toLong()
					}
					default { (Color(UserPreferences.subtitlesBackgroundColor.defaultValue).alpha * 100f).roundToInt() }
				}

				depends {
					Color(userPreferences[UserPreferences.subtitlesBackgroundColor]).alpha > 0f
				}
			}

			colorList {
				setTitle(R.string.lbl_subtitle_text_stroke_color)
				entries = mapOf(
					Color(0x00FFFFFFL) to context.getString(R.string.lbl_none),
					Color(0xFFFFFFFFL) to context.getString(R.string.color_white),
					Color(0XFF000000L) to context.getString(R.string.color_black),
					Color(0xFF7F7F7FL) to context.getString(R.string.color_darkgrey),
					Color(0xFFC80000L) to context.getString(R.string.color_red),
					Color(0xFF00C800L) to context.getString(R.string.color_green),
					Color(0xFF0000C8L) to context.getString(R.string.color_blue),
					Color(0xFFEEDC00L) to context.getString(R.string.color_yellow),
					Color(0xFFD60080L) to context.getString(R.string.color_pink),
					Color(0xFF009FDAL) to context.getString(R.string.color_cyan),
				)

				bind {
					get { Color(userPreferences[UserPreferences.subtitleTextStrokeColor]) }
					set { value -> userPreferences[UserPreferences.subtitleTextStrokeColor] = value.toArgb().toLong() }
					default { Color(UserPreferences.subtitleTextStrokeColor.defaultValue) }
				}
			}

			@Suppress("MagicNumber")
			// Stored in floats (1f = 100%) but seekbar preference works with integers only
			seekbar {
				setTitle(R.string.pref_subtitles_size)
				min = 25 // 0.25f
				max = 250 // 2.5f
				increment = 5 // 0.05f
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = "$value%"
				}

				bind {
					get { (userPreferences[UserPreferences.subtitlesTextSize] * 100f).roundToInt() }
					set { value -> userPreferences[UserPreferences.subtitlesTextSize] = value / 100f }
					default { (UserPreferences.subtitlesTextSize.defaultValue * 100f).roundToInt() }
				}
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.pref_subtitles_position)
				min = 0 // 0.0f
				max = 80 // 0.8f
				increment = 1 // 0.01f
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = "$value%"
				}

				bind {
					get { (userPreferences[UserPreferences.subtitlesOffsetPosition] * 100f).roundToInt() }
					set { value -> userPreferences[UserPreferences.subtitlesOffsetPosition] = value / 100f }
					default { (UserPreferences.subtitlesOffsetPosition.defaultValue * 100f).roundToInt() }
				}
			}

			checkbox {
				setTitle(R.string.pref_subtitles_bold)
				bind {
					val boldWeight = 700
					val normalWeight = UserPreferences.subtitlesTextWeight.defaultValue
					get { userPreferences[UserPreferences.subtitlesTextWeight] == boldWeight }
					set { checked -> userPreferences[UserPreferences.subtitlesTextWeight] = if (checked) boldWeight else normalWeight }
					default { false }
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
