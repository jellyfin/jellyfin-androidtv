package org.jellyfin.androidtv.playback;

import android.content.Context;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue {

    private PlaybackControlsRow.PlayPauseAction playPauseAction;
    private PlaybackControlsRow.RewindAction rewindAction;
    private PlaybackControlsRow.FastForwardAction fastForwardAction;

    private PlaybackControlsRow.ClosedCaptioningAction closedCaptioningAction;
    private PlaybackControlsRow.RepeatAction repeatAction;
    private PlaybackControlsRow.ShuffleAction shuffleAction;

    private final PlaybackController playbackController;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;


    CustomPlaybackTransportControlGlue(Context context, PlayerAdapter playerAdapter, PlaybackController playbackController) {
        super(context, playerAdapter);
        this.playbackController = playbackController;
        initActions(context);
    }

    private void initActions(Context context) {
        playPauseAction = new PlaybackControlsRow.PlayPauseAction(context);
        rewindAction = new PlaybackControlsRow.RewindAction(context);
        fastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        closedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(context);
        repeatAction = new PlaybackControlsRow.RepeatAction(context);
        shuffleAction = new PlaybackControlsRow.ShuffleAction(context);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        this.primaryActionsAdapter = primaryActionsAdapter;
        primaryActionsAdapter.add(playPauseAction);
        primaryActionsAdapter.add(rewindAction);
        primaryActionsAdapter.add(fastForwardAction);
        secondaryActionsAdapter.add(closedCaptioningAction);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        this.secondaryActionsAdapter = secondaryActionsAdapter;
        secondaryActionsAdapter.add(repeatAction);
        secondaryActionsAdapter.add(shuffleAction);
    }

    @Override
    public void onActionClicked(Action action) {
        if (action == playPauseAction) {
            if (playPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE) {
                getPlayerAdapter().pause();
            } else if (playPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.INDEX_PLAY) {
                getPlayerAdapter().play();
            }
            playPauseAction.nextIndex();
        } else if (action == rewindAction) {
            getPlayerAdapter().rewind();
            rewindAction.nextIndex();
        } else if (action == fastForwardAction) {
            getPlayerAdapter().fastForward();
            fastForwardAction.nextIndex();
        }
        notifyActionChanged(action);
    }

    private void notifyActionChanged(Action action) {
        ArrayObjectAdapter adapter = primaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = secondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
    }

    public void setInitialPlaybackDrawable() {
        playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
        notifyActionChanged(playPauseAction);
    }
}
