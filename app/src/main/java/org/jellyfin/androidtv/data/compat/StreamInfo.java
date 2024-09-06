package org.jellyfin.androidtv.data.compat;

import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.EncodingContext;
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;
import org.jellyfin.apiclient.model.dlna.SubtitleProfile;
import org.jellyfin.apiclient.model.dlna.TranscodeSeekInfo;
import org.jellyfin.apiclient.model.session.PlayMethod;
import org.jellyfin.sdk.model.api.MediaProtocol;
import org.jellyfin.sdk.model.api.MediaSourceInfo;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;

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

    private PlayMethod PlayMethod = getPlayMethod().values()[0];

    public final PlayMethod getPlayMethod() {
        return PlayMethod;
    }

    public final void setPlayMethod(PlayMethod value) {
        PlayMethod = value;
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

    private long StartPositionTicks;

    public final long getStartPositionTicks() {
        return StartPositionTicks;
    }

    public final void setStartPositionTicks(long value) {
        StartPositionTicks = value;
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

    private SubtitleDeliveryMethod SubtitleDeliveryMethod = getSubtitleDeliveryMethod().values()[0];

    public final SubtitleDeliveryMethod getSubtitleDeliveryMethod() {
        return SubtitleDeliveryMethod;
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

        // HLS will preserve timestamps so we can just grab the full subtitle stream
        long startPositionTicks = getPlayMethod() == PlayMethod.Transcode ? getStartPositionTicks() : 0;

        if (!includeSelectedTrackOnly) {
            if (getMediaSource() == null) return list;

            for (org.jellyfin.sdk.model.api.MediaStream stream : getMediaSource().getMediaStreams()) {
                if (stream.getType() == org.jellyfin.sdk.model.api.MediaStreamType.SUBTITLE) {
                    addSubtitleProfiles(list, stream, enableAllProfiles, baseUrl, accessToken, startPositionTicks);
                }
            }
        }

        return list;
    }

    private void addSubtitleProfiles(ArrayList<SubtitleStreamInfo> list, org.jellyfin.sdk.model.api.MediaStream stream, boolean enableAllProfiles, String baseUrl, String accessToken, long startPositionTicks) {
        if (enableAllProfiles) {
            for (SubtitleProfile profile : getDeviceProfile().getSubtitleProfiles()) {
                SubtitleStreamInfo info = getSubtitleStreamInfo(stream, baseUrl, accessToken, startPositionTicks, new SubtitleProfile[]{profile});

                list.add(info);
            }
        } else {
            SubtitleStreamInfo info = getSubtitleStreamInfo(stream, baseUrl, accessToken, startPositionTicks, getDeviceProfile().getSubtitleProfiles());

            list.add(info);
        }
    }

    private SubtitleStreamInfo getSubtitleStreamInfo(org.jellyfin.sdk.model.api.MediaStream stream, String baseUrl, String accessToken, long startPositionTicks, SubtitleProfile[] subtitleProfiles) {
        SubtitleProfile subtitleProfile = StreamBuilder.getSubtitleProfile(stream, subtitleProfiles, getPlayMethod());
        SubtitleStreamInfo tempVar = new SubtitleStreamInfo();
        String tempVar2 = stream.getLanguage();
        tempVar.setName((tempVar2 != null) ? tempVar2 : "Unknown");
        tempVar.setFormat(subtitleProfile.getFormat());
        tempVar.setIndex(stream.getIndex());
        tempVar.setDeliveryMethod(subtitleProfile.getMethod());
        tempVar.setDisplayTitle(stream.getDisplayTitle());
        SubtitleStreamInfo info = tempVar;

        if (info.getDeliveryMethod() == SubtitleDeliveryMethod.External) {
            if (getMediaSource().getProtocol() == MediaProtocol.FILE || !stream.getCodec().equalsIgnoreCase(subtitleProfile.getFormat())) {
                info.setUrl(String.format("%1$s/Videos/%2$s/%3$s/Subtitles/%4$s/%5$s/Stream.%6$s", baseUrl, getItemId(), getMediaSourceId(), String.valueOf(stream.getIndex()), String.valueOf(startPositionTicks), subtitleProfile.getFormat()));

                if (!Utils.isEmpty(accessToken)) {
                    info.setUrl(info.getUrl() + "?api_key=" + accessToken);
                }
            } else {
                info.setUrl(stream.getPath());
            }
        }

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
