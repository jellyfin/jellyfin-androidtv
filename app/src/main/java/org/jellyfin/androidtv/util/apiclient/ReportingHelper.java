package org.jellyfin.androidtv.util.apiclient;

import androidx.annotation.Nullable;

import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackManager;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.session.PlaybackProgressInfo;
import org.jellyfin.apiclient.model.session.PlaybackStartInfo;
import org.jellyfin.apiclient.model.session.PlaybackStopInfo;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.koin.java.KoinJavaComponent;

import timber.log.Timber;

public class ReportingHelper {
    public static void reportStopped(org.jellyfin.sdk.model.api.BaseItemDto item, StreamInfo streamInfo, long pos) {
        if (item != null && streamInfo != null) {
            Timber.i("ReportingHelper.reportStopped called for " + item.getId() + " at position " + pos);
            PlaybackStopInfo info = new PlaybackStopInfo();
            info.setItemId(item.getId().toString());
            info.setPositionTicks(pos);
            KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).reportPlaybackStopped(info, streamInfo, KoinJavaComponent.<ApiClient>get(ApiClient.class), new EmptyResponse());

            DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
            dataRefreshService.setLastPlayback(System.currentTimeMillis());
            switch (item.getType()) {
                case MOVIE:
                    dataRefreshService.setLastMoviePlayback(System.currentTimeMillis());
                    break;
                case EPISODE:
                    dataRefreshService.setLastTvPlayback(System.currentTimeMillis());
                    break;
            }
        }
    }

    public static void reportStart(org.jellyfin.sdk.model.api.BaseItemDto item, long pos) {
        PlaybackStartInfo startInfo = new PlaybackStartInfo();
        startInfo.setItemId(item.getId().toString());
        if (item.getType() != BaseItemKind.TV_CHANNEL) startInfo.setPositionTicks(pos);
        KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).reportPlaybackStart(startInfo, KoinJavaComponent.<ApiClient>get(ApiClient.class), new EmptyResponse());
        Timber.i("Playback of %s started.", item.getName());
    }

    public static void reportProgress(@Nullable PlaybackController playbackController, org.jellyfin.sdk.model.api.BaseItemDto item, StreamInfo currentStreamInfo, Long position, boolean isPaused) {
        if (item != null && currentStreamInfo != null) {
            PlaybackProgressInfo info = new PlaybackProgressInfo();
            info.setItemId(item.getId().toString());
            if (item.getType() != BaseItemKind.TV_CHANNEL) {
                info.setPositionTicks(position);
                info.setCanSeek(currentStreamInfo.getRunTimeTicks() != null && currentStreamInfo.getRunTimeTicks() > 0);
            }
            info.setIsPaused(isPaused);
            info.setPlayMethod(currentStreamInfo.getPlayMethod());
            if (playbackController != null && playbackController.isPlaying()) {
                info.setAudioStreamIndex(playbackController.getAudioStreamIndex());
                info.setSubtitleStreamIndex(playbackController.getSubtitleStreamIndex());
            }
            KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).reportPlaybackProgress(info, currentStreamInfo, KoinJavaComponent.<ApiClient>get(ApiClient.class), new EmptyResponse());
        }
    }
}
