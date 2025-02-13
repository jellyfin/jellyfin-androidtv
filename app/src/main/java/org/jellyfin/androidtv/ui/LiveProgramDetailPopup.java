package org.jellyfin.androidtv.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyResponse;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.koin.java.KoinJavaComponent;

import java.time.Instant;
import java.time.LocalDateTime;

public class LiveProgramDetailPopup {
    private PopupWindow mPopup;
    private BaseItemDto mProgram;
    private ProgramGridCell mSelectedProgramView;
    final Context mContext;
    final Lifecycle lifecycle;
    private LiveTvGuide mTvGuide;
    private TextView mDTitle;
    private TextView mDSummary;
    private TextView mDRecordInfo;
    private LinearLayout mDTimeline;
    private LinearLayout mDInfoRow;
    private LinearLayout mDButtonRow;
    private LinearLayout mDSimilarRow;
    private Button mFirstButton;
    private Button mSeriesSettingsButton;

    private EmptyResponse mTuneAction;

    private View mAnchor;
    private int mPosLeft;
    private int mPosTop;

    public LiveProgramDetailPopup(Context context, LifecycleOwner lifecycleOwner, LiveTvGuide tvGuide, int width, EmptyResponse tuneAction) {
        mContext = context;
        lifecycle = lifecycleOwner.getLifecycle();
        mTvGuide = tvGuide;
        mTuneAction = tuneAction;
        View layout = LayoutInflater.from(context).inflate(R.layout.program_detail_popup, null);
        int popupHeight = Utils.convertDpToPixel(context, 400);
        mPopup = new PopupWindow(layout, width, popupHeight);
        ViewTreeLifecycleOwner.set(mPopup.getContentView(), lifecycleOwner);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss
        mPopup.setAnimationStyle(R.style.WindowAnimation_SlideTop);
        mDTitle = layout.findViewById(R.id.title);
        mDSummary = layout.findViewById(R.id.summary);
        mDRecordInfo = layout.findViewById(R.id.recordLine);
        mDTimeline = layout.findViewById(R.id.timeline);
        mDButtonRow = layout.findViewById(R.id.buttonRow);
        mDInfoRow = layout.findViewById(R.id.infoRow);
        mDSimilarRow = layout.findViewById(R.id.similarRow);
    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void setContent(final BaseItemDto program, final ProgramGridCell selectedGridView) {
        mProgram = program;
        mSelectedProgramView = selectedGridView;
        mDTitle.setText(mProgram.getName());
        mDButtonRow.removeAllViews();

        mDSummary.setText(mProgram.getOverview());
        if (mDSummary.getLineCount() < 2) {
            mDSummary.setGravity(Gravity.CENTER);
        } else {
            mDSummary.setGravity(Gravity.START);
        }

        // build timeline info
        TvManager.setTimelineRow(mContext, mDTimeline, mProgram);

        //info row
        // TODO: Requires LifecycleOwner for compose
        // InfoLayoutHelper.addInfoRow(mContext, mProgram, mDInfoRow, false);

        //buttons
        mFirstButton = null;
        if (mProgram.getEndDate().isAfter(LocalDateTime.now())) {
            if (mProgram.getStartDate().isBefore(LocalDateTime.now())) {
                // program in progress - tune first button
                mFirstButton = createTuneButton();
            }

            if (Utils.canManageRecordings(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue())) {
                if (mProgram.getTimerId() != null) {
                    // cancel button
                    Button cancel = new Button(mContext);
                    cancel.setText(mContext.getResources().getString(R.string.lbl_cancel_recording));
                    cancel.setTextColor(Color.WHITE);
                    cancel.setBackgroundResource(R.drawable.jellyfin_button);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveProgramDetailPopupHelperKt.cancelTimer(LiveProgramDetailPopup.this, mProgram.getTimerId(), () -> {
                                selectedGridView.setRecTimer(null);
                                mProgram = LiveProgramDetailPopupHelperKt.copyWithTimerId(mProgram, null);
                                dismiss();
                                Utils.showToast(mContext, R.string.msg_recording_cancelled);
                                return null;
                            });
                        }
                    });
                    mDButtonRow.addView(cancel);
                    if (mFirstButton == null) mFirstButton = cancel;
                    // recording info
                    mDRecordInfo.setText(mProgram.getStartDate().isBefore(LocalDateTime.now()) ? mContext.getResources().getString(R.string.msg_recording_now) : mContext.getResources().getString(R.string.msg_will_record));
                } else {
                    // record button
                    Button rec = new Button(mContext);
                    rec.setText(mContext.getResources().getString(R.string.lbl_record));
                    rec.setTextColor(Color.WHITE);
                    rec.setBackgroundResource(R.drawable.jellyfin_button);
                    rec.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LiveProgramDetailPopupHelperKt.recordProgram(LiveProgramDetailPopup.this, mProgram.getId(), program -> {
                                mProgram = program;
                                mSelectedProgramView.setRecSeriesTimer(program.getSeriesTimerId());
                                mSelectedProgramView.setRecTimer(program.getTimerId());
                                if (mSeriesSettingsButton != null)
                                    mSeriesSettingsButton.setVisibility(View.VISIBLE);
                                Utils.showToast(mContext, R.string.msg_set_to_record);
                                dismiss();
                                return null;
                            });
                        }
                    });
                    mDButtonRow.addView(rec);
                    if (mFirstButton == null) mFirstButton = rec;
                    mDRecordInfo.setText(mProgram.getSeriesTimerId() == null ? "" : mContext.getString(R.string.lbl_episode_not_record));
                }
                if (Utils.isTrue(mProgram.isSeries())) {
                    if (mProgram.getSeriesTimerId() != null) {
                        // cancel series button
                        Button cancel = new Button(mContext);
                        cancel.setText(mContext.getResources().getString(R.string.lbl_cancel_series));
                        cancel.setTextColor(Color.WHITE);
                        cancel.setBackgroundResource(R.drawable.jellyfin_button);
                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new AlertDialog.Builder(mContext)
                                        .setTitle(mContext.getResources().getString(R.string.lbl_cancel_series))
                                        .setMessage(mContext.getResources().getString(R.string.msg_cancel_entire_series))
                                        .setNegativeButton(R.string.lbl_no, null)
                                        .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                LiveProgramDetailPopupHelperKt.cancelSeriesTimer(LiveProgramDetailPopup.this, mProgram.getSeriesTimerId(), () -> {
                                                    selectedGridView.setRecSeriesTimer(null);
                                                    mProgram = LiveProgramDetailPopupHelperKt.copyWithSeriesTimerId(mProgram, null);
                                                    mSeriesSettingsButton.setVisibility(View.GONE);
                                                    dismiss();
                                                    Utils.showToast(mContext, R.string.msg_recording_cancelled);
                                                    return null;
                                                });
                                            }
                                        }).show();
                            }
                        });
                        mDButtonRow.addView(cancel);
                    } else {
                        // record series button
                        Button rec = new Button(mContext);
                        rec.setText(mContext.getResources().getString(R.string.lbl_record_series));
                        rec.setTextColor(Color.WHITE);
                        rec.setBackgroundResource(R.drawable.jellyfin_button);
                        rec.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                LiveProgramDetailPopupHelperKt.recordSeries(LiveProgramDetailPopup.this, mProgram.getId(), program -> {
                                    mProgram = program;
                                    mSelectedProgramView.setRecSeriesTimer(program.getSeriesTimerId());
                                    mSelectedProgramView.setRecTimer(program.getTimerId());
                                    if (mSeriesSettingsButton != null)
                                        mSeriesSettingsButton.setVisibility(View.VISIBLE);
                                    Utils.showToast(mContext, R.string.msg_set_to_record);
                                    dismiss();
                                    return null;
                                });
                            }
                        });
                        mDButtonRow.addView(rec);
                    }

                    // manage series button
                    mSeriesSettingsButton = new Button(mContext);
                    mSeriesSettingsButton.setText(mContext.getResources().getString(R.string.lbl_series_settings));
                    mSeriesSettingsButton.setTextColor(Color.WHITE);
                    mSeriesSettingsButton.setBackgroundResource(R.drawable.jellyfin_button);
                    mSeriesSettingsButton.setVisibility(mProgram.getSeriesTimerId() != null ? View.VISIBLE : View.GONE);
                    mSeriesSettingsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showRecordingOptions(true);
                        }
                    });

                    mDButtonRow.addView(mSeriesSettingsButton);
                }

            }

            if (mProgram.getStartDate().isAfter(LocalDateTime.now())) {
                // add tune to button for programs that haven't started yet
                createTuneButton();
            }

            createFavoriteButton();
        } else {
            // program has already ended
            mDRecordInfo.setText(mContext.getResources().getString(R.string.lbl_program_ended));
            mFirstButton = createTuneButton();
        }
        mDSimilarRow.setVisibility(View.GONE);
    }

    public Button createTuneButton() {
        Button tune = addButton(mDButtonRow, R.string.lbl_tune_to_channel);
        tune.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTuneAction != null) mTuneAction.onResponse();
                mPopup.dismiss();
            }
        });

        return tune;
    }

    public android.widget.ImageButton createFavoriteButton() {
        BaseItemDto channel = TvManager.getChannel(TvManager.getAllChannelsIndex(mProgram.getChannelId()));
        boolean isFav = channel.getUserData() != null && channel.getUserData().isFavorite();

        android.widget.ImageButton fave = addImgButton(mDButtonRow, isFav ? R.drawable.ic_heart_red : R.drawable.ic_heart);
        fave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LiveProgramDetailPopupHelperKt.toggleFavorite(LiveProgramDetailPopup.this, channel, channel -> {
                    fave.setImageDrawable(ContextCompat.getDrawable(mContext, channel.getUserData().isFavorite() ? R.drawable.ic_heart_red : R.drawable.ic_heart));
                    mTvGuide.refreshFavorite(channel.getId());
                    DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
                    dataRefreshService.setLastFavoriteUpdate(Instant.now());
                    return null;
                });
            }
        });

        return fave;
    }

    private android.widget.ImageButton addImgButton(LinearLayout layout, int imgResource) {
        android.widget.ImageButton btn = new android.widget.ImageButton(mContext);
        btn.setImageDrawable(ContextCompat.getDrawable(mContext, imgResource));
        btn.setBackgroundResource(R.drawable.jellyfin_button);
        layout.addView(btn);
        return btn;
    }

    private Button addButton(LinearLayout layout, int stringResource) {
        Button btn = new Button(mContext);
        btn.setText(mContext.getResources().getString(stringResource));
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundResource(R.drawable.jellyfin_button);
        layout.addView(btn);
        return btn;
    }

    public void show(View anchor, int x, int y) {
        mAnchor = anchor;
        mPosLeft = x;
        mPosTop = y;
        mPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
        if (mFirstButton != null) mFirstButton.requestFocus();

    }

    public void dismiss() {
        if (mRecordPopup != null && mRecordPopup.isShowing()) mRecordPopup.dismiss();
        if (mPopup != null && mPopup.isShowing()) mPopup.dismiss();
    }

    private RecordPopup mRecordPopup;

    public void showRecordingOptions(final boolean recordSeries) {
        if (mRecordPopup == null)
            mRecordPopup = new RecordPopup(mContext, lifecycle, mAnchor, mPosLeft, mPosTop, mPopup.getWidth());

        LiveProgramDetailPopupHelperKt.getSeriesTimer(this, mProgram.getSeriesTimerId(), seriesTimer -> {
            mRecordPopup.setContent(mContext, mProgram, seriesTimer, mSelectedProgramView, recordSeries);
            mRecordPopup.show();
            return null;
        });
    }
}
