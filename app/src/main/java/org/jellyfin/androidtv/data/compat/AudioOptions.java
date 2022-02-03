package org.jellyfin.androidtv.data.compat;

import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.EncodingContext;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;

import java.util.ArrayList;

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
}
