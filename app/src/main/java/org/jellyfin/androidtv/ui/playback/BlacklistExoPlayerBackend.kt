package org.jellyfin.androidtv.ui.playback

import android.content.Context
import androidx.media3.common.text.CueGroup
import org.jellyfin.androidtv.preference.SubtitleBlacklistPreferences
import org.jellyfin.playback.media3.exoplayer.ExoPlayerBackend
import org.jellyfin.playback.media3.exoplayer.ExoPlayerOptions
import timber.log.Timber

/**
 * Custom ExoPlayer backend that processes subtitles to detect blacklisted words
 * and mute audio when they are found.
 */
class BlacklistExoPlayerBackend(
    context: Context,
    exoPlayerOptions: ExoPlayerOptions,
    private val subtitleBlacklistPreferences: SubtitleBlacklistPreferences
) : ExoPlayerBackend(context, exoPlayerOptions) {
    
    private var subtitleProcessor: SubtitleBlacklistProcessor? = null
    
    init {
        Timber.d("Initializing BlacklistExoPlayerBackend")
        // Create the subtitle processor with the volume state from the parent class
        subtitleProcessor = SubtitleBlacklistProcessor(
            subtitleBlacklistPreferences = subtitleBlacklistPreferences,
            volumeState = state.volume
        )
    }
    
    /**
     * Override the onCues method to intercept subtitle cues and process them
     * with our blacklist processor before passing them to the parent class.
     * This includes censoring blacklisted words in the subtitle text.
     */
    override fun onCues(cueGroup: CueGroup) {
        // Process cues with our blacklist processor and get censored cues
        val censoredCues = subtitleProcessor?.processCues(cueGroup.cues) ?: cueGroup.cues
        
        // Create a new CueGroup with the censored cues
        val censoredCueGroup = CueGroup(censoredCues, cueGroup.presentationTimeUs)
        
        // Call the parent method with the censored cue group
        super.onCues(censoredCueGroup)
    }
    
    /**
     * Clean up resources when the backend is no longer needed.
     */
    override fun release() {
        subtitleProcessor?.release()
        subtitleProcessor = null
        super.release()
    }
}