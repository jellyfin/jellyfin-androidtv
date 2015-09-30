package tv.emby.embyatv.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.Date;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.livetv.LiveTvGuideActivity;
import tv.emby.embyatv.livetv.TvManager;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 9/8/2015.
 */
public class LiveProgramDetailPopup {
    final int MOVIE_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 540);
    final int NORMAL_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 400);

    PopupWindow mPopup;
    BaseItemDto mProgram;
    ProgramGridCell mSelectedProgramView;
    BaseActivity mActivity;
    TextView mDTitle;
    TextView mDSummary;
    TextView mDRecordInfo;
    LinearLayout mDTimeline;
    LinearLayout mDInfoRow;
    LinearLayout mDButtonRow;
    LinearLayout mDSimilarRow;
    Button mFirstButton;

    EmptyResponse mTuneAction;

    View mAnchor;
    int mPosLeft;
    int mPosTop;

    public LiveProgramDetailPopup(BaseActivity activity, int width, EmptyResponse tuneAction) {
        mActivity = activity;
        mTuneAction = tuneAction;
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.program_detail_popup, null);
        Typeface roboto = TvApp.getApplication().getDefaultFont();
        mPopup = new PopupWindow(layout, width, NORMAL_HEIGHT);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
        mPopup.setAnimationStyle(R.style.PopupSlideInTop);
        mDTitle = (TextView)layout.findViewById(R.id.title);
        mDTitle.setTypeface(roboto);
        mDSummary = (TextView)layout.findViewById(R.id.summary);
        mDSummary.setTypeface(roboto);
        mDRecordInfo = (TextView) layout.findViewById(R.id.recordLine);

        mDTimeline = (LinearLayout) layout.findViewById(R.id.timeline);
        mDButtonRow = (LinearLayout) layout.findViewById(R.id.buttonRow);
        mDInfoRow = (LinearLayout) layout.findViewById(R.id.infoRow);
        mDSimilarRow = (LinearLayout) layout.findViewById(R.id.similarRow);
    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void setContent(final BaseItemDto program, final ProgramGridCell selectedGridView) {
        mProgram = program;
        mSelectedProgramView = selectedGridView;
        mDTitle.setText(program.getName());
        mDButtonRow.removeAllViews();
        if (program.getId() == null) {
            //empty item, just offer tune button
            mFirstButton = createTuneButton();
            mDInfoRow.removeAllViews();
            mDTimeline.removeAllViews();
            mDSummary.setText("");
            return;
        }

        mDSummary.setText(program.getOverview());
        if (mDSummary.getLineCount() < 2) {
            mDSummary.setGravity(Gravity.CENTER);
        } else {
            mDSummary.setGravity(Gravity.LEFT);
        }
        //TvApp.getApplication().getLogger().Debug("Text height: "+mDSummary.getHeight() + " (120 = "+Utils.convertDpToPixel(mActivity, 120)+")");

        // build timeline info
        TvManager.setTimelineRow(mActivity, mDTimeline, program);

        //info row
        InfoLayoutHelper.addInfoRow(mActivity, program, mDInfoRow, false, false);

        //buttons
        mFirstButton = null;
        Date now = new Date();
        Date local = Utils.convertToLocalDate(program.getStartDate());
        if (Utils.convertToLocalDate(program.getEndDate()).getTime() > now.getTime()) {
            if (local.getTime() <= now.getTime()) {
                // program in progress - tune first button
                mFirstButton = createTuneButton();
            }

            if (TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvManagement()) {
                if (program.getTimerId() != null) {
                    // cancel button
                    Button cancel = new Button(mActivity);
                    cancel.setText(mActivity.getResources().getString(R.string.lbl_cancel_recording));
                    cancel.setTextColor(Color.WHITE);
                    cancel.setBackground(mActivity.getResources().getDrawable(R.drawable.emby_button));
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TvApp.getApplication().getApiClient().CancelLiveTvTimerAsync(program.getTimerId(), new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    selectedGridView.setRecTimer(null);
                                    dismiss();
                                    Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                                }

                                @Override
                                public void onError(Exception ex) {
                                    Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                                }
                            });
                        }
                    });
                    mDButtonRow.addView(cancel);
                    if (mFirstButton == null) mFirstButton = cancel;
                    // recording info
                    mDRecordInfo.setText(local.getTime() <= now.getTime() ? mActivity.getResources().getString(R.string.msg_recording_now) : mActivity.getResources().getString(R.string.msg_will_record));
                } else {
                    // record button
                    Button rec = new Button(mActivity);
                    rec.setText(mActivity.getResources().getString(R.string.lbl_record));
                    rec.setTextColor(Color.WHITE);
                    rec.setBackground(mActivity.getResources().getDrawable(R.drawable.emby_button));
                    rec.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showRecordingOptions(false);
                        }
                    });
                    mDButtonRow.addView(rec);
                    if (mFirstButton == null) mFirstButton = rec;
                    mDRecordInfo.setText("");
                }
                if (Utils.isTrue(program.getIsSeries())) {
                    if (program.getSeriesTimerId() != null) {
                        // cancel series button
                        Button cancel = new Button(mActivity);
                        cancel.setText(mActivity.getResources().getString(R.string.lbl_cancel_series));
                        cancel.setTextColor(Color.WHITE);
                        cancel.setBackground(mActivity.getResources().getDrawable(R.drawable.emby_button));
                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new AlertDialog.Builder(mActivity)
                                        .setTitle(mActivity.getResources().getString(R.string.lbl_cancel_series))
                                        .setMessage(mActivity.getResources().getString(R.string.msg_cancel_entire_series))
                                        .setNegativeButton(R.string.lbl_no, null)
                                        .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                TvApp.getApplication().getApiClient().CancelLiveTvSeriesTimerAsync(program.getSeriesTimerId(), new EmptyResponse() {
                                                    @Override
                                                    public void onResponse() {
                                                        selectedGridView.setRecSeriesTimer(null);
                                                        dismiss();
                                                        Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                                                    }

                                                    @Override
                                                    public void onError(Exception ex) {
                                                        Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                                                    }
                                                });
                                            }
                                        }).show();
                            }
                        });
                        mDButtonRow.addView(cancel);
                    }else {
                        // record series button
                        Button rec = new Button(mActivity);
                        rec.setText(mActivity.getResources().getString(R.string.lbl_record_series));
                        rec.setTextColor(Color.WHITE);
                        rec.setBackground(mActivity.getResources().getDrawable(R.drawable.emby_button));
                        rec.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showRecordingOptions(true);
                            }
                        });
                        mDButtonRow.addView(rec);
                    }
                }

            }

            if (local.getTime() > now.getTime()) {
                // add tune to button for programs that haven't started yet
                createTuneButton();
            }


        } else {
            // program has already ended
            mDRecordInfo.setText(mActivity.getResources().getString(R.string.lbl_program_ended));
            mFirstButton = createTuneButton();
        }
//                if (program.getIsMovie()) {
//                    mDSimilarRow.setVisibility(View.VISIBLE);
//                    mPopup.setHeight(MOVIE_HEIGHT);
//                } else {
        mDSimilarRow.setVisibility(View.GONE);
//                    mPopup.setHeight(NORMAL_HEIGHT);
//
//                }
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

    private Button addButton(LinearLayout layout, int stringResource) {
        Button btn = new Button(mActivity);
        btn.setText(mActivity.getResources().getString(stringResource));
        btn.setTextColor(Color.WHITE);
        btn.setBackground(mActivity.getResources().getDrawable(R.drawable.emby_button));
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
        if (mRecordPopup == null) mRecordPopup = new RecordPopup(mActivity, mAnchor, mPosLeft, mPosTop, mPopup.getWidth());
        TvApp.getApplication().getApiClient().GetDefaultLiveTvTimerInfo(mProgram.getId(), new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                mRecordPopup.setContent(mProgram, response, mSelectedProgramView, recordSeries);
                mRecordPopup.show();
            }
        });
    }

}

