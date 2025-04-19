package org.jellyfin.androidtv.ui.preference.screen

import android.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.SubtitleBlacklistPreferences
import org.jellyfin.androidtv.ui.preference.custom.DurationSeekBarPreference
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.seekbar
import org.jellyfin.preference.store.PreferenceStore
import org.koin.android.ext.android.inject

/**
 * Preference screen for subtitle blacklist settings.
 * Allows users to enable/disable the feature, manage blacklisted words,
 * and configure mute durations.
 */
class SubtitleBlacklistPreferencesScreen : OptionsFragment() {
    private val subtitleBlacklistPreferences: SubtitleBlacklistPreferences by inject()

    override val stores: Array<PreferenceStore<*, *>>
        get() = arrayOf(subtitleBlacklistPreferences)

    override val screen by optionsScreen {
        setTitle(R.string.pref_subtitle_blacklist)

        category {
            setTitle(R.string.pref_subtitle_blacklist)

            checkbox {
                setTitle(R.string.pref_subtitle_blacklist_enable)
                setContent(R.string.pref_subtitle_blacklist_enable_summary)
                bind(subtitleBlacklistPreferences, SubtitleBlacklistPreferences.blacklistEnabled)
            }

            @Suppress("MagicNumber")
            seekbar {
                setTitle(R.string.pref_subtitle_blacklist_mute_before)
                setContent(R.string.pref_subtitle_blacklist_mute_before_summary)
                min = 0
                max = 5000
                increment = 250
                valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
                    override fun display(value: Int): String = "${value.toDouble() / 1000}s"
                }
                bind(subtitleBlacklistPreferences, SubtitleBlacklistPreferences.muteBeforeDuration)
                depends { subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled] }
            }

            @Suppress("MagicNumber")
            seekbar {
                setTitle(R.string.pref_subtitle_blacklist_mute_after)
                setContent(R.string.pref_subtitle_blacklist_mute_after_summary)
                min = 0
                max = 5000
                increment = 250
                valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
                    override fun display(value: Int): String = "${value.toDouble() / 1000}s"
                }
                bind(subtitleBlacklistPreferences, SubtitleBlacklistPreferences.muteAfterDuration)
                depends { subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled] }
            }
        }

        category {
            setTitle(R.string.pref_subtitle_blacklist_words)
            depends { subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled] }

            action {
                setTitle(R.string.pref_subtitle_blacklist_add_word)
                icon = R.drawable.ic_add
                depends { subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled] }

                onClick { _, _ ->
                    val editText = EditText(context)
                    editText.hint = getString(R.string.pref_subtitle_blacklist_add_word_hint)

                    AlertDialog.Builder(context)
                        .setTitle(R.string.pref_subtitle_blacklist_add_word_title)
                        .setView(editText)
                        .setPositiveButton(R.string.lbl_ok) { _, _ ->
                            val word = editText.text.toString().trim()
                            if (word.isNotEmpty()) {
                                subtitleBlacklistPreferences.addBlacklistedWord(word)
                                Toast.makeText(context, R.string.pref_subtitle_blacklist_word_added, Toast.LENGTH_SHORT).show()
                                reloadCategory()
                            }
                        }
                        .setNegativeButton(R.string.lbl_cancel, null)
                        .show()
                }
            }

            // Display all blacklisted words as list items
            val blacklistedWords = subtitleBlacklistPreferences.getBlacklistedWords()
            if (blacklistedWords.isEmpty()) {
                action {
                    setTitle(R.string.pref_subtitle_blacklist_empty)
                    isSelectable = false
                }
            } else {
                for (word in blacklistedWords) {
                    action {
                        setTitle(word)
                        icon = R.drawable.ic_delete

                        onClick { _, _ ->
                            AlertDialog.Builder(context)
                                .setTitle(word)
                                .setMessage(R.string.lbl_delete)
                                .setPositiveButton(R.string.lbl_yes) { _, _ ->
                                    subtitleBlacklistPreferences.removeBlacklistedWord(word)
                                    Toast.makeText(context, R.string.pref_subtitle_blacklist_word_removed, Toast.LENGTH_SHORT).show()
                                    reloadCategory()
                                }
                                .setNegativeButton(R.string.lbl_no, null)
                                .show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Reload the current category to reflect changes in blacklisted words
     */
    private fun reloadCategory() {
        parentFragmentManager.beginTransaction()
            .detach(this)
            .attach(this)
            .commit()
    }
}