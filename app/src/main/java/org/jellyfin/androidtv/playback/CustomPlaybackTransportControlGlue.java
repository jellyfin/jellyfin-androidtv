package org.jellyfin.androidtv.playback;

import android.content.Context;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackRowPresenter;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue {

    private PlaybackControlsRow.PlayPauseAction playPauseAction;
    private PlaybackControlsRow.RewindAction rewindAction;
    private PlaybackControlsRow.FastForwardAction fastForwardAction;

    private PlaybackControlsRow.ClosedCaptioningAction closedCaptioningAction;
    private PlaybackControlsRow.RepeatAction repeatAction;
    private PlaybackControlsRow.ShuffleAction shuffleAction;

    private final PlaybackController playbackController;


    CustomPlaybackTransportControlGlue(Context context, PlayerAdapter playerAdapter, PlaybackController playbackController) {
        super(context, playerAdapter);
        this.playbackController = playbackController;
    }
/*
    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        primaryActionsAdapter.add(rewindAction);
        primaryActionsAdapter.add(playPauseAction);
        primaryActionsAdapter.add(fastForwardAction);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        secondaryActionsAdapter.add(closedCaptioningAction);
        secondaryActionsAdapter.add(repeatAction);
        secondaryActionsAdapter.add(shuffleAction);
    }

    @Override
    public void onActionClicked(Action action) {
        if (action == playPauseAction) {
            if (getPlayerAdapter().isPlaying()) {
                getPlayerAdapter().play();
            } else {
                getPlayerAdapter().pause();
            }
        } else if (action == rewindAction) {
            getPlayerAdapter().rewind();
        } else if (action == fastForwardAction) {
            getPlayerAdapter().fastForward();
        }
    }*/
}
