package org.jellyfin.androidtv.ui.playback.overlay;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowView;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.UserPreferences;
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

import java.text.DateFormat;
import java.util.Calendar;

import static org.koin.java.KoinJavaComponent.get;

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

    // Injected views
    private TextView mEndsText = null;

    private Handler mHandler = new Handler();
    private Runnable mRefreshEndTime;

    private View mButtonRef;

    CustomPlaybackTransportControlGlue(Context context, VideoPlayerAdapter playerAdapter, PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment) {
        super(context, playerAdapter);
        this.playerAdapter = playerAdapter;
        this.playbackController = playbackController;
        this.leanbackOverlayFragment = leanbackOverlayFragment;

        mRefreshEndTime = () -> {
            if (!isPlaying())
                setEndTime();

            if (mButtonRef != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
                mEndsText.setVisibility(mButtonRef.getVisibility());

            mHandler.postDelayed(mRefreshEndTime, 250);
        };

        mHandler.postDelayed(mRefreshEndTime, 250);

        initActions(context);
    }

    @Override
    protected PlaybackRowPresenter onCreateRowPresenter() {
        final AbstractDetailsDescriptionPresenter detailsPresenter =
                new AbstractDetailsDescriptionPresenter() {
                    @Override
                    protected void onBindDescription(ViewHolder
                                                             viewHolder, Object obj) {
                        PlaybackTransportControlGlue glue = (PlaybackTransportControlGlue) obj;
                        viewHolder.getTitle().setText(glue.getTitle());
                        viewHolder.getSubtitle().setText(glue.getSubtitle());
                    }
                };

        PlaybackTransportRowPresenter rowPresenter = new PlaybackTransportRowPresenter() {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                RowPresenter.ViewHolder vh = super.createRowViewHolder(parent);
                Context context = parent.getContext();

                mEndsText = new TextView(context);
                mEndsText.setTextAppearance(context, androidx.leanback.R.style.Widget_Leanback_PlaybackControlsTimeStyle);
                setEndTime();

                LinearLayout view = (LinearLayout) vh.view;

                PlaybackTransportRowView bar = (PlaybackTransportRowView) view.getChildAt(1);
                FrameLayout v = (FrameLayout) bar.getChildAt(0);
                mButtonRef = v.getChildAt(0);
                bar.removeViewAt(0);
                RelativeLayout rl = new RelativeLayout(context);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                rl.addView(v);

                RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                rlp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                rlp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                rl.addView(mEndsText, rlp2);
                bar.addView(rl, 0, rlp);

                return vh;
            }

            @Override
            protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                super.onBindRowViewHolder(vh, item);
                vh.setOnKeyListener(CustomPlaybackTransportControlGlue.this);
            }
            @Override
            protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                super.onUnbindRowViewHolder(vh);
                vh.setOnKeyListener(null);
            }
        };
        rowPresenter.setDescriptionPresenter(detailsPresenter);
        return rowPresenter;
    }

    private void initActions(Context context) {
        playPauseAction = new PlaybackControlsRow.PlayPauseAction(context);
        rewindAction = new PlaybackControlsRow.RewindAction(context);
        fastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        skipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        selectAudioAction = new SelectAudioAction(context, this);
        selectAudioAction.setLabels(new String[]{context.getString(R.string.lbl_audio_track)});
        closedCaptionsAction = new ClosedCaptionsAction(context, this);
        closedCaptionsAction.setLabels(new String[]{context.getString(R.string.lbl_subtitle_track)});
        adjustAudioDelayAction = new AdjustAudioDelayAction(context, this);
        adjustAudioDelayAction.setLabels(new String[]{context.getString(R.string.lbl_audio_delay)});
        zoomAction = new ZoomAction(context, this);
        zoomAction.setLabels(new String[]{context.getString(R.string.lbl_zoom)});
        chapterAction = new ChapterAction(context, this);
        chapterAction.setLabels(new String[]{context.getString(R.string.lbl_chapters)});

        previousLiveTvChannelAction = new PreviousLiveTvChannelAction(context, this);
        previousLiveTvChannelAction.setLabels(new String[]{context.getString(R.string.lbl_prev_item)});
        channelBarChannelAction = new ChannelBarChannelAction(context, this);
        channelBarChannelAction.setLabels(new String[]{context.getString(R.string.lbl_other_channels)});
        guideAction = new GuideAction(context, this);
        guideAction.setLabels(new String[]{context.getString(R.string.lbl_live_tv_guide)});
        recordAction = new RecordAction(context, this);
        recordAction.setLabels(new String[]{context.getString(R.string.lbl_record)});
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

    private void setEndTime() {
        if (mEndsText == null)
            return;
        long msLeft = playerAdapter.getDuration() - playerAdapter.getCurrentPosition();
        Calendar ends = Calendar.getInstance();
        ends.setTimeInMillis(ends.getTimeInMillis() + msLeft);
        mEndsText.setText(getContext().getString(R.string.lbl_playback_control_ends, DateFormat.getTimeInstance(DateFormat.SHORT).format(ends.getTime())));
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
        setEndTime();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (hasSubs() && event.getAction() == KeyEvent.ACTION_UP && keyCode == get(UserPreferences.class).get(UserPreferences.Companion.getShortcutSubtitleTrack())) {
            closedCaptionsAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), v);
        }
        if (hasMultiAudio() && event.getAction() == KeyEvent.ACTION_UP && keyCode == get(UserPreferences.class).get(UserPreferences.Companion.getShortcutAudioTrack())) {
            selectAudioAction.handleClickAction(playbackController, leanbackOverlayFragment, getContext(), v);
        }
        return super.onKey(v, keyCode, event);
    }
}
