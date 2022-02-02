package org.jellyfin.androidtv.data.compat;

import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.DlnaProfileType;
import org.jellyfin.apiclient.model.dlna.EncodingContext;
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;
import org.jellyfin.apiclient.model.dlna.SubtitleProfile;
import org.jellyfin.apiclient.model.dlna.TranscodeSeekInfo;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.dto.NameValuePair;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.MediaStreamType;
import org.jellyfin.apiclient.model.mediainfo.MediaProtocol;
import org.jellyfin.apiclient.model.session.PlayMethod;

import java.util.ArrayList;

/**
 * Class StreamInfo.
 *
 * @deprecated
 */
@Deprecated
public class StreamInfo {
    private static final String START_TIME_TICKS = "StartTimeTicks";
    private static final String SUBTITLE_STREAM_INDEX = "SubtitleStreamIndex";
    private static final String STATIC = "Static";

    public StreamInfo() {
        setAudioCodecs(new String[]{});
    }

    private String ItemId;

    public final String getItemId() {
        return ItemId;
    }

    public final void setItemId(String value) {
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

    private DlnaProfileType MediaType = DlnaProfileType.values()[0];

    public final DlnaProfileType getMediaType() {
        return MediaType;
    }

    public final void setMediaType(DlnaProfileType value) {
        MediaType = value;
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

    private String[] AudioCodecs;

    public final String[] getAudioCodecs() {
        return AudioCodecs;
    }

    public final void setAudioCodecs(String[] value) {
        AudioCodecs = value;
    }

    private DeviceProfile DeviceProfile;

    public final DeviceProfile getDeviceProfile() {
        return DeviceProfile;
    }

    public final void setDeviceProfile(DeviceProfile value) {
        DeviceProfile = value;
    }

    private String DeviceId;

    public final String getDeviceId() {
        return DeviceId;
    }

    public final void setDeviceId(String value) {
        DeviceId = value;
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

    public final boolean getIsDirectStream() {
        return getPlayMethod() == PlayMethod.DirectStream || getPlayMethod() == PlayMethod.DirectPlay;
    }

    public final String ToUrl(String baseUrl, String accessToken) {
        if (!Utils.isEmpty(getMediaUrl())) {
            return getMediaUrl();
        }

        if (getPlayMethod() == PlayMethod.DirectPlay) {
            return getMediaSource().getPath();
        }

        if (Utils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException(baseUrl);
        }

        ArrayList<String> list = new ArrayList<String>();
        for (NameValuePair pair : BuildParams(this, accessToken, false)) {
            if (Utils.isEmpty(pair.getValue())) {
                continue;
            }

            // Try to keep the url clean by omitting defaults
            if (START_TIME_TICKS.equalsIgnoreCase(pair.getName()) && "0".equalsIgnoreCase(pair.getValue())) {
                continue;
            }
            if (SUBTITLE_STREAM_INDEX.equalsIgnoreCase(pair.getName()) && "-1".equalsIgnoreCase(pair.getValue())) {
                continue;
            }
            if (STATIC.equalsIgnoreCase(pair.getName()) && "false".equalsIgnoreCase(pair.getValue())) {
                continue;
            }

            list.add(String.format("%1$s=%2$s", pair.getName(), pair.getValue()));
        }

        String queryString = Utils.join("&", list);

        return GetUrl(baseUrl, queryString);
    }

    private String GetUrl(String baseUrl, String queryString) {
        if (Utils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException(baseUrl);
        }

        String extension = Utils.isEmpty(getContainer()) ? "" : "." + getContainer();

        // remove trailing slashes
        baseUrl = baseUrl.replaceAll("[/]+$", "");

        if (getMediaType() == DlnaProfileType.Audio) {
            return String.format("%1$s/audio/%2$s/stream%3$s?%4$s", baseUrl, getItemId(), extension, queryString);
        }

        return String.format("%1$s/videos/%2$s/stream%3$s?%4$s", baseUrl, getItemId(), extension, queryString);
    }

    private static ArrayList<NameValuePair> BuildParams(StreamInfo item, String accessToken, boolean isDlna) {
        ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

        String audioCodecs = item.getAudioCodecs().length == 0 ? "" : Utils.join(",", item.getAudioCodecs());

        list.add(new NameValuePair("DeviceProfileId", ""));
        String tempVar2 = item.getDeviceId();
        list.add(new NameValuePair("DeviceId", (tempVar2 != null) ? tempVar2 : ""));
        String tempVar3 = item.getMediaSourceId();
        list.add(new NameValuePair("MediaSourceId", (tempVar3 != null) ? tempVar3 : ""));
        list.add(new NameValuePair(STATIC, Boolean.valueOf(item.getIsDirectStream()).toString().toLowerCase()));
        list.add(new NameValuePair("VideoCodec", ""));
        list.add(new NameValuePair("AudioCodec", audioCodecs));
        list.add(new NameValuePair("AudioStreamIndex", ""));
        list.add(new NameValuePair("SubtitleStreamIndex", ""));
        list.add(new NameValuePair("VideoBitrate", ""));
        list.add(new NameValuePair("AudioBitrate", ""));
        list.add(new NameValuePair("MaxAudioChannels", ""));
        list.add(new NameValuePair("MaxFramerate", ""));
        list.add(new NameValuePair("MaxWidth", ""));
        list.add(new NameValuePair("MaxHeight", ""));

        list.add(new NameValuePair(START_TIME_TICKS, String.valueOf(item.getStartPositionTicks())));

        list.add(new NameValuePair("Level", ""));

        list.add(new NameValuePair("MaxRefFrames", ""));
        list.add(new NameValuePair("MaxVideoBitDepth", ""));
        list.add(new NameValuePair("Profile", ""));

        // no longer used
        list.add(new NameValuePair("Cabac", ""));

        String tempVar6 = item.getPlaySessionId();
        list.add(new NameValuePair("PlaySessionId", (tempVar6 != null) ? tempVar6 : ""));
        list.add(new NameValuePair("api_key", (accessToken != null) ? accessToken : ""));

        String liveStreamId = item.getMediaSource() == null ? null : item.getMediaSource().getLiveStreamId();
        list.add(new NameValuePair("LiveStreamId", (liveStreamId != null) ? liveStreamId : ""));

        if (isDlna) {
            list.add(new NameValuePair("ItemId", item.getItemId()));
        }

        list.add(new NameValuePair("CopyTimestamps", "false"));
        list.add(new NameValuePair("ForceLiveStream", "false"));
        list.add(new NameValuePair("SubtitleMethod", ""));

        list.add(new NameValuePair("TranscodingMaxAudioChannels", ""));
        list.add(new NameValuePair("EnableSubtitlesInManifest", "false"));

        String tempVar7 = item.getMediaSource().getETag();
        list.add(new NameValuePair("Tag", (tempVar7 != null) ? tempVar7 : ""));

        return list;
    }

    public final ArrayList<SubtitleStreamInfo> GetSubtitleProfiles(boolean includeSelectedTrackOnly, String baseUrl, String accessToken) {
        return GetSubtitleProfiles(includeSelectedTrackOnly, false, baseUrl, accessToken);
    }

    public final ArrayList<SubtitleStreamInfo> GetSubtitleProfiles(boolean includeSelectedTrackOnly, boolean enableAllProfiles, String baseUrl, String accessToken) {
        ArrayList<SubtitleStreamInfo> list = new ArrayList<SubtitleStreamInfo>();

        // HLS will preserve timestamps so we can just grab the full subtitle stream
        long startPositionTicks = getPlayMethod() == PlayMethod.Transcode ? getStartPositionTicks() : 0;

        if (!includeSelectedTrackOnly) {
            for (MediaStream stream : getMediaSource().getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Subtitle) {
                    AddSubtitleProfiles(list, stream, enableAllProfiles, baseUrl, accessToken, startPositionTicks);
                }
            }
        }

        return list;
    }

    private void AddSubtitleProfiles(ArrayList<SubtitleStreamInfo> list, MediaStream stream, boolean enableAllProfiles, String baseUrl, String accessToken, long startPositionTicks) {
        if (enableAllProfiles) {
            for (SubtitleProfile profile : getDeviceProfile().getSubtitleProfiles()) {
                SubtitleStreamInfo info = GetSubtitleStreamInfo(stream, baseUrl, accessToken, startPositionTicks, new SubtitleProfile[]{profile});

                list.add(info);
            }
        } else {
            SubtitleStreamInfo info = GetSubtitleStreamInfo(stream, baseUrl, accessToken, startPositionTicks, getDeviceProfile().getSubtitleProfiles());

            list.add(info);
        }
    }

    private SubtitleStreamInfo GetSubtitleStreamInfo(MediaStream stream, String baseUrl, String accessToken, long startPositionTicks, SubtitleProfile[] subtitleProfiles) {
        SubtitleProfile subtitleProfile = StreamBuilder.GetSubtitleProfile(stream, subtitleProfiles, getPlayMethod());
        SubtitleStreamInfo tempVar = new SubtitleStreamInfo();
        String tempVar2 = stream.getLanguage();
        tempVar.setName((tempVar2 != null) ? tempVar2 : "Unknown");
        tempVar.setFormat(subtitleProfile.getFormat());
        tempVar.setIndex(stream.getIndex());
        tempVar.setDeliveryMethod(subtitleProfile.getMethod());
        tempVar.setDisplayTitle(stream.getDisplayTitle());
        SubtitleStreamInfo info = tempVar;

        if (info.getDeliveryMethod() == SubtitleDeliveryMethod.External) {
            if (getMediaSource().getProtocol() == MediaProtocol.File || !stream.getCodec().equalsIgnoreCase(subtitleProfile.getFormat())) {
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

    public final ArrayList<MediaStream> GetSelectableAudioStreams() {
        return GetSelectableStreams(MediaStreamType.Audio);
    }

    public final ArrayList<MediaStream> GetSelectableStreams(MediaStreamType type) {
        ArrayList<MediaStream> list = new ArrayList<MediaStream>();

        for (MediaStream stream : getMediaSource().getMediaStreams()) {
            if (type == stream.getType()) {
                list.add(stream);
            }
        }

        return list;
    }
}
