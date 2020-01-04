package org.jellyfin.androidtv.playback.overlay;

import android.content.Context;
import android.view.View;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.playback.overlay.actions.AdjustAudioDelayAction;
import org.jellyfin.androidtv.playback.overlay.actions.ClosedCaptionsAction;
import org.jellyfin.androidtv.playback.overlay.actions.SelectAudioAction;
import org.jellyfin.androidtv.playback.overlay.actions.ZoomAction;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue {

    private PlaybackControlsRow.PlayPauseAction playPauseAction;
    private PlaybackControlsRow.RewindAction rewindAction;
    private PlaybackControlsRow.FastForwardAction fastForwardAction;
    private PlaybackControlsRow.SkipNextAction skipNextAction;
    private SelectAudioAction selectAudioAction;
    private ClosedCaptionsAction closedCaptionsAction;
    private AdjustAudioDelayAction adjustAudioDelayAction;
    private ZoomAction zoomAction;

    private final CustomActionClickedHandler customActionClickedHandler;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;


    CustomPlaybackTransportControlGlue(Context context, PlayerAdapter playerAdapter, PlaybackController playbackController) {
        super(context, playerAdapter);
        customActionClickedHandler = new CustomActionClickedHandler(playbackController, context);
        initActions(context);
    }

    private void initActions(Context context) {
        playPauseAction = new PlaybackControlsRow.PlayPauseAction(context);
        rewindAction = new PlaybackControlsRow.RewindAction(context);
        fastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        skipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        selectAudioAction = new SelectAudioAction(context, this);
        closedCaptionsAction = new ClosedCaptionsAction(context, this);
        adjustAudioDelayAction = new AdjustAudioDelayAction(context, this);
        zoomAction = new ZoomAction(context, this);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        this.primaryActionsAdapter = primaryActionsAdapter;
        primaryActionsAdapter.add(playPauseAction);
        primaryActionsAdapter.add(rewindAction);
        primaryActionsAdapter.add(fastForwardAction);
        primaryActionsAdapter.add(closedCaptionsAction);
        primaryActionsAdapter.add(selectAudioAction);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        this.secondaryActionsAdapter = secondaryActionsAdapter;
        secondaryActionsAdapter.add(skipNextAction);
        secondaryActionsAdapter.add(adjustAudioDelayAction);
        secondaryActionsAdapter.add(zoomAction);
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
        } else if (action == fastForwardAction) {
            getPlayerAdapter().fastForward();
        } else if (action == skipNextAction) {
            getPlayerAdapter().next();
        }
        notifyActionChanged(action);
    }

    public void onCustomActionClicked(Action action, View view) {
        // Handle custom action clicks which require a popup menu
        if (action == selectAudioAction) {
            customActionClickedHandler.handleAudioSelection(view);
        } else if (action == closedCaptionsAction) {
            customActionClickedHandler.handleClosedCaptionsSelection(view);
        } else if (action == adjustAudioDelayAction) {
            customActionClickedHandler.handleAudioDelaySelection(view);
        } else if (action == zoomAction) {
            customActionClickedHandler.handleZoomSelection(view);
        }
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
        }
    }

    void setInitialPlaybackDrawable() {
        playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
        notifyActionChanged(playPauseAction);
    }
}
