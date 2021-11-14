package org.jellyfin.androidtv.ui.playback.overlay;

import android.os.Bundle;

import androidx.leanback.app.PlaybackSupportFragment;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import timber.log.Timber;

public class LeanbackOverlayFragment extends PlaybackSupportFragment {

    private PlaybackController playbackController;
    private CustomPlaybackTransportControlGlue playerGlue;
    private VideoPlayerAdapter playerAdapter;
    private boolean shouldShowOverlay = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setBackgroundType(BG_LIGHT);

        PlaybackController playbackController = TvApp.getApplication().getPlaybackController();

        playerAdapter = new VideoPlayerAdapter(playbackController);
        playerGlue = new CustomPlaybackTransportControlGlue(getContext(), playerAdapter, playbackController, this);
        playerGlue.setHost(new CustomPlaybackFragmentGlueHost(this));
    }

    public void initFromView(PlaybackController playbackController, CustomPlaybackOverlayFragment customPlaybackOverlayFragment) {
        this.playbackController = playbackController;
        playerGlue.setInitialPlaybackDrawable();
        playerAdapter.setMasterOverlayFragment(customPlaybackOverlayFragment);
    }

    @Override
    public void showControlsOverlay(boolean runAnimation) {
        if (shouldShowOverlay) {
            super.showControlsOverlay(runAnimation);
        }
    }

    public void updateCurrentPosition() {
        playerAdapter.updateCurrentPosition();
        updatePlayState();
    }

    public void updatePlayState() {
        playerAdapter.updatePlayState();
        playerGlue.updatePlayState();
    }

    public void setShouldShowOverlay(boolean shouldShowOverlay) {
        this.shouldShowOverlay = shouldShowOverlay;
    }

    public void hideOverlay() {
        hideControlsOverlay(true);
    }

    public void setFading(boolean fadingEnabled) {
        playerAdapter.getMasterOverlayFragment().setFadingEnabled(fadingEnabled);
    }

    public void mediaInfoChanged() {
        Timber.d("leanback overlay fragment - media info changed");
        BaseItemDto currentlyPlayingItem = playbackController.getCurrentlyPlayingItem();
        if (currentlyPlayingItem == null) return;

        playerGlue.invalidatePlaybackControls();
        playerGlue.setSeekEnabled(playerAdapter.canSeek());
        playerGlue.setSeekProvider(playerAdapter.canSeek() ? new CustomSeekProvider(playerAdapter) : null);
        recordingStateChanged();
        playerAdapter.updateDuration();
    }

    public void recordingStateChanged() {
        playerGlue.recordingStateChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        playerAdapter.getMasterOverlayFragment().onPause();
    }

    public CustomPlaybackTransportControlGlue getPlayerGlue() {
        return playerGlue;
    }

    public void onFullyInitialized() {
        updatePlayState();
        playerGlue.addMediaActions();
    }
}
