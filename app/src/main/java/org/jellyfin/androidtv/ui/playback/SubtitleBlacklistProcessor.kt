package org.jellyfin.androidtv.ui.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.text.Cue
import androidx.media3.common.text.SpannedString
import org.jellyfin.androidtv.preference.SubtitleBlacklistPreferences
import org.jellyfin.playback.core.PlayerVolumeState
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Processes subtitles to detect blacklisted words and mute audio when they are found.
 */
class SubtitleBlacklistProcessor(
    private val subtitleBlacklistPreferences: SubtitleBlacklistPreferences,
    private val volumeState: PlayerVolumeState
) {
    private val handler = Handler(Looper.getMainLooper())
    private var wasMuted = false
    private var originalMuteState = false
    private var isMutingActive = false
    
    /**
     * Process a list of subtitle cues to check for blacklisted words.
     * If a blacklisted word is found, mute the audio for the specified duration
     * and censor the blacklisted words in the subtitle text.
     * 
     * @return The list of cues with censored text if blacklisted words were found
     */
    fun processCues(cues: List<Cue>): List<Cue> {
        if (!subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled]) return cues
        
        val blacklistedWords = subtitleBlacklistPreferences.getBlacklistedWords()
        if (blacklistedWords.isEmpty()) return cues
        
        // Extract text from all cues
        val subtitleText = cues.mapNotNull { it.text?.toString() }.joinToString(" ")
        if (subtitleText.isBlank()) return cues
        
        // Check if any blacklisted word is in the subtitle text
        val foundBlacklistedWord = blacklistedWords.any { word ->
            subtitleText.contains(word, ignoreCase = true)
        }
        
        if (foundBlacklistedWord) {
            muteAudio()
            
            // Censor blacklisted words in the subtitle text
            return censorBlacklistedWords(cues, blacklistedWords)
        }
        
        return cues
    }
    
    /**
     * Censor blacklisted words in subtitle cues by replacing them with asterisks.
     * 
     * @param cues The original subtitle cues
     * @param blacklistedWords The list of words to censor
     * @return The list of cues with censored text
     */
    private fun censorBlacklistedWords(cues: List<Cue>, blacklistedWords: List<String>): List<Cue> {
        return cues.map { cue ->
            val text = cue.text?.toString() ?: return@map cue
            
            var censoredText = text
            for (word in blacklistedWords) {
                if (text.contains(word, ignoreCase = true)) {
                    // Create a pattern that matches the word with case insensitivity
                    val pattern = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(censoredText)
                    
                    // Replace each occurrence with asterisks of the same length
                    val sb = StringBuffer()
                    while (matcher.find()) {
                        val replacement = "*".repeat(matcher.group().length)
                        matcher.appendReplacement(sb, replacement)
                    }
                    matcher.appendTail(sb)
                    censoredText = sb.toString()
                }
            }
            
            // If text was censored, create a new cue with the censored text
            if (censoredText != text) {
                Timber.d("Censored blacklisted word in subtitle")
                Cue.Builder()
                    .setText(SpannedString(censoredText))
                    .setLine(cue.line)
                    .setLineType(cue.lineType)
                    .setLineAnchor(cue.lineAnchor)
                    .setPosition(cue.position)
                    .setPositionAnchor(cue.positionAnchor)
                    .setSize(cue.size)
                    .setTextAlignment(cue.textAlignment)
                    .build()
            } else {
                cue
            }
        }
    }
    
    /**
     * Mute the audio for the specified duration.
     */
    private fun muteAudio() {
        if (isMutingActive) {
            // Already muting, just extend the unmute timer
            handler.removeCallbacksAndMessages(UNMUTE_TOKEN)
        } else {
            // Start new muting session
            isMutingActive = true
            originalMuteState = volumeState.muted
            
            if (!volumeState.muted) {
                Timber.d("Muting audio due to blacklisted word in subtitle")
                volumeState.mute()
                wasMuted = true
            }
        }
        
        // Schedule unmuting after the specified duration
        val muteAfterDuration = subtitleBlacklistPreferences[SubtitleBlacklistPreferences.muteAfterDuration].toLong()
        handler.postDelayed({
            if (wasMuted && !originalMuteState) {
                Timber.d("Unmuting audio after blacklisted word")
                volumeState.unmute()
            }
            wasMuted = false
            isMutingActive = false
        }, UNMUTE_TOKEN, muteAfterDuration)
    }
    
    /**
     * Clean up resources when the processor is no longer needed.
     */
    fun release() {
        handler.removeCallbacksAndMessages(null)
        if (wasMuted && !originalMuteState) {
            volumeState.unmute()
        }
        wasMuted = false
        isMutingActive = false
    }
    
    companion object {
        private val UNMUTE_TOKEN = Any()
    }
}