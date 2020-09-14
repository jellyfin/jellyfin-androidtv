package org.jellyfin.androidtv.ui.playback.overlay;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.action.AdjustAudioDelayAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChannelBarChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChapterAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ClosedCaptionsAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.GuideAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PreviousLiveTvChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RecordAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectAudioAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ZoomAction;
import org.jellyfin.androidtv.preference.UserPreferences;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

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
    private ChapterAction chapterAction;

    // TV actions
    private PreviousLiveTvChannelAction previousLiveTvChannelAction;
    private ChannelBarChannelAction channelBarChannelAction;
    private GuideAction guideAction;
    private RecordAction recordAction;

    private final VideoPlayerAdapter playerAdapter;
    private final PlaybackController playbackController;
    private final LeanbackOverlayFragment leanbackOverlayFragment;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;

    CustomPlaybackTransportControlGlue(Context context, VideoPlayerAdapter playerAdapter, PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment) {
        super(context, playerAdapter);
        this.playerAdapter = playerAdapter;
        this.playbackController = playbackController;
        this.leanbackOverlayFragment = leanbackOverlayFragment;
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
        chapterAction = new ChapterAction(context, this);

        previousLiveTvChannelAction = new PreviousLiveTvChannelAction(context, this);
        channelBarChannelAction = new ChannelBarChannelAction(context, this);
        guideAction = new GuideAction(context, this);
        recordAction = new RecordAction(context, this);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        this.primaryActionsAdapter = primaryActionsAdapter;
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        this.secondaryActionsAdapter = secondaryActionsAdapter;
    }

    void addMediaActions() {
        primaryActionsAdapter.clear();
        secondaryActionsAdapter.clear();

        // Primary Items
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


        // Secondary Items
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


        if (hasChapters()) {
            secondaryActionsAdapter.add(chapterAction);
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
                playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
            } else if (playPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.INDEX_PLAY) {
                getPlayerAdapter().play();
                playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
            }
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
            leanbackOverlayFragment.setFading(false);
            selectAudioAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), view);
        } else if (action == closedCaptionsAction) {
            leanbackOverlayFragment.setFading(false);
            closedCaptionsAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), view);
        } else if (action == adjustAudioDelayAction) {
            leanbackOverlayFragment.hideOverlay();
            adjustAudioDelayAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), view);
        } else if (action == zoomAction) {
            leanbackOverlayFragment.setFading(false);
            zoomAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), view);
        } else if (action == chapterAction) {
            leanbackOverlayFragment.hideOverlay();
            playerAdapter.getMasterOverlayFragment().showChapterSelector();
        } else if (action == previousLiveTvChannelAction) {
            playerAdapter.getMasterOverlayFragment().switchChannel(TvManager.getPrevLiveTvChannel());
        } else if (action == channelBarChannelAction) {
            leanbackOverlayFragment.hideOverlay();
            playerAdapter.getMasterOverlayFragment().showQuickChannelChanger();
        } else if (action == guideAction) {
            leanbackOverlayFragment.hideOverlay();
            playerAdapter.getMasterOverlayFragment().showGuide();
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
        return playerAdapter.canRecordLiveTv();
    }

    private boolean hasChapters() {
        return playerAdapter.hasChapters();
    }

    void invalidatePlaybackControls() {
        primaryActionsAdapter.clear();
        secondaryActionsAdapter.clear();
        addMediaActions();
    }

    void recordingStateChanged() {
        if (playerAdapter.isRecording()) {
            recordAction.setIndex(RecordAction.INDEX_RECORDING);
        } else {
            recordAction.setIndex(RecordAction.INDEX_INACTIVE);
        }
        notifyActionChanged(recordAction);
    }

    void updatePlayState() {
        playPauseAction.setIndex(isPlaying() ? PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE : PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
        notifyActionChanged(playPauseAction);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (hasSubs() && event.getAction() == KeyEvent.ACTION_UP && keyCode == TvApp.getApplication().getUserPreferences().get(UserPreferences.Companion.getShortcutSubtitleTrack())) {
            closedCaptionsAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), v);
        }
        if (hasMultiAudio() && event.getAction() == KeyEvent.ACTION_UP && keyCode == TvApp.getApplication().getUserPreferences().get(UserPreferences.Companion.getShortcutAudioTrack())) {
            selectAudioAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), v);
        }
        return super.onKey(v, keyCode, event);
    }
}
