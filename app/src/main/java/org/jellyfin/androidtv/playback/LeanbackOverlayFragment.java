package org.jellyfin.androidtv.playback;

import android.net.Uri;
import android.os.Bundle;

import androidx.leanback.app.PlaybackSupportFragment;
import androidx.leanback.app.PlaybackSupportFragmentGlueHost;
import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.media.PlaybackGlue;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

public class LeanbackOverlayFragment extends PlaybackSupportFragment {

    PlaybackController playbackController;
    PlaybackTransportControlGlue playerGlue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TvApp application = TvApp.getApplication();
        PlaybackController playbackController = application.getPlaybackController();

        playerGlue = new CustomPlaybackTransportControlGlue(getContext(), new VideoPlayerAdapter(playbackController), playbackController);
        playerGlue.setHost(new PlaybackSupportFragmentGlueHost(this));
    }

    public void setData(PlaybackController playbackController) {
        this.playbackController = playbackController;
    }

    public void setMediaInfo() {
        BaseItemDto currentlyPlayingItem = playbackController.getCurrentlyPlayingItem();

        playerGlue.setTitle(currentlyPlayingItem.getOriginalTitle());
    }
}
