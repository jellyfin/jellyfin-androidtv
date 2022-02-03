package org.jellyfin.androidtv.ui.playback;

import org.jellyfin.androidtv.data.compat.AudioOptions;
import org.jellyfin.androidtv.data.compat.PlaybackException;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.compat.VideoOptions;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.interaction.device.IDevice;
import org.jellyfin.apiclient.logging.ILogger;
import org.jellyfin.apiclient.model.dlna.PlaybackErrorCode;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.mediainfo.PlaybackInfoRequest;
import org.jellyfin.apiclient.model.session.PlaybackProgressInfo;
import org.jellyfin.apiclient.model.session.PlaybackStartInfo;
import org.jellyfin.apiclient.model.session.PlaybackStopInfo;

import java.util.ArrayList;

/**
 * Reimplementation of the PlaybackManager class from the apiclient with local item support removed.
 *
 * @deprecated
 */
@Deprecated
public class PlaybackManager {
    private final ILogger logger;
    private final IDevice device;

    public PlaybackManager(IDevice device, ILogger logger) {
        this.device = device;
        this.logger = logger;
    }

    public ArrayList<MediaStream> getInPlaybackSelectableAudioStreams(StreamInfo info) {
        return info.GetSelectableAudioStreams();
    }

    void SendResponse(Response<StreamInfo> response, StreamInfo info) {

        if (info == null) {
            PlaybackException error = new PlaybackException();
            error.setErrorCode(PlaybackErrorCode.NoCompatibleStream);
            response.onError(error);
        } else {
            response.onResponse(info);
        }
    }

    public void getAudioStreamInfo(String serverId, AudioOptions options, Long startPositionTicks, ApiClient apiClient, Response<StreamInfo> response) {
        PlaybackInfoRequest request = new PlaybackInfoRequest();
        request.setId(options.getItemId());
        request.setUserId(apiClient.getCurrentUserId());
        request.setMaxStreamingBitrate(Long.valueOf(options.getMaxBitrate()));
        request.setMediaSourceId(options.getMediaSourceId());
        request.setStartTimeTicks(startPositionTicks);
        request.setDeviceProfile(options.getProfile());
        request.setMaxAudioChannels(options.getMaxAudioChannels());

        apiClient.GetPlaybackInfoWithPost(request, new GetPlaybackInfoResponse(this, apiClient, options, response, false, startPositionTicks));
    }

    public void getVideoStreamInfo(final String serverId, final VideoOptions options, Long startPositionTicks, ApiClient apiClient, final Response<StreamInfo> response) {
        PlaybackInfoRequest request = new PlaybackInfoRequest();
        request.setId(options.getItemId());
        request.setUserId(apiClient.getCurrentUserId());
        request.setMaxStreamingBitrate(Long.valueOf(options.getMaxBitrate()));
        request.setMediaSourceId(options.getMediaSourceId());
        request.setAudioStreamIndex(options.getAudioStreamIndex());
        request.setSubtitleStreamIndex(options.getSubtitleStreamIndex());
        request.setStartTimeTicks(startPositionTicks);
        request.setDeviceProfile(options.getProfile());
        request.setEnableDirectStream(options.getEnableDirectStream());
        request.setEnableDirectPlay(options.getEnableDirectPlay());
        request.setMaxAudioChannels(options.getMaxAudioChannels());

        apiClient.GetPlaybackInfoWithPost(request, new GetPlaybackInfoResponse(this, apiClient, options, response, true, startPositionTicks));

    }

    public void changeVideoStream(final StreamInfo currentStreamInfo, final String serverId, final VideoOptions options, Long startPositionTicks, ApiClient apiClient, final Response<StreamInfo> response) {
        String playSessionId = currentStreamInfo.getPlaySessionId();

        apiClient.StopTranscodingProcesses(device.getDeviceId(), playSessionId, new StopTranscodingResponse(this, serverId, currentStreamInfo, options, logger, startPositionTicks, apiClient, response));
    }

    public void reportPlaybackStart(PlaybackStartInfo info, ApiClient apiClient, EmptyResponse response) {
        apiClient.ReportPlaybackStartAsync(info, response);
    }

    public void reportPlaybackProgress(PlaybackProgressInfo info, final StreamInfo streamInfo, ApiClient apiClient, EmptyResponse response) {
        MediaSourceInfo mediaSource = streamInfo.getMediaSource();

        if (mediaSource != null) {
            info.setLiveStreamId(mediaSource.getLiveStreamId());
        }

        info.setPlaySessionId(streamInfo.getPlaySessionId());

        apiClient.ReportPlaybackProgressAsync(info, response);
    }

    public void reportPlaybackStopped(PlaybackStopInfo info, final StreamInfo streamInfo, final String serverId, String userId, final ApiClient apiClient, final EmptyResponse response) {
        MediaSourceInfo mediaSource = streamInfo.getMediaSource();

        if (mediaSource != null) {
            info.setLiveStreamId(mediaSource.getLiveStreamId());
        }

        info.setPlaySessionId(streamInfo.getPlaySessionId());

        apiClient.ReportPlaybackStoppedAsync(info, response);
    }
}
