package org.jellyfin.androidtv.ui.playback.overlay;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;

import androidx.leanback.app.PlaybackSupportFragment;

import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer;

import kotlin.Lazy;
import timber.log.Timber;

public class LeanbackOverlayFragment extends PlaybackSupportFragment {
    private CustomPlaybackTransportControlGlue playerGlue;
    private VideoPlayerAdapter playerAdapter;
    private boolean shouldShowOverlay = true;
    private Lazy<PlaybackControllerContainer> playbackControllerContainer = inject(PlaybackControllerContainer.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setBackgroundType(BG_LIGHT);

        PlaybackController playbackController = playbackControllerContainer.getValue().getPlaybackController();
        if (playbackController == null) {
            Timber.w("PlaybackController is null, skipping initialization.");
            return;
        }

        playerAdapter = new VideoPlayerAdapter(playbackController, this);
        playerGlue = new CustomPlaybackTransportControlGlue(getContext(), playerAdapter, playbackController);
        playerGlue.setHost(new CustomPlaybackFragmentGlueHost(this));
    }

    public void initFromView(CustomPlaybackOverlayFragment customPlaybackOverlayFragment) {
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
        org.jellyfin.sdk.model.api.BaseItemDto currentlyPlayingItem = playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem();
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
