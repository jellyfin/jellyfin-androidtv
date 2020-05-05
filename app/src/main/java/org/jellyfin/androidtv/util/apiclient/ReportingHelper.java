package org.jellyfin.androidtv.util.apiclient;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.compat.StreamInfo;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.session.PlaybackProgressInfo;
import org.jellyfin.apiclient.model.session.PlaybackStartInfo;
import org.jellyfin.apiclient.model.session.PlaybackStopInfo;

import java.util.Calendar;

import timber.log.Timber;

public class ReportingHelper {
    public static void reportStopped(BaseItemDto item, StreamInfo streamInfo, long pos) {
        if (item != null && streamInfo != null) {
            PlaybackStopInfo info = new PlaybackStopInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(pos);
            TvApp.getApplication().getPlaybackManager().reportPlaybackStopped(info, streamInfo, apiClient.getServerInfo().getId(), TvApp.getApplication().getCurrentUser().getId(), false, apiClient, new EmptyResponse());

            TvApp.getApplication().setLastPlayback(Calendar.getInstance());
            switch (item.getBaseItemType()) {
                case Movie:
                    TvApp.getApplication().setLastMoviePlayback(Calendar.getInstance());
                    break;
                case Episode:
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
        Timber.i("Playback of %s started.", item.getName());
    }

    public static void reportProgress(BaseItemDto item, StreamInfo currentStreamInfo, Long position, boolean isPaused) {
        if (item != null && currentStreamInfo != null) {
            PlaybackProgressInfo info = new PlaybackProgressInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(position);
            info.setIsPaused(isPaused);
            info.setCanSeek(currentStreamInfo.getRunTimeTicks() != null && currentStreamInfo.getRunTimeTicks() > 0);
            info.setPlayMethod(currentStreamInfo.getPlayMethod());
            if (TvApp.getApplication().getPlaybackController() != null && TvApp.getApplication().getPlaybackController().isPlaying()) {
                info.setAudioStreamIndex(TvApp.getApplication().getPlaybackController().getAudioStreamIndex());
                info.setSubtitleStreamIndex(TvApp.getApplication().getPlaybackController().getSubtitleStreamIndex());
            }
            TvApp.getApplication().getPlaybackManager().reportPlaybackProgress(info, currentStreamInfo, false, apiClient, new EmptyResponse());
        }
    }
}
