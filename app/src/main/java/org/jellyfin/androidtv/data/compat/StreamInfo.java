package org.jellyfin.androidtv.data.compat;

import org.jellyfin.sdk.model.api.MediaSourceInfo;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;
import org.jellyfin.sdk.model.api.PlayMethod;
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

    private PlayMethod playMethod = PlayMethod.DIRECT_PLAY;

    public final PlayMethod getPlayMethod() {
        return playMethod;
    }

    public final void setPlayMethod(PlayMethod value) {
        playMethod = value;
    }

    private String Container;

    public final String getContainer() {
        return Container;
    }

    public final void setContainer(String value) {
        Container = value;
    }

    private Long RunTimeTicks = null;

    public final Long getRunTimeTicks() {
        return RunTimeTicks;
    }

    public final void setRunTimeTicks(Long value) {
        RunTimeTicks = value;
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
