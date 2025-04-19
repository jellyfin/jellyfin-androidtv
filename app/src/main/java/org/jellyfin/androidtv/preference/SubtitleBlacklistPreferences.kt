package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

/**
 * Preferences for subtitle blacklist functionality.
 * Stores blacklisted words/phrases and related settings.
 */
class SubtitleBlacklistPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("subtitle_blacklist", Context.MODE_PRIVATE)
) {
	companion object {
		/**
		 * Enable subtitle blacklist functionality
		 */
		val blacklistEnabled = booleanPreference("blacklist_enabled", false)

		/**
		 * Semicolon-separated list of words/phrases to blacklist
		 */
		val blacklistedWords = stringPreference("blacklisted_words", "")

		/**
		 * Duration in milliseconds to mute audio before the blacklisted word appears
		 */
		val muteBeforeDuration = intPreference("mute_before_duration", 1000)

		/**
		 * Duration in milliseconds to mute audio after the blacklisted word appears
		 */
		val muteAfterDuration = intPreference("mute_after_duration", 1000)
	}

	/**
	 * Get the list of blacklisted words/phrases
	 */
	fun getBlacklistedWords(): List<String> {
		val wordsString = get(blacklistedWords)
		return if (wordsString.isBlank()) {
			emptyList()
		} else {
			wordsString.split(";").map { it.trim() }.filter { it.isNotEmpty() }
		}
	}

	/**
	 * Set the list of blacklisted words/phrases
	 */
	fun setBlacklistedWords(words: List<String>) {
		set(blacklistedWords, words.joinToString(";"))
	}

	/**
	 * Add a word/phrase to the blacklist
	 */
	fun addBlacklistedWord(word: String) {
		val words = getBlacklistedWords().toMutableList()
		if (word.trim().isNotEmpty() && !words.contains(word.trim())) {
			words.add(word.trim())
			setBlacklistedWords(words)
		}
	}

	/**
	 * Remove a word/phrase from the blacklist
	 */
	fun removeBlacklistedWord(word: String) {
		val words = getBlacklistedWords().toMutableList()
		words.remove(word.trim())
		setBlacklistedWords(words)
	}
}