package org.jellyfin.androidtv.ui.playback

import androidx.media3.common.text.Cue
import org.jellyfin.androidtv.preference.SubtitleBlacklistPreferences
import org.jellyfin.playback.core.PlayerVolumeState
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SubtitleBlacklistProcessorTest {
    private lateinit var subtitleBlacklistPreferences: SubtitleBlacklistPreferences
    private lateinit var volumeState: PlayerVolumeState
    private lateinit var processor: SubtitleBlacklistProcessor

    @Before
    fun setup() {
        subtitleBlacklistPreferences = mock()
        volumeState = mock()
        processor = SubtitleBlacklistProcessor(
            subtitleBlacklistPreferences = subtitleBlacklistPreferences,
            volumeState = volumeState
        )
    }

    @Test
    fun `processCues does nothing when blacklist is disabled`() {
        // Given
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled]).thenReturn(false)
        val cues = listOf(Cue.Builder().setText("This contains a blacklisted word").build())

        // When
        processor.processCues(cues)

        // Then
        verify(volumeState, never()).mute()
    }

    @Test
    fun `processCues does nothing when blacklist is empty`() {
        // Given
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled]).thenReturn(true)
        whenever(subtitleBlacklistPreferences.getBlacklistedWords()).thenReturn(emptyList())
        val cues = listOf(Cue.Builder().setText("This contains a blacklisted word").build())

        // When
        processor.processCues(cues)

        // Then
        verify(volumeState, never()).mute()
    }

    @Test
    fun `processCues does nothing when no cues have text`() {
        // Given
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled]).thenReturn(true)
        whenever(subtitleBlacklistPreferences.getBlacklistedWords()).thenReturn(listOf("blacklisted"))
        val cues = listOf(Cue.Builder().build()) // No text

        // When
        processor.processCues(cues)

        // Then
        verify(volumeState, never()).mute()
    }

    @Test
    fun `processCues mutes audio when blacklisted word is found`() {
        // Given
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled]).thenReturn(true)
        whenever(subtitleBlacklistPreferences.getBlacklistedWords()).thenReturn(listOf("blacklisted"))
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.muteAfterDuration]).thenReturn(1000)
        whenever(volumeState.muted).thenReturn(false)
        val cues = listOf(Cue.Builder().setText("This contains a blacklisted word").build())

        // When
        processor.processCues(cues)

        // Then
        verify(volumeState).mute()
    }

    @Test
    fun `processCues is case insensitive when matching blacklisted words`() {
        // Given
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled]).thenReturn(true)
        whenever(subtitleBlacklistPreferences.getBlacklistedWords()).thenReturn(listOf("blacklisted"))
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.muteAfterDuration]).thenReturn(1000)
        whenever(volumeState.muted).thenReturn(false)
        val cues = listOf(Cue.Builder().setText("This contains a BLACKLISTED word").build())

        // When
        processor.processCues(cues)

        // Then
        verify(volumeState).mute()
    }

    @Test
    fun `processCues does not mute when no blacklisted words are found`() {
        // Given
        whenever(subtitleBlacklistPreferences[SubtitleBlacklistPreferences.blacklistEnabled]).thenReturn(true)
        whenever(subtitleBlacklistPreferences.getBlacklistedWords()).thenReturn(listOf("blacklisted"))
        val cues = listOf(Cue.Builder().setText("This contains no bad words").build())

        // When
        processor.processCues(cues)

        // Then
        verify(volumeState, never()).mute()
    }

    @Test
    fun `release unmutes audio if it was muted by processor`() {
        // Given
        whenever(volumeState.muted).thenReturn(false)

        // When
        processor.release()

        // Then
        verify(volumeState, never()).unmute()
    }
}