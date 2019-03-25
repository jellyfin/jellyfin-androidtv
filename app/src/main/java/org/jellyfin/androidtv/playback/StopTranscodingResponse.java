package org.jellyfin.androidtv.playback;

import org.jellyfin.androidtv.model.compat.AudioOptions;
import org.jellyfin.androidtv.model.compat.StreamInfo;
import org.jellyfin.androidtv.model.compat.VideoOptions;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.logging.ILogger;

@Deprecated
public class StopTranscodingResponse extends EmptyResponse {
    private PlaybackManager playbackManager;
    private String serverId;
    private StreamInfo currentStreamInfo;
    private AudioOptions options;
    private ILogger logger;
    private Response<StreamInfo> response;
    private Long startPositionTicks;
    private ApiClient apiClient;

    public StopTranscodingResponse(PlaybackManager playbackManager, String serverId, StreamInfo currentStreamInfo, AudioOptions options, ILogger logger, Long startPositionTicks, ApiClient apiClient, Response<StreamInfo> response) {
        this.playbackManager = playbackManager;
        this.serverId = serverId;
        this.currentStreamInfo = currentStreamInfo;
        this.options = options;
        this.logger = logger;
        this.response = response;
        this.startPositionTicks = startPositionTicks;
        this.apiClient = apiClient;
    }

    private void onAny() {
        playbackManager.getVideoStreamInfo(serverId, (VideoOptions) options, startPositionTicks, false, apiClient, response);
    }

    @Override
    public void onResponse() {
        onAny();
    }

    @Override
    public void onError(Exception ex) {
        logger.ErrorException("Error in StopTranscodingProcesses", ex);
        onAny();
    }
}
