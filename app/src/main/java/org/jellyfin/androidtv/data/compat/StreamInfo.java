package org.jellyfin.androidtv.data.compat;

import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.EncodingContext;
import org.jellyfin.apiclient.model.dlna.SubtitleProfile;
import org.jellyfin.apiclient.model.dlna.TranscodeSeekInfo;
import org.jellyfin.apiclient.model.session.PlayMethod;
import org.jellyfin.sdk.model.api.MediaSourceInfo;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod;

import java.util.ArrayList;
import java.util.UUID;

public class StreamInfo {
    private UUID ItemId;

    public final UUID getItemId() {
        return ItemId;
    }

    public final void setItemId(UUID value) {
        ItemId = value;
    }

    private String MediaUrl;

    public final String getMediaUrl() {
        return MediaUrl;
    }

    public final void setMediaUrl(String value) {
        MediaUrl = value;
    }

    private PlayMethod playMethod = PlayMethod.DirectPlay;

    public final PlayMethod getPlayMethod() {
        return playMethod;
    }

    public final void setPlayMethod(PlayMethod value) {
        playMethod = value;
    }

    private EncodingContext Context = EncodingContext.values()[0];

    public final EncodingContext getContext() {
        return Context;
    }

    public final void setContext(EncodingContext value) {
        Context = value;
    }

    private String Container;

    public final String getContainer() {
        return Container;
    }

    public final void setContainer(String value) {
        Container = value;
    }

    private DeviceProfile DeviceProfile;

    public final DeviceProfile getDeviceProfile() {
        return DeviceProfile;
    }

    public final void setDeviceProfile(DeviceProfile value) {
        DeviceProfile = value;
    }

    private Long RunTimeTicks = null;

    public final Long getRunTimeTicks() {
        return RunTimeTicks;
    }

    public final void setRunTimeTicks(Long value) {
        RunTimeTicks = value;
    }

    private TranscodeSeekInfo TranscodeSeekInfo = getTranscodeSeekInfo().values()[0];

    public final TranscodeSeekInfo getTranscodeSeekInfo() {
        return TranscodeSeekInfo;
    }

    private MediaSourceInfo MediaSource;

    public final MediaSourceInfo getMediaSource() {
        return MediaSource;
    }

    public final void setMediaSource(MediaSourceInfo value) {
        MediaSource = value;
    }

    public final org.jellyfin.sdk.model.api.SubtitleDeliveryMethod getSubtitleDeliveryMethod() {
        Integer subtitleStreamIndex = MediaSource.getDefaultSubtitleStreamIndex();
        if (subtitleStreamIndex == null || subtitleStreamIndex == -1) return SubtitleDeliveryMethod.DROP;
        return MediaSource.getMediaStreams().get(subtitleStreamIndex).getDeliveryMethod();
    }

    private String PlaySessionId;

    public final String getPlaySessionId() {
        return PlaySessionId;
    }

    public final void setPlaySessionId(String value) {
        PlaySessionId = value;
    }

    public final String getMediaSourceId() {
        return getMediaSource() == null ? null : getMediaSource().getId();
    }

    public final ArrayList<SubtitleStreamInfo> getSubtitleProfiles(boolean includeSelectedTrackOnly, String baseUrl, String accessToken) {
        return getSubtitleProfiles(includeSelectedTrackOnly, false, baseUrl, accessToken);
    }

    public final ArrayList<SubtitleStreamInfo> getSubtitleProfiles(boolean includeSelectedTrackOnly, boolean enableAllProfiles, String baseUrl, String accessToken) {
        ArrayList<SubtitleStreamInfo> list = new ArrayList<SubtitleStreamInfo>();

        if (!includeSelectedTrackOnly) {
            if (getMediaSource() == null) return list;

            for (org.jellyfin.sdk.model.api.MediaStream stream : getMediaSource().getMediaStreams()) {
                if (stream.getType() == org.jellyfin.sdk.model.api.MediaStreamType.SUBTITLE) {
                    SubtitleStreamInfo info = getSubtitleStreamInfo(stream, getDeviceProfile().getSubtitleProfiles());
                    list.add(info);
                }
            }
        }

        return list;
    }

    private SubtitleStreamInfo getSubtitleStreamInfo(org.jellyfin.sdk.model.api.MediaStream stream, SubtitleProfile[] subtitleProfiles) {
        SubtitleProfile subtitleProfile = StreamBuilder.getSubtitleProfile(stream, subtitleProfiles, getPlayMethod());
        SubtitleStreamInfo info = new SubtitleStreamInfo();
        String tempVar2 = stream.getLanguage();
        info.setName((tempVar2 != null) ? tempVar2 : "Unknown");
        info.setFormat(subtitleProfile.getFormat());
        info.setIndex(stream.getIndex());
        info.setDeliveryMethod(subtitleProfile.getMethod());
        info.setDisplayTitle(stream.getDisplayTitle());
        return info;
    }

    public final ArrayList<MediaStream> getSelectableAudioStreams() {
        return getSelectableStreams(MediaStreamType.AUDIO);
    }

    public final ArrayList<org.jellyfin.sdk.model.api.MediaStream> getSelectableStreams(MediaStreamType type) {
        ArrayList<org.jellyfin.sdk.model.api.MediaStream> list = new ArrayList<org.jellyfin.sdk.model.api.MediaStream>();

        for (org.jellyfin.sdk.model.api.MediaStream stream : getMediaSource().getMediaStreams()) {
            if (type == stream.getType()) {
                list.add(stream);
            }
        }

        return list;
    }
}
