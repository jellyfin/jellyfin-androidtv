package org.jellyfin.androidtv.ui.playback;

import org.jellyfin.androidtv.data.compat.PlaybackException;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.compat.VideoOptions;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dlna.PlaybackErrorCode;
import org.jellyfin.apiclient.model.mediainfo.PlaybackInfoRequest;
import org.jellyfin.sdk.model.DeviceInfo;
import org.jellyfin.sdk.model.api.MediaStream;

import java.util.ArrayList;

public class PlaybackManager {
    private final org.jellyfin.sdk.api.client.ApiClient api;

    public PlaybackManager(org.jellyfin.sdk.api.client.ApiClient api) {
        this.api = api;
    }

    public ArrayList<MediaStream> getInPlaybackSelectableAudioStreams(StreamInfo info) {
        if (info == null)
            return null;
        return info.getSelectableAudioStreams();
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

    public void getVideoStreamInfo(DeviceInfo deviceInfo, final VideoOptions options, Long startPositionTicks, ApiClient apiClient, final Response<StreamInfo> response) {
        PlaybackInfoRequest request = new PlaybackInfoRequest();
        request.setId(options.getItemId().toString());
        request.setUserId(apiClient.getCurrentUserId());
        request.setMaxStreamingBitrate(Long.valueOf(options.getMaxBitrate()));
        request.setMediaSourceId(options.getMediaSourceId());
        request.setStartTimeTicks(startPositionTicks);
        request.setDeviceProfile(options.getProfile());
        request.setEnableDirectStream(options.getEnableDirectStream());
        request.setEnableDirectPlay(options.getEnableDirectPlay());
        request.setMaxAudioChannels(options.getMaxAudioChannels());

        Integer audioIdx = options.getAudioStreamIndex();
        if (audioIdx != null && audioIdx >= 0) {
            request.setAudioStreamIndex(audioIdx);
        }
        Integer subIdx = options.getSubtitleStreamIndex();
        if (subIdx != null) request.setSubtitleStreamIndex(subIdx);

        request.setAllowVideoStreamCopy(true);
        request.setAllowAudioStreamCopy(true);

        apiClient.GetPlaybackInfoWithPost(request, new GetPlaybackInfoResponse(this, deviceInfo, apiClient, options, response, true));
    }

    public void changeVideoStream(final StreamInfo currentStreamInfo, DeviceInfo deviceInfo, final VideoOptions options, Long startPositionTicks, ApiClient apiClient, final Response<StreamInfo> response) {
        String playSessionId = currentStreamInfo.getPlaySessionId();

        apiClient.StopTranscodingProcesses(api.getDeviceInfo().getId(), playSessionId, new StopTranscodingResponse(this, deviceInfo, options, startPositionTicks, apiClient, response));
    }
}
