package org.jellyfin.androidtv.playback.overlay;

import android.content.Context;
import android.view.View;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowPresenter;

import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.playback.overlay.actions.AdjustAudioDelayAction;
import org.jellyfin.androidtv.playback.overlay.actions.ChannelBarChannelAction;
import org.jellyfin.androidtv.playback.overlay.actions.ClosedCaptionsAction;
import org.jellyfin.androidtv.playback.overlay.actions.GuideAction;
import org.jellyfin.androidtv.playback.overlay.actions.PreviousLiveTvChannelAction;
import org.jellyfin.androidtv.playback.overlay.actions.RecordAction;
import org.jellyfin.androidtv.playback.overlay.actions.SelectAudioAction;
import org.jellyfin.androidtv.playback.overlay.actions.ZoomAction;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue {

    // Normal playback actions
    private PlaybackControlsRow.PlayPauseAction playPauseAction;
    private PlaybackControlsRow.RewindAction rewindAction;
    private PlaybackControlsRow.FastForwardAction fastForwardAction;
    private PlaybackControlsRow.SkipNextAction skipNextAction;
    private SelectAudioAction selectAudioAction;
    private ClosedCaptionsAction closedCaptionsAction;
    private AdjustAudioDelayAction adjustAudioDelayAction;
    private ZoomAction zoomAction;

    // TV actions
    private PreviousLiveTvChannelAction previousLiveTvChannelAction;
    private ChannelBarChannelAction channelBarChannelAction;
    private GuideAction guideAction;
    private RecordAction recordAction;

    private final VideoPlayerAdapter playerAdapter;
    private final LeanbackOverlayFragment leanbackOverlayFragment;
    private final CustomActionClickedHandler customActionClickedHandler;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;

    CustomPlaybackTransportControlGlue(Context context, VideoPlayerAdapter playerAdapter, PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment) {
        super(context, playerAdapter);
        this.playerAdapter = playerAdapter;
        this.leanbackOverlayFragment = leanbackOverlayFragment;
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
        previousLiveTvChannelAction = new PreviousLiveTvChannelAction(context, this);
        channelBarChannelAction = new ChannelBarChannelAction(context, this);
        guideAction = new GuideAction(context, this);
        recordAction = new RecordAction(context, this);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        this.primaryActionsAdapter = primaryActionsAdapter;

        primaryActionsAdapter.add(playPauseAction);

        if (canSeek()) {
            primaryActionsAdapter.add(rewindAction);
            primaryActionsAdapter.add(fastForwardAction);
        }

        if (hasSubs()) {
            primaryActionsAdapter.add(closedCaptionsAction);
        }

        if (hasMultiAudio()) {
            primaryActionsAdapter.add(selectAudioAction);
        }

        if (isLiveTv()) {
            primaryActionsAdapter.add(channelBarChannelAction);
            primaryActionsAdapter.add(guideAction);
        }
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        this.secondaryActionsAdapter = secondaryActionsAdapter;

        if (isLiveTv()) {
            secondaryActionsAdapter.add(previousLiveTvChannelAction);
            if (canRecordLiveTv()) {
                secondaryActionsAdapter.add(recordAction);
                recordingStateChanged();
            }
        }

        if (hasNextItem()) {
            secondaryActionsAdapter.add(skipNextAction);
        }

        if (!isNativeMode()) {
            secondaryActionsAdapter.add(adjustAudioDelayAction);
        } else {
            secondaryActionsAdapter.add(zoomAction);
        }
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
        } else if (action == previousLiveTvChannelAction) {
            customActionClickedHandler.handlePreviousLiveTvChannelSelection(playerAdapter.getMasterOverlayFragment());
        } else if (action == channelBarChannelAction) {
            leanbackOverlayFragment.hideOverlay();
            customActionClickedHandler.handleChannelBarSelection(playerAdapter.getMasterOverlayFragment());
        } else if (action == guideAction) {
            leanbackOverlayFragment.hideOverlay();
            customActionClickedHandler.handleGuideSelection(playerAdapter.getMasterOverlayFragment());
        } else if (action == recordAction) {
            playerAdapter.toggleRecording();
            // Icon will be updated via callback recordingStateChanged
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

    @Override
    protected PlaybackRowPresenter onCreateRowPresenter() {
        PlaybackTransportRowPresenter presenter = (PlaybackTransportRowPresenter) super.onCreateRowPresenter();
        presenter.setDescriptionPresenter(new FullDescriptionPresenter());
        return presenter;
    }

    void setInitialPlaybackDrawable() {
        playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
        notifyActionChanged(playPauseAction);
    }

    private boolean hasSubs() {
        return playerAdapter.hasSubs();
    }

    private boolean hasMultiAudio() {
        return playerAdapter.hasMultiAudio();
    }

    private boolean hasNextItem() {
        return playerAdapter.hasNextItem();
    }

    private boolean isNativeMode() {
        return playerAdapter.isNativeMode();
    }

    private boolean canSeek() {
        return playerAdapter.canSeek();
    }

    private boolean isLiveTv() {
        return playerAdapter.isLiveTv();
    }

    private boolean canRecordLiveTv() {
        return playerAdapter.canRecordLieTv();
    }

    void invalidatePlaybackControls() {
        primaryActionsAdapter.clear();
        secondaryActionsAdapter.clear();
        onCreatePrimaryActions(primaryActionsAdapter);
        onCreateSecondaryActions(secondaryActionsAdapter);
    }

    void recordingStateChanged() {
        if (playerAdapter.isRecording()) {
            recordAction.setIndex(RecordAction.INDEX_RECORDING);
        } else {
            recordAction.setIndex(RecordAction.INDEX_INACTIVE);
        }
        notifyActionChanged(recordAction);
    }
}
