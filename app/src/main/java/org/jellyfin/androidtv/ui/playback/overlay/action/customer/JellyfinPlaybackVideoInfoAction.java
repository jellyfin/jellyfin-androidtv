package org.jellyfin.androidtv.ui.playback.overlay.action.customer;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.customer.action.PlaybackInfoActionComponent;
import org.jellyfin.androidtv.customer.common.CustomerCommonUtils;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter;
import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;
import org.jellyfin.sdk.model.api.MediaSourceInfo;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JellyfinPlaybackVideoInfoAction  extends CustomAction implements PlaybackInfoActionComponent {
    private final PlaybackController playbackController;
    private final Context context;

    public JellyfinPlaybackVideoInfoAction(@NonNull Context context, @NonNull CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue, PlaybackController playbackController) {
        super(context, customPlaybackTransportControlGlue);
        this.playbackController = playbackController;
        this.context = context;

        initializeWithIcon(R.drawable.info);
    }

    @Override
    public void handleClickAction(@NonNull PlaybackController playbackController, @NonNull VideoPlayerAdapter videoPlayerAdapter, @NonNull Context context, @NonNull View view) {
        doClick(context, view);
    }


    @Override
    public Map<String, Map<String, String>> getVideoInfos() {
        StreamInfo streamInfo = playbackController.getCurrentStreamInfo();
        if (streamInfo == null || streamInfo.getMediaSource() == null || streamInfo.getMediaSource().getMediaStreams() == null) {
            return null;
        }

        MediaSourceInfo mediaSource = streamInfo.getMediaSource();
        int audioIndex = playbackController.getmDefaultAudioIndex();
        if (audioIndex == -1) {
            Integer defaultAudioStreamIndex = streamInfo.getMediaSource().getDefaultAudioStreamIndex();
            audioIndex = defaultAudioStreamIndex == null ? -1 : defaultAudioStreamIndex;
        }

        MediaStream videoMediaStream = null;
        MediaStream audioMediaStream = null;
        for (MediaStream mediaStream : mediaSource.getMediaStreams()) {
            if (MediaStreamType.VIDEO.equals(mediaStream.getType())) {
                videoMediaStream = mediaStream;
            } else if (MediaStreamType.AUDIO.equals(mediaStream.getType())) {
                if (audioIndex != -1 && Objects.equals(mediaStream.getIndex(), audioIndex)) {
                    audioMediaStream = mediaStream;
                } else if (audioIndex == -1) {
                    audioMediaStream = mediaStream;
                }
            }

            if (videoMediaStream != null && audioMediaStream != null) {
                break;
            }
        }
        // 过滤视频不为空
        if (videoMediaStream == null) {
            return null;
        }

        Map<String, Map<String, String>> resultMap = new LinkedHashMap<>();
        // 播放器信息
        Map<String, String> playInfoMap = new LinkedHashMap<>();
        // 设备信息
//        DeviceProfile deviceProfile = streamInfo.d();
//        playInfoMap.put(context.getString(R.string.LabelPlayer), deviceProfile.getName());
        playInfoMap.put(context.getString(R.string.LabelPlayMethod), streamInfo.getPlayMethod().name());
        playInfoMap.put(context.getString(R.string.LabelProtocol), mediaSource.getTranscodingSubProtocol().getSerialName());
        resultMap.put(context.getString(R.string.label_playback_info), playInfoMap);

        // 视频信息
        Map<String, String> videoInfoMap = new LinkedHashMap<>();
        DisplayMetrics resolutionRatio = getResolutionRatio(context);
        if (resolutionRatio != null) {
            videoInfoMap.put(context.getString(R.string.LabelRealPlayerDimensions), resolutionRatio.widthPixels + "x" + resolutionRatio.heightPixels);
        }
        {
            if (context instanceof Activity) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                videoInfoMap.put(context.getString(R.string.LabelPlayerDimensions), displayMetrics.widthPixels + "x" + displayMetrics.heightPixels);
            }
        }

        videoInfoMap.put(context.getString(R.string.LabelVideoResolution), videoMediaStream.getWidth() + "x" + videoMediaStream.getHeight());
        videoInfoMap.put(context.getString(R.string.media_info_size), CustomerCommonUtils.getSuitableSize(mediaSource.getSize(), "B"));
        videoInfoMap.put(context.getString(R.string.LabelVideoFrameRate), String.valueOf(videoMediaStream.getRealFrameRate()));
        videoInfoMap.put(context.getString(R.string.LabelVideoAgvFrameRate), String.valueOf(videoMediaStream.getAverageFrameRate()));
        resultMap.put(context.getString(R.string.label_video_info), videoInfoMap);

        // 媒体信息
        Map<String, String> orginMediaInfoMap = new LinkedHashMap<>();
        // 视频编码
        if (videoMediaStream.getCodec() != null) {
            orginMediaInfoMap.put(context.getString(R.string.LabelVideoCodec), videoMediaStream.getCodec().toUpperCase() + " " + videoMediaStream.getProfile());
        }

        // 视频码率
        orginMediaInfoMap.put(context.getString(R.string.LabelVideoBitrate), CustomerCommonUtils.getSuitableSize(mediaSource.getBitrate(), "bps"));
        // 视频比特率
//        orginMediaInfoMap.put(context.getString(R.string.LabelAudioBitrate), CustomerCommonUtils.getSuitableSize(videoMediaStream.getBitRate(), "bps"));
        if (audioMediaStream != null) {
            // 音频比特率
            if (audioMediaStream.getCodec() != null) {
                orginMediaInfoMap.put(context.getString(R.string.LabelAudioCodec), audioMediaStream.getCodec().toUpperCase());
            }
            orginMediaInfoMap.put(context.getString(R.string.LabelAudioBitrate), CustomerCommonUtils.getSuitableSize(audioMediaStream.getBitRate(), "bps"));
            if (audioMediaStream.getChannels() != null) {
                orginMediaInfoMap.put(context.getString(R.string.LabelAudioChannels), audioMediaStream.getChannels().toString());
            }
            orginMediaInfoMap.put(context.getString(R.string.media_info_sampleRate), audioMediaStream.getSampleRate() + "Hz");
        }

        videoInfoMap.put(context.getString(R.string.media_info_container), streamInfo.getContainer());
        videoInfoMap.put(context.getString(R.string.media_info_path), mediaSource.getPath());
        resultMap.put(context.getString(R.string.label_original_media_info), orginMediaInfoMap);

        return resultMap;
    }

}
