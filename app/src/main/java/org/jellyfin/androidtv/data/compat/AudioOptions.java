package org.jellyfin.androidtv.data.compat;

import java.util.ArrayList;

import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.EncodingContext;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;

/**
 * Class AudioOptions.
 *
 * @deprecated
 */
@Deprecated
public class AudioOptions {
    public AudioOptions() {
        setContext(EncodingContext.Streaming);

        setEnableDirectPlay(true);
        setEnableDirectStream(true);
    }

    private boolean EnableDirectPlay;

    public final boolean getEnableDirectPlay() {
        return EnableDirectPlay;
    }

    public final void setEnableDirectPlay(boolean value) {
        EnableDirectPlay = value;
    }

    private boolean EnableDirectStream;

    public final boolean getEnableDirectStream() {
        return EnableDirectStream;
    }

    public final void setEnableDirectStream(boolean value) {
        EnableDirectStream = value;
    }

    private boolean ForceDirectPlay;

    public final boolean getForceDirectPlay() {
        return ForceDirectPlay;
    }

    public final void setForceDirectPlay(boolean value) {
        ForceDirectPlay = value;
    }

    private boolean ForceDirectStream;

    public final boolean getForceDirectStream() {
        return ForceDirectStream;
    }

    public final void setForceDirectStream(boolean value) {
        ForceDirectStream = value;
    }

    private String ItemId;

    public final String getItemId() {
        return ItemId;
    }

    public final void setItemId(String value) {
        ItemId = value;
    }

    private ArrayList<MediaSourceInfo> MediaSources;

    public final ArrayList<MediaSourceInfo> getMediaSources() {
        return MediaSources;
    }

    public final void setMediaSources(ArrayList<MediaSourceInfo> value) {
        MediaSources = value;
    }

    private DeviceProfile Profile;

    public final DeviceProfile getProfile() {
        return Profile;
    }

    public final void setProfile(DeviceProfile value) {
        Profile = value;
    }

    /**
     * Optional. Only needed if a specific AudioStreamIndex or SubtitleStreamIndex are requested.
     */
    private String MediaSourceId;

    public final String getMediaSourceId() {
        return MediaSourceId;
    }

    public final void setMediaSourceId(String value) {
        MediaSourceId = value;
    }

    private String DeviceId;

    public final String getDeviceId() {
        return DeviceId;
    }

    public final void setDeviceId(String value) {
        DeviceId = value;
    }

    /**
     * Allows an override of supported number of audio channels
     * Example: DeviceProfile supports five channel, but user only has stereo speakers
     */
    private Integer MaxAudioChannels = null;

    public final Integer getMaxAudioChannels() {
        return MaxAudioChannels;
    }

    public final void setMaxAudioChannels(Integer value) {
        MaxAudioChannels = value;
    }

    /**
     * The application's configured quality setting
     */
    private Integer MaxBitrate = null;

    public final Integer getMaxBitrate() {
        return MaxBitrate;
    }

    public final void setMaxBitrate(Integer value) {
        MaxBitrate = value;
    }

    /**
     * Gets or sets the context.
     *
     * <value>The context.</value>
     */
    private EncodingContext Context = EncodingContext.values()[0];

    public final EncodingContext getContext() {
        return Context;
    }

    public final void setContext(EncodingContext value) {
        Context = value;
    }

    /**
     * Gets or sets the audio transcoding bitrate.
     *
     * <value>The audio transcoding bitrate.</value>
     */
    private Integer AudioTranscodingBitrate = null;

    public final Integer getAudioTranscodingBitrate() {
        return AudioTranscodingBitrate;
    }

    public final void setAudioTranscodingBitrate(Integer value) {
        AudioTranscodingBitrate = value;
    }

    /**
     * Gets the maximum bitrate.
     *
     * @return System.Nullable&lt;System.Int32&gt;.
     */
    public final Integer GetMaxBitrate(boolean isAudio) {
        if (getMaxBitrate() != null) {
            return getMaxBitrate();
        }

        if (getProfile() != null) {
            if (getContext() == EncodingContext.Static) {
                if (isAudio && getProfile().getMaxStaticMusicBitrate() != null) {
                    return getProfile().getMaxStaticMusicBitrate();
                }
                return getProfile().getMaxStaticBitrate();
            }

            return getProfile().getMaxStreamingBitrate();
        }

        return null;
    }
}
