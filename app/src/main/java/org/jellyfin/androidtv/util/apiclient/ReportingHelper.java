package org.jellyfin.androidtv.util.apiclient;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.ui.playback.PlaybackManager;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.session.PlaybackProgressInfo;
import org.jellyfin.apiclient.model.session.PlaybackStartInfo;
import org.jellyfin.apiclient.model.session.PlaybackStopInfo;
import org.koin.java.KoinJavaComponent;

import timber.log.Timber;

public class ReportingHelper {
    public static void reportStopped(BaseItemDto item, StreamInfo streamInfo, long pos) {
        if (item != null && streamInfo != null) {
            PlaybackStopInfo info = new PlaybackStopInfo();
            info.setItemId(item.getId());
            info.setPositionTicks(pos);
            KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).reportPlaybackStopped(info, streamInfo, KoinJavaComponent.<ApiClient>get(ApiClient.class).getServerInfo().getId(), TvApp.getApplication().getCurrentUser().getId(), KoinJavaComponent.<ApiClient>get(ApiClient.class), new EmptyResponse());

            DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
            dataRefreshService.setLastPlayback(System.currentTimeMillis());
            switch (item.getBaseItemType()) {
                case Movie:
                    dataRefreshService.setLastMoviePlayback(System.currentTimeMillis());
                    break;
                case Episode:
                    dataRefreshService.setLastTvPlayback(System.currentTimeMillis());
                    break;
            }
        }
    }

    public static void reportStart(BaseItemDto item, long pos) {
        PlaybackStartInfo startInfo = new PlaybackStartInfo();
        startInfo.setItemId(item.getId());
        startInfo.setPositionTicks(pos);
        KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).reportPlaybackStart(startInfo, KoinJavaComponent.<ApiClient>get(ApiClient.class), new EmptyResponse());
        Timber.i("Playback of %s started.", item.getName());
    }

    public static void reportProgress(BaseItemDto item, StreamInfo currentStreamInfo, Long position, boolean isPaused) {
        if (item != null && currentStreamInfo != null) {
            PlaybackProgressInfo info = new PlaybackProgressInfo();
            info.setItemId(item.getId());
            info.setPositionTicks(position);
            info.setIsPaused(isPaused);
            info.setCanSeek(currentStreamInfo.getRunTimeTicks() != null && currentStreamInfo.getRunTimeTicks() > 0);
            info.setPlayMethod(currentStreamInfo.getPlayMethod());
            if (TvApp.getApplication().getPlaybackController() != null && TvApp.getApplication().getPlaybackController().isPlaying()) {
                info.setAudioStreamIndex(TvApp.getApplication().getPlaybackController().getAudioStreamIndex());
                info.setSubtitleStreamIndex(TvApp.getApplication().getPlaybackController().getSubtitleStreamIndex());
            }
            KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).reportPlaybackProgress(info, currentStreamInfo, KoinJavaComponent.<ApiClient>get(ApiClient.class), new EmptyResponse());
        }
    }
}
