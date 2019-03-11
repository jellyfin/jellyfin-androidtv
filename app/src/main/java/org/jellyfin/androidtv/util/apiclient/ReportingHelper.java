package org.jellyfin.androidtv.util.apiclient;

import org.jellyfin.androidtv.TvApp;

import java.util.Calendar;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.session.PlaybackProgressInfo;
import mediabrowser.model.session.PlaybackStartInfo;
import mediabrowser.model.session.PlaybackStopInfo;

public class ReportingHelper {
    public static void reportStopped(BaseItemDto item, StreamInfo streamInfo, long pos) {
        if (item != null && streamInfo != null) {
            PlaybackStopInfo info = new PlaybackStopInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(pos);
            TvApp.getApplication().getPlaybackManager().reportPlaybackStopped(info, streamInfo, apiClient.getServerInfo().getId(), TvApp.getApplication().getCurrentUser().getId(), false, apiClient, new EmptyResponse());

            TvApp.getApplication().setLastPlayback(Calendar.getInstance());
            switch (item.getType()) {
                case "Movie":
                    TvApp.getApplication().setLastMoviePlayback(Calendar.getInstance());
                    break;
                case "Episode":
                    TvApp.getApplication().setLastTvPlayback(Calendar.getInstance());
                    break;
            }
        }
    }

    public static void reportStart(BaseItemDto item, long pos) {
        PlaybackStartInfo startInfo = new PlaybackStartInfo();
        startInfo.setItemId(item.getId());
        startInfo.setPositionTicks(pos);
        TvApp.getApplication().getPlaybackManager().reportPlaybackStart(startInfo, false, TvApp.getApplication().getApiClient(), new EmptyResponse());
        TvApp.getApplication().getLogger().Info("Playback of " + item.getName() + " started.");
    }

    public static void reportProgress(BaseItemDto item, StreamInfo currentStreamInfo, Long position, boolean isPaused) {
        if (item != null && currentStreamInfo != null) {
            PlaybackProgressInfo info = new PlaybackProgressInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(position);
            info.setIsPaused(isPaused);
            info.setCanSeek(currentStreamInfo.getRunTimeTicks() != null && currentStreamInfo.getRunTimeTicks() > 0);
            info.setIsMuted(TvApp.getApplication().isAudioMuted());
            info.setPlayMethod(currentStreamInfo.getPlayMethod());
            if (TvApp.getApplication().getPlaybackController() != null && TvApp.getApplication().getPlaybackController().isPlaying()) {
                info.setAudioStreamIndex(TvApp.getApplication().getPlaybackController().getAudioStreamIndex());
                info.setSubtitleStreamIndex(TvApp.getApplication().getPlaybackController().getSubtitleStreamIndex());
            }
            TvApp.getApplication().getPlaybackManager().reportPlaybackProgress(info, currentStreamInfo, false, apiClient, new EmptyResponse());
        }
    }
}
