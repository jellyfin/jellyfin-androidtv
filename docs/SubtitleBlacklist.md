# Subtitle Blacklist Feature

The Subtitle Blacklist feature allows users to automatically mute audio and censor text when specific words or phrases appear in subtitles. This can be useful for filtering out unwanted language or content during playback.

## How It Works

1. Users can enable the feature in the Playback Preferences under the Subtitles section
2. Users can add words or phrases to the blacklist
3. When enabled, the system will detect these words in subtitles during playback and:
   - Temporarily mute the audio
   - Replace the blacklisted words with asterisks (*) in the displayed subtitles
4. Users can configure how long to mute before and after the blacklisted word appears

## Implementation Details

The feature consists of several components:

- **SubtitleBlacklistPreferences**: Stores user preferences including the list of blacklisted words and mute durations
- **SubtitleBlacklistProcessor**: Processes subtitle cues to detect blacklisted words, controls audio muting, and censors blacklisted words in the displayed text
- **BlacklistExoPlayerBackend**: Custom ExoPlayer backend that intercepts subtitle cues and passes them to the processor
- **SubtitleBlacklistPreferencesScreen**: UI for managing blacklisted words and settings

## Testing

The feature includes unit tests for both the SubtitleBlacklistProcessor and BlacklistExoPlayerBackend classes to ensure proper functionality.

## Future Improvements

Possible future enhancements could include:

- Regex pattern matching for more flexible word detection
- Per-word customization of mute durations
- Additional text censoring options beyond asterisks (e.g., blur effect or custom replacement text)
- Option to only censor text without muting audio