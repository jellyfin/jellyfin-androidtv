package org.jellyfin.androidtv.data.compat;

import org.jellyfin.androidtv.constants.ContainerTypes;
import org.jellyfin.androidtv.constants.MediaTypes;
import org.jellyfin.androidtv.util.Utils;

import java.util.ArrayList;

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
import org.jellyfin.apiclient.model.mediainfo.TransportStreamTimestamp;
import org.jellyfin.apiclient.model.session.PlayMethod;

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

    private String SubProtocol;

    public final String getSubProtocol() {
        return SubProtocol;
    }

    public final void setSubProtocol(String value) {
        SubProtocol = value;
    }

    private long StartPositionTicks;

    public final long getStartPositionTicks() {
        return StartPositionTicks;
    }

    public final void setStartPositionTicks(long value) {
        StartPositionTicks = value;
    }

    private String VideoCodec;

    public final String getVideoCodec() {
        return VideoCodec;
    }

    public final void setVideoCodec(String value) {
        VideoCodec = value;
    }

    private String VideoProfile;

    public final String getVideoProfile() {
        return VideoProfile;
    }

    public final void setVideoProfile(String value) {
        VideoProfile = value;
    }

    private boolean CopyTimestamps;

    public final boolean getCopyTimestamps() {
        return CopyTimestamps;
    }

    public final void setCopyTimestamps(boolean value) {
        CopyTimestamps = value;
    }

    private boolean ForceLiveStream;

    public final boolean getForceLiveStream() {
        return ForceLiveStream;
    }

    public final void setForceLiveStream(boolean value) {
        ForceLiveStream = value;
    }

    private boolean EnableSubtitlesInManifest;

    public final boolean getEnableSubtitlesInManifest() {
        return EnableSubtitlesInManifest;
    }

    public final void setEnableSubtitlesInManifest(boolean value) {
        EnableSubtitlesInManifest = value;
    }

    private String[] AudioCodecs;

    public final String[] getAudioCodecs() {
        return AudioCodecs;
    }

    public final void setAudioCodecs(String[] value) {
        AudioCodecs = value;
    }

    private Integer AudioStreamIndex = null;

    public final Integer getAudioStreamIndex() {
        return AudioStreamIndex;
    }

    public final void setAudioStreamIndex(Integer value) {
        AudioStreamIndex = value;
    }

    private Integer SubtitleStreamIndex = null;

    public final Integer getSubtitleStreamIndex() {
        return SubtitleStreamIndex;
    }

    public final void setSubtitleStreamIndex(Integer value) {
        SubtitleStreamIndex = value;
    }

    private Integer TranscodingMaxAudioChannels = null;

    public final Integer getTranscodingMaxAudioChannels() {
        return TranscodingMaxAudioChannels;
    }

    public final void setTranscodingMaxAudioChannels(Integer value) {
        TranscodingMaxAudioChannels = value;
    }

    private Integer MaxAudioChannels = null;

    public final Integer getMaxAudioChannels() {
        return MaxAudioChannels;
    }

    public final void setMaxAudioChannels(Integer value) {
        MaxAudioChannels = value;
    }

    private Integer AudioBitrate = null;

    public final Integer getAudioBitrate() {
        return AudioBitrate;
    }

    public final void setAudioBitrate(Integer value) {
        AudioBitrate = value;
    }

    private Integer VideoBitrate = null;

    public final Integer getVideoBitrate() {
        return VideoBitrate;
    }

    public final void setVideoBitrate(Integer value) {
        VideoBitrate = value;
    }

    private Integer VideoLevel = null;

    public final Integer getVideoLevel() {
        return VideoLevel;
    }

    public final void setVideoLevel(Integer value) {
        VideoLevel = value;
    }

    private Integer MaxWidth = null;

    public final Integer getMaxWidth() {
        return MaxWidth;
    }

    public final void setMaxWidth(Integer value) {
        MaxWidth = value;
    }

    private Integer MaxHeight = null;

    public final Integer getMaxHeight() {
        return MaxHeight;
    }

    public final void setMaxHeight(Integer value) {
        MaxHeight = value;
    }

    private Integer MaxVideoBitDepth = null;

    public final Integer getMaxVideoBitDepth() {
        return MaxVideoBitDepth;
    }

    public final void setMaxVideoBitDepth(Integer value) {
        MaxVideoBitDepth = value;
    }

    private Integer MaxRefFrames = null;

    public final Integer getMaxRefFrames() {
        return MaxRefFrames;
    }

    public final void setMaxRefFrames(Integer value) {
        MaxRefFrames = value;
    }

    private Float MaxFramerate = null;

    public final Float getMaxFramerate() {
        return MaxFramerate;
    }

    public final void setMaxFramerate(Float value) {
        MaxFramerate = value;
    }

    private DeviceProfile DeviceProfile;

    public final DeviceProfile getDeviceProfile() {
        return DeviceProfile;
    }

    public final void setDeviceProfile(DeviceProfile value) {
        DeviceProfile = value;
    }

    private String DeviceProfileId;

    public final String getDeviceProfileId() {
        return DeviceProfileId;
    }

    public final void setDeviceProfileId(String value) {
        DeviceProfileId = value;
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

    public final void setTranscodeSeekInfo(TranscodeSeekInfo value) {
        TranscodeSeekInfo = value;
    }

    private boolean EstimateContentLength;

    public final boolean getEstimateContentLength() {
        return EstimateContentLength;
    }

    public final void setEstimateContentLength(boolean value) {
        EstimateContentLength = value;
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

    public final void setSubtitleDeliveryMethod(SubtitleDeliveryMethod value) {
        SubtitleDeliveryMethod = value;
    }

    private String SubtitleFormat;

    public final String getSubtitleFormat() {
        return SubtitleFormat;
    }

    public final void setSubtitleFormat(String value) {
        SubtitleFormat = value;
    }

    private String PlaySessionId;

    public final String getPlaySessionId() {
        return PlaySessionId;
    }

    public final void setPlaySessionId(String value) {
        PlaySessionId = value;
    }

    private ArrayList<MediaSourceInfo> AllMediaSources;

    public final ArrayList<MediaSourceInfo> getAllMediaSources() {
        return AllMediaSources;
    }

    public final void setAllMediaSources(ArrayList<MediaSourceInfo> value) {
        AllMediaSources = value;
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

    public final String ToDlnaUrl(String baseUrl, String accessToken) {
        if (getPlayMethod() == PlayMethod.DirectPlay) {
            return getMediaSource().getPath();
        }

        String dlnaCommand = BuildDlnaParam(this, accessToken);
        return GetUrl(baseUrl, dlnaCommand);
    }

    private String GetUrl(String baseUrl, String queryString) {
        if (Utils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException(baseUrl);
        }

        String extension = Utils.isEmpty(getContainer()) ? "" : "." + getContainer();

        // remove trailing slashes
        baseUrl = baseUrl.replaceAll("[/]+$", "");

        if (getMediaType() == DlnaProfileType.Audio) {
            if (MediaTypes.HLS.equalsIgnoreCase(getSubProtocol())) {
                return String.format("%1$s/audio/%2$s/master.m3u8?%3$s", baseUrl, getItemId(), queryString);
            }

            return String.format("%1$s/audio/%2$s/stream%3$s?%4$s", baseUrl, getItemId(), extension, queryString);
        }

        if (MediaTypes.HLS.equalsIgnoreCase(getSubProtocol())) {
            return String.format("%1$s/videos/%2$s/master.m3u8?%3$s", baseUrl, getItemId(), queryString);
        }

        return String.format("%1$s/videos/%2$s/stream%3$s?%4$s", baseUrl, getItemId(), extension, queryString);
    }

    private static String BuildDlnaParam(StreamInfo item, String accessToken) {
        ArrayList<String> list = new ArrayList<String>();

        for (NameValuePair pair : BuildParams(item, accessToken, true)) {
            list.add(pair.getValue());
        }

        return String.format("Params=%1$s", Utils.join(";", list));
    }

    private static ArrayList<NameValuePair> BuildParams(StreamInfo item, String accessToken, boolean isDlna) {
        ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

        String audioCodecs = item.getAudioCodecs().length == 0 ? "" : Utils.join(",", item.getAudioCodecs());

        String tempVar = item.getDeviceProfileId();
        list.add(new NameValuePair("DeviceProfileId", (tempVar != null) ? tempVar : ""));
        String tempVar2 = item.getDeviceId();
        list.add(new NameValuePair("DeviceId", (tempVar2 != null) ? tempVar2 : ""));
        String tempVar3 = item.getMediaSourceId();
        list.add(new NameValuePair("MediaSourceId", (tempVar3 != null) ? tempVar3 : ""));
        list.add(new NameValuePair(STATIC, Boolean.valueOf(item.getIsDirectStream()).toString().toLowerCase()));
        String tempVar4 = item.getVideoCodec();
        list.add(new NameValuePair("VideoCodec", (tempVar4 != null) ? tempVar4 : ""));
        list.add(new NameValuePair("AudioCodec", audioCodecs));
        list.add(new NameValuePair("AudioStreamIndex", item.getAudioStreamIndex() != null ? String.valueOf(item.getAudioStreamIndex()) : ""));
        list.add(new NameValuePair("SubtitleStreamIndex", item.getSubtitleStreamIndex() != null && item.getSubtitleDeliveryMethod() != org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod.External ? String.valueOf(item.getSubtitleStreamIndex()) : ""));
        list.add(new NameValuePair("VideoBitrate", item.getVideoBitrate() != null ? String.valueOf(item.getVideoBitrate()) : ""));
        list.add(new NameValuePair("AudioBitrate", item.getAudioBitrate() != null ? String.valueOf(item.getAudioBitrate()) : ""));
        list.add(new NameValuePair("MaxAudioChannels", item.getMaxAudioChannels() != null ? String.valueOf(item.getMaxAudioChannels()) : ""));
        list.add(new NameValuePair("MaxFramerate", item.getMaxFramerate() != null ? String.valueOf(item.getMaxFramerate()) : ""));
        list.add(new NameValuePair("MaxWidth", item.getMaxWidth() != null ? String.valueOf(item.getMaxWidth()) : ""));
        list.add(new NameValuePair("MaxHeight", item.getMaxHeight() != null ? String.valueOf(item.getMaxHeight()) : ""));

        if (MediaTypes.HLS.equalsIgnoreCase(item.getSubProtocol()) && !item.getForceLiveStream()) {
            list.add(new NameValuePair(START_TIME_TICKS, ""));
        } else {
            list.add(new NameValuePair(START_TIME_TICKS, String.valueOf(item.getStartPositionTicks())));
        }

        list.add(new NameValuePair("Level", item.getVideoLevel() != null ? String.valueOf(item.getVideoLevel()) : ""));

        list.add(new NameValuePair("MaxRefFrames", item.getMaxRefFrames() != null ? String.valueOf(item.getMaxRefFrames()) : ""));
        list.add(new NameValuePair("MaxVideoBitDepth", item.getMaxVideoBitDepth() != null ? String.valueOf(item.getMaxVideoBitDepth()) : ""));
        String tempVar5 = item.getVideoProfile();
        list.add(new NameValuePair("Profile", (tempVar5 != null) ? tempVar5 : ""));

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

        list.add(new NameValuePair("CopyTimestamps", Boolean.valueOf(item.getCopyTimestamps()).toString().toLowerCase()));
        list.add(new NameValuePair("ForceLiveStream", Boolean.valueOf(item.getForceLiveStream()).toString().toLowerCase()));
        list.add(new NameValuePair("SubtitleMethod", item.getSubtitleStreamIndex() != null && item.getSubtitleDeliveryMethod() != org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod.External ? item.getSubtitleDeliveryMethod().toString() : ""));

        list.add(new NameValuePair("TranscodingMaxAudioChannels", item.getTranscodingMaxAudioChannels() != null ? String.valueOf(item.getTranscodingMaxAudioChannels()) : ""));
        list.add(new NameValuePair("EnableSubtitlesInManifest", Boolean.valueOf(item.getEnableSubtitlesInManifest()).toString().toLowerCase()));

        String tempVar7 = item.getMediaSource().getETag();
        list.add(new NameValuePair("Tag", (tempVar7 != null) ? tempVar7 : ""));

        return list;
    }

    public final ArrayList<SubtitleStreamInfo> GetExternalSubtitles(boolean includeSelectedTrackOnly, String baseUrl, String accessToken) {
        return GetExternalSubtitles(includeSelectedTrackOnly, false, baseUrl, accessToken);
    }

    public final ArrayList<SubtitleStreamInfo> GetExternalSubtitles(boolean includeSelectedTrackOnly, boolean enableAllProfiles, String baseUrl, String accessToken) {
        ArrayList<SubtitleStreamInfo> list = GetSubtitleProfiles(includeSelectedTrackOnly, enableAllProfiles, baseUrl, accessToken);
        ArrayList<SubtitleStreamInfo> newList = new ArrayList<SubtitleStreamInfo>();

        // First add the selected track
        for (SubtitleStreamInfo stream : list) {
            if (stream.getDeliveryMethod() == SubtitleDeliveryMethod.External) {
                newList.add(stream);
            }
        }

        return newList;
    }

    public final ArrayList<SubtitleStreamInfo> GetSubtitleProfiles(boolean includeSelectedTrackOnly, String baseUrl, String accessToken) {
        return GetSubtitleProfiles(includeSelectedTrackOnly, false, baseUrl, accessToken);
    }

    public final ArrayList<SubtitleStreamInfo> GetSubtitleProfiles(boolean includeSelectedTrackOnly, boolean enableAllProfiles, String baseUrl, String accessToken) {
        ArrayList<SubtitleStreamInfo> list = new ArrayList<SubtitleStreamInfo>();

        // HLS will preserve timestamps so we can just grab the full subtitle stream
        long startPositionTicks = MediaTypes.HLS.equalsIgnoreCase(getSubProtocol()) ? 0 : (getPlayMethod() == PlayMethod.Transcode && !getCopyTimestamps() ? getStartPositionTicks() : 0);

        // First add the selected track
        if (getSubtitleStreamIndex() != null) {
            for (MediaStream stream : getMediaSource().getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Subtitle && stream.getIndex() == getSubtitleStreamIndex()) {
                    AddSubtitleProfiles(list, stream, enableAllProfiles, baseUrl, accessToken, startPositionTicks);
                }
            }
        }

        if (!includeSelectedTrackOnly) {
            for (MediaStream stream : getMediaSource().getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Subtitle && (getSubtitleStreamIndex() == null || stream.getIndex() != getSubtitleStreamIndex())) {
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
        tempVar.setIsForced(stream.getIsForced());
        tempVar.setLanguage(stream.getLanguage());
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

                info.setIsExternalUrl(false);
            } else {
                info.setUrl(stream.getPath());
                info.setIsExternalUrl(true);
            }
        }

        return info;
    }

    /**
     * Returns the audio stream that will be used
     */
    public final MediaStream getTargetAudioStream() {
        if (getMediaSource() != null) {
            return getMediaSource().GetDefaultAudioStream(getAudioStreamIndex());
        }

        return null;
    }

    /**
     * Returns the video stream that will be used
     */
    public final MediaStream getTargetVideoStream() {
        if (getMediaSource() != null) {
            return getMediaSource().getVideoStream();
        }

        return null;
    }

    /**
     * Predicts the audio sample rate that will be in the output stream
     */
    public final Integer getTargetAudioSampleRate() {
        MediaStream stream = getTargetAudioStream();
        return stream == null ? null : stream.getSampleRate();
    }

    /**
     * Predicts the audio sample rate that will be in the output stream
     */
    public final Integer getTargetVideoBitDepth() {
        MediaStream stream = getTargetVideoStream();
        return stream == null || !getIsDirectStream() ? null : stream.getBitDepth();
    }

    /**
     * Gets the target reference frames.
     *
     * <value>The target reference frames.</value>
     */
    public final Integer getTargetRefFrames() {
        MediaStream stream = getTargetVideoStream();
        return stream == null || !getIsDirectStream() ? null : stream.getRefFrames();
    }

    /**
     * Predicts the audio sample rate that will be in the output stream
     */
    public final Float getTargetFramerate() {
        MediaStream stream = getTargetVideoStream();
        Float tempVar = stream.getAverageFrameRate();
        return getMaxFramerate() != null && !getIsDirectStream() ? getMaxFramerate() : stream == null ? null : (tempVar != null) ? tempVar : stream.getRealFrameRate();
    }

    /**
     * Predicts the audio sample rate that will be in the output stream
     */
    public final Double getTargetVideoLevel() {
        MediaStream stream = getTargetVideoStream();
        return getVideoLevel() != null && !getIsDirectStream() ? getVideoLevel() : stream == null ? null : stream.getLevel();
    }

    /**
     * Predicts the audio sample rate that will be in the output stream
     */
    public final Integer getTargetPacketLength() {
        MediaStream stream = getTargetVideoStream();
        return !getIsDirectStream() ? null : stream == null ? null : stream.getPacketLength();
    }

    /**
     * Predicts the audio sample rate that will be in the output stream
     */
    public final String getTargetVideoProfile() {
        MediaStream stream = getTargetVideoStream();
        return !Utils.isEmpty(getVideoProfile()) && !getIsDirectStream() ? getVideoProfile() : stream == null ? null : stream.getProfile();
    }

    /**
     * Gets the target video codec tag.
     *
     * <value>The target video codec tag.</value>
     */
    public final String getTargetVideoCodecTag() {
        MediaStream stream = getTargetVideoStream();
        return !getIsDirectStream() ? null : stream == null ? null : stream.getCodecTag();
    }

    /**
     * Predicts the audio bitrate that will be in the output stream
     */
    public final Integer getTargetAudioBitrate() {
        MediaStream stream = getTargetAudioStream();
        return getAudioBitrate() != null && !getIsDirectStream() ? getAudioBitrate() : stream == null ? null : stream.getBitRate();
    }

    /**
     * Predicts the audio channels that will be in the output stream
     */
    public final Integer getTargetAudioChannels() {
        MediaStream stream = getTargetAudioStream();
        Integer streamChannels = stream == null ? null : stream.getChannels();

        if (getMaxAudioChannels() != null && !getIsDirectStream()) {
            if (streamChannels != null) {
                return Math.min(getMaxAudioChannels(), streamChannels);
            }

            return getMaxAudioChannels();
        }

        return streamChannels;
    }

    /**
     * Predicts the audio codec that will be in the output stream
     */
    public final String getTargetAudioCodec() {
        MediaStream stream = getTargetAudioStream();

        String inputCodec = stream == null ? null : stream.getCodec();

        if (getIsDirectStream()) {
            return inputCodec;
        }

        for (String codec : getAudioCodecs()) {
            if (codec.equalsIgnoreCase(inputCodec)) {
                return codec;
            }
        }

        return getAudioCodecs().length == 0 ? null : getAudioCodecs()[0];
    }

    /**
     * Predicts the audio channels that will be in the output stream
     */
    public final Long getTargetSize() {
        if (getIsDirectStream()) {
            return getMediaSource().getSize();
        }

        if (getRunTimeTicks() != null) {
            Integer totalBitrate = getTargetTotalBitrate();

            double totalSeconds = getRunTimeTicks();
            // Convert to ms
            totalSeconds /= 10000;
            // Convert to seconds
            totalSeconds /= 1000;

            return totalBitrate != null ? java.lang.Math.round(totalBitrate * totalSeconds) : (Long) null;
        }

        return null;
    }

    public final Integer getTargetVideoBitrate() {
        MediaStream stream = getTargetVideoStream();

        return getVideoBitrate() != null && !getIsDirectStream() ? getVideoBitrate() : stream == null ? null : stream.getBitRate();
    }

    public final TransportStreamTimestamp getTargetTimestamp() {
        TransportStreamTimestamp defaultValue = ContainerTypes.M2TS.equalsIgnoreCase(getContainer()) ? TransportStreamTimestamp.Valid : TransportStreamTimestamp.None;
        TransportStreamTimestamp tempVar = getMediaSource().getTimestamp();
        return !getIsDirectStream() ? defaultValue : getMediaSource() == null ? defaultValue : (tempVar != null) ? tempVar : TransportStreamTimestamp.None;
    }

    public final Integer getTargetTotalBitrate() {
        Integer tempVar = getTargetAudioBitrate();
        Integer tempVar2 = getTargetVideoBitrate();
        return ((tempVar != null) ? tempVar : 0) + ((tempVar2 != null) ? tempVar2 : 0);
    }

    public final Boolean getIsTargetAnamorphic() {
        if (getIsDirectStream()) {
            return getTargetVideoStream() == null ? null : getTargetVideoStream().getIsAnamorphic();
        }

        return false;
    }

    public final Integer getTargetWidth() {
        MediaStream videoStream = getTargetVideoStream();

        if (videoStream != null && videoStream.getWidth() != null && videoStream.getHeight() != null) {
            ImageSize tempVar = new ImageSize();
            tempVar.setWidth(videoStream.getWidth());
            tempVar.setHeight(videoStream.getHeight());
            ImageSize size = tempVar.clone();

            Double maxWidth = getMaxWidth() != null ? (double) getMaxWidth() : (Double) null;
            Double maxHeight = getMaxHeight() != null ? (double) getMaxHeight() : (Double) null;

            ImageSize newSize = DrawingUtils.Resize(size.clone(), null, null, maxWidth, maxHeight);

            return (int) newSize.getWidth();
        }

        return getMaxWidth();
    }

    public final Integer getTargetHeight() {
        MediaStream videoStream = getTargetVideoStream();

        if (videoStream != null && videoStream.getWidth() != null && videoStream.getHeight() != null) {
            ImageSize tempVar = new ImageSize();
            tempVar.setWidth(videoStream.getWidth());
            tempVar.setHeight(videoStream.getHeight());
            ImageSize size = tempVar.clone();

            Double maxWidth = getMaxWidth() != null ? (double) getMaxWidth() : (Double) null;
            Double maxHeight = getMaxHeight() != null ? (double) getMaxHeight() : (Double) null;

            ImageSize newSize = DrawingUtils.Resize(size.clone(), null, null, maxWidth, maxHeight);

            return (int) newSize.getHeight();
        }

        return getMaxHeight();
    }

    public final Integer getTargetVideoStreamCount() {
        if (getIsDirectStream()) {
            return GetMediaStreamCount(MediaStreamType.Video, Integer.MAX_VALUE);
        }
        return GetMediaStreamCount(MediaStreamType.Video, 1);
    }

    public final Integer getTargetAudioStreamCount() {
        if (getIsDirectStream()) {
            return GetMediaStreamCount(MediaStreamType.Audio, Integer.MAX_VALUE);
        }
        return GetMediaStreamCount(MediaStreamType.Audio, 1);
    }

    private Integer GetMediaStreamCount(MediaStreamType type, int limit) {
        Integer count = getMediaSource().GetStreamCount(type);

        if (count != null) {
            count = Math.min(count, limit);
        }

        return count;
    }

    public final ArrayList<MediaStream> GetSelectableAudioStreams() {
        return GetSelectableStreams(MediaStreamType.Audio);
    }

    public final ArrayList<MediaStream> GetSelectableSubtitleStreams() {
        return GetSelectableStreams(MediaStreamType.Subtitle);
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
