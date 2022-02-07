package org.jellyfin.androidtv.ui.playback;

import org.jellyfin.androidtv.data.compat.AudioOptions;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.compat.VideoOptions;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.sdk.model.DeviceInfo;

import timber.log.Timber;

@Deprecated
public class StopTranscodingResponse extends EmptyResponse {
    private PlaybackManager playbackManager;
    private final DeviceInfo deviceInfo;
    private VideoOptions options;
    private Response<StreamInfo> response;
    private Long startPositionTicks;
    private ApiClient apiClient;

    public StopTranscodingResponse(PlaybackManager playbackManager, DeviceInfo deviceInfo, VideoOptions options, Long startPositionTicks, ApiClient apiClient, Response<StreamInfo> response) {
        this.playbackManager = playbackManager;
        this.deviceInfo = deviceInfo;
        this.options = options;
        this.response = response;
        this.startPositionTicks = startPositionTicks;
        this.apiClient = apiClient;
    }

    private void onAny() {
        playbackManager.getVideoStreamInfo(deviceInfo, options, startPositionTicks, apiClient, response);
    }

    @Override
    public void onResponse() {
        onAny();
    }

    @Override
    public void onError(Exception ex) {
        Timber.e(ex, "Error in StopTranscodingProcesses");
        onAny();
    }
}
