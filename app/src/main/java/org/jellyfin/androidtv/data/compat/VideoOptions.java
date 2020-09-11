package org.jellyfin.androidtv.data.compat;

/**
 * Class VideoOptions.
 *
 * @deprecated
 */
@Deprecated
public class VideoOptions extends AudioOptions {
    private Integer AudioStreamIndex;
    private Integer SubtitleStreamIndex;

    public final Integer getAudioStreamIndex() {
        return AudioStreamIndex;
    }

    public final void setAudioStreamIndex(Integer value) {
        AudioStreamIndex = value;
    }

    public final Integer getSubtitleStreamIndex() {
        return SubtitleStreamIndex;
    }

    public final void setSubtitleStreamIndex(Integer value) {
        SubtitleStreamIndex = value;
    }
}
