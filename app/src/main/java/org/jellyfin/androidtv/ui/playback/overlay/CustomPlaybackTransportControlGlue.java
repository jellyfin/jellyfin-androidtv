package org.jellyfin.androidtv.ui.playback.overlay;

import android.content.Context;
import android.os.Handler;
import android.text.format.DateFormat;
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
import org.jellyfin.androidtv.preference.constant.ClockBehavior;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.action.AdjustAudioDelayAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChannelBarChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChapterAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ClosedCaptionsAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.GuideAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PlaybackSpeedAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PreviousLiveTvChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RecordAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectAudioAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ZoomAction;
import org.koin.java.KoinJavaComponent;

import java.util.Calendar;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue<VideoPlayerAdapter> {

    // Normal playback actions
    private PlaybackControlsRow.PlayPauseAction playPauseAction;
    private PlaybackControlsRow.RewindAction rewindAction;
    private PlaybackControlsRow.FastForwardAction fastForwardAction;
    private PlaybackControlsRow.SkipNextAction skipNextAction;
    private SelectAudioAction selectAudioAction;
    private ClosedCaptionsAction closedCaptionsAction;
    private AdjustAudioDelayAction adjustAudioDelayAction;
    private PlaybackSpeedAction playbackSpeedAction;
    private ZoomAction zoomAction;
    private ChapterAction chapterAction;

    // TV actions
    private PreviousLiveTvChannelAction previousLiveTvChannelAction;
    private ChannelBarChannelAction channelBarChannelAction;
    private GuideAction guideAction;
    private RecordAction recordAction;

    private final PlaybackController playbackController;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;

    // Injected views
    private TextView mEndsText = null;

    private final Handler mHandler = new Handler();
    private Runnable mRefreshEndTime;
    private Runnable mRefreshViewVisibility;

    private LinearLayout mButtonRef;

    CustomPlaybackTransportControlGlue(Context context, VideoPlayerAdapter playerAdapter, PlaybackController playbackController) {
        super(context, playerAdapter);
        this.playbackController = playbackController;

        mRefreshEndTime = () -> {
            if (!isPlaying()) {
                setEndTime();

                mHandler.postDelayed(mRefreshEndTime, 30000);
            }
        };

        mRefreshViewVisibility = () -> {
            if (mButtonRef != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
                mEndsText.setVisibility(mButtonRef.getVisibility());
            else
                mHandler.postDelayed(mRefreshViewVisibility, 100);
        };

        initActions(context);
    }

    @Override
    protected void onDetachedFromHost() {
        mHandler.removeCallbacks(mRefreshEndTime);
        mHandler.removeCallbacks(mRefreshViewVisibility);
        super.onDetachedFromHost();
    }

    @Override
    protected PlaybackRowPresenter onCreateRowPresenter() {
        final AbstractDetailsDescriptionPresenter detailsPresenter = new AbstractDetailsDescriptionPresenter() {
            @Override
            protected void onBindDescription(ViewHolder vh, Object item) {

            }
        };
        PlaybackTransportRowPresenter rowPresenter = new PlaybackTransportRowPresenter() {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                RowPresenter.ViewHolder vh = super.createRowViewHolder(parent);

                ClockBehavior showClock = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getClockBehavior());

                if (showClock == ClockBehavior.ALWAYS || showClock == ClockBehavior.IN_VIDEO) {
                    Context context = parent.getContext();
                    mEndsText = new TextView(context);
                    mEndsText.setTextAppearance(context, androidx.leanback.R.style.Widget_Leanback_PlaybackControlsTimeStyle);
                    setEndTime();

                    LinearLayout view = (LinearLayout) vh.view;

                    PlaybackTransportRowView bar = (PlaybackTransportRowView) view.getChildAt(1);
                    FrameLayout v = (FrameLayout) bar.getChildAt(0);
                    mButtonRef = (LinearLayout) v.getChildAt(0);

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
                }

                return vh;
            }

            @Override
            protected void onProgressBarClicked(PlaybackTransportRowPresenter.ViewHolder vh) {
                CustomPlaybackTransportControlGlue controlglue = CustomPlaybackTransportControlGlue.this;
                controlglue.onActionClicked(controlglue.playPauseAction);
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
        playbackSpeedAction = new PlaybackSpeedAction(context, this, playbackController);
        playbackSpeedAction.setLabels(new String[]{context.getString(R.string.lbl_playback_speed)});
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
        recordAction.setLabels(new String[]{
                context.getString(R.string.lbl_record),
                context.getString(R.string.lbl_cancel_recording)
        });
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
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
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

        if (!isLiveTv()) {
            secondaryActionsAdapter.add(playbackSpeedAction);
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
            getPlayerAdapter().getLeanbackOverlayFragment().setFading(false);
            selectAudioAction.handleClickAction(playbackController, getPlayerAdapter().getLeanbackOverlayFragment(), getContext(), view);
        } else if (action == closedCaptionsAction) {
            getPlayerAdapter().getLeanbackOverlayFragment().setFading(false);
            closedCaptionsAction.handleClickAction(playbackController, getPlayerAdapter().getLeanbackOverlayFragment(), getContext(), view);
        } else if (action == playbackSpeedAction) {
            getPlayerAdapter().getLeanbackOverlayFragment().setFading(false);
            playbackSpeedAction.handleClickAction(playbackController, getPlayerAdapter().getLeanbackOverlayFragment(), getContext(), view);
        } else if (action == adjustAudioDelayAction) {
            getPlayerAdapter().getLeanbackOverlayFragment().setFading(false);
            adjustAudioDelayAction.handleClickAction(playbackController, getPlayerAdapter().getLeanbackOverlayFragment(), getContext(), view);
        } else if (action == zoomAction) {
            getPlayerAdapter().getLeanbackOverlayFragment().setFading(false);
            zoomAction.handleClickAction(playbackController, getPlayerAdapter().getLeanbackOverlayFragment(), getContext(), view);
        } else if (action == chapterAction) {
            getPlayerAdapter().getLeanbackOverlayFragment().hideOverlay();
            getPlayerAdapter().getMasterOverlayFragment().showChapterSelector();
        } else if (action == previousLiveTvChannelAction) {
            getPlayerAdapter().getMasterOverlayFragment().switchChannel(TvManager.getPrevLiveTvChannel());
        } else if (action == channelBarChannelAction) {
            getPlayerAdapter().getLeanbackOverlayFragment().hideOverlay();
            getPlayerAdapter().getMasterOverlayFragment().showQuickChannelChanger();
        } else if (action == guideAction) {
            getPlayerAdapter().getLeanbackOverlayFragment().hideOverlay();
            getPlayerAdapter().getMasterOverlayFragment().showGuide();
        } else if (action == recordAction) {
            getPlayerAdapter().toggleRecording();
            // Icon will be updated via callback recordingStateChanged
        }
    }

    private void setEndTime() {
        if (mEndsText == null || getPlayerAdapter().getDuration() < 1)
            return;
        long msLeft = getPlayerAdapter().getDuration() - getPlayerAdapter().getCurrentPosition();
        Calendar ends = Calendar.getInstance();
        ends.setTimeInMillis(ends.getTimeInMillis() + msLeft);
        mEndsText.setText(getContext().getString(R.string.lbl_playback_control_ends, DateFormat.getTimeFormat(getContext()).format(ends.getTime())));
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
        return getPlayerAdapter().hasSubs();
    }

    private boolean hasMultiAudio() {
        return getPlayerAdapter().hasMultiAudio();
    }

    private boolean hasNextItem() {
        return getPlayerAdapter().hasNextItem();
    }

    private boolean isNativeMode() {
        return getPlayerAdapter().isNativeMode();
    }

    private boolean canSeek() {
        return getPlayerAdapter().canSeek();
    }

    private boolean isLiveTv() {
        return getPlayerAdapter().isLiveTv();
    }

    private boolean canRecordLiveTv() {
        return getPlayerAdapter().canRecordLiveTv();
    }

    private boolean hasChapters() {
        return getPlayerAdapter().hasChapters();
    }

    void invalidatePlaybackControls() {
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
            secondaryActionsAdapter.clear();
        addMediaActions();
    }

    void recordingStateChanged() {
        if (getPlayerAdapter().isRecording()) {
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
        if (!isPlaying()) {
            mHandler.removeCallbacks(mRefreshEndTime);
            mHandler.postDelayed(mRefreshEndTime, 30000);
        } else {
            mHandler.removeCallbacks(mRefreshEndTime);
        }

    }

    public void setInjectedViewsVisibility() {
        if (mButtonRef != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
            mEndsText.setVisibility(mButtonRef.getVisibility());
        mHandler.removeCallbacks(mRefreshViewVisibility);
        mHandler.postDelayed(mRefreshViewVisibility, 100);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (hasSubs() && event.getAction() == KeyEvent.ACTION_UP && keyCode == KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getShortcutSubtitleTrack())) {
            closedCaptionsAction.handleClickAction(playbackController, getPlayerAdapter().getLeanbackOverlayFragment(), getContext(), v);
        }
        if (hasMultiAudio() && event.getAction() == KeyEvent.ACTION_UP && keyCode == KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getShortcutAudioTrack())) {
            selectAudioAction.handleClickAction(playbackController, getPlayerAdapter().getLeanbackOverlayFragment(), getContext(), v);
        }
        return super.onKey(v, keyCode, event);
    }
}
