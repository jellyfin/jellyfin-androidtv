package org.jellyfin.androidtv.playback.overlay;

import android.os.Bundle;

import androidx.leanback.app.PlaybackSupportFragment;
import androidx.leanback.app.PlaybackSupportFragmentGlueHost;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.playback.overlay.CustomSeekProvider;
import org.jellyfin.androidtv.playback.overlay.VideoPlayerAdapter;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

public class LeanbackOverlayFragment extends PlaybackSupportFragment {

    private PlaybackController playbackController;
    private CustomPlaybackTransportControlGlue playerGlue;
    private VideoPlayerAdapter playerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TvApp application = TvApp.getApplication();
        PlaybackController playbackController = application.getPlaybackController();

        playerAdapter = new VideoPlayerAdapter(playbackController);
        playerGlue = new CustomPlaybackTransportControlGlue(getContext(), playerAdapter, playbackController);
        playerGlue.setHost(new CustomPlaybackFragmentGlueHost(this));
    }

    public void initFromView(PlaybackController playbackController) {
        this.playbackController = playbackController;
        playerGlue.setSeekProvider(new CustomSeekProvider(playerAdapter.getDuration()));
        playerGlue.setInitialPlaybackDrawable();
    }

    public void setMediaInfo() {
        BaseItemDto currentlyPlayingItem = playbackController.getCurrentlyPlayingItem();
        playerGlue.setTitle(currentlyPlayingItem.getOriginalTitle());
    }

    public void updateCurrentPosition() {
        playerAdapter.updateCurrentPosition();
    }

    public void updatePlayState() {
        playerAdapter.updatePlayState();
    }
}
