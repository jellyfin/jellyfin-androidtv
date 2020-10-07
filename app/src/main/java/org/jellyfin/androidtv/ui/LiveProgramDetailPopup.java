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

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.livetv.ILiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;

import java.util.Date;

import kotlin.Lazy;
import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.inject;

public class LiveProgramDetailPopup {
    private final int NORMAL_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 400);

    private PopupWindow mPopup;
    private BaseItemDto mProgram;
    private ProgramGridCell mSelectedProgramView;
    private BaseActivity mActivity;
    private ILiveTvGuide mTvGuide;
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

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

    public LiveProgramDetailPopup(BaseActivity activity, ILiveTvGuide tvGuide, int width, EmptyResponse tuneAction) {
        mActivity = activity;
        mTvGuide = tvGuide;
        mTuneAction = tuneAction;
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.program_detail_popup, null);
        mPopup = new PopupWindow(layout, width, NORMAL_HEIGHT);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss
        mPopup.setAnimationStyle(R.style.PopupSlideInTop);
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
        mDTitle.setText(program.getName());
        mDButtonRow.removeAllViews();
        if (program.getId() == null) {
            //empty item, just offer tune button
            mFirstButton = createTuneButton();
            createFavoriteButton();
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
        Date local = TimeUtils.convertToLocalDate(program.getStartDate());
        if (TimeUtils.convertToLocalDate(program.getEndDate()).getTime() > now.getTime()) {
            if (local.getTime() <= now.getTime()) {
                // program in progress - tune first button
                mFirstButton = createTuneButton();
            }

            if (TvApp.getApplication().canManageRecordings()) {
                if (program.getTimerId() != null) {
                    // cancel button
                    Button cancel = new Button(mActivity);
                    cancel.setText(mActivity.getResources().getString(R.string.lbl_cancel_recording));
                    cancel.setTextColor(Color.WHITE);
                    cancel.setBackgroundResource(R.drawable.jellyfin_button);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            apiClient.getValue().CancelLiveTvTimerAsync(program.getTimerId(), new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    selectedGridView.setRecTimer(null);
                                    program.setTimerId(null);
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
                    rec.setBackgroundResource(R.drawable.jellyfin_button);
                    rec.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Create one-off recording with defaults
                            apiClient.getValue().GetDefaultLiveTvTimerInfo(mProgram.getId(), new Response<SeriesTimerInfoDto>() {
                                @Override
                                public void onResponse(SeriesTimerInfoDto response) {
                                    apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            // we have to re-retrieve the program to get the timer id
                                            apiClient.getValue().GetLiveTvProgramAsync(mProgram.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                                @Override
                                                public void onResponse(BaseItemDto response) {
                                                    mProgram = response;
                                                    mSelectedProgramView.setRecSeriesTimer(response.getSeriesTimerId());
                                                    mSelectedProgramView.setRecTimer(response.getTimerId());
                                                    Utils.showToast(mActivity, R.string.msg_set_to_record);
                                                    dismiss();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            Timber.e(ex, "Error creating recording");
                                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception exception) {
                                    Timber.e(exception, "Error creating recording");
                                    Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                }
                            });
                        }
                    });
                    mDButtonRow.addView(rec);
                    if (mFirstButton == null) mFirstButton = rec;
                    mDRecordInfo.setText(program.getSeriesTimerId() == null ? "" : mActivity.getString(R.string.lbl_episode_not_record));
                }
                if (Utils.isTrue(program.getIsSeries())) {
                    if (program.getSeriesTimerId() != null) {
                        // cancel series button
                        Button cancel = new Button(mActivity);
                        cancel.setText(mActivity.getResources().getString(R.string.lbl_cancel_series));
                        cancel.setTextColor(Color.WHITE);
                        cancel.setBackgroundResource(R.drawable.jellyfin_button);
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
                                                apiClient.getValue().CancelLiveTvSeriesTimerAsync(program.getSeriesTimerId(), new EmptyResponse() {
                                                    @Override
                                                    public void onResponse() {
                                                        selectedGridView.setRecSeriesTimer(null);
                                                        program.setSeriesTimerId(null);
                                                        mSeriesSettingsButton.setVisibility(View.GONE);
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
                        rec.setBackgroundResource(R.drawable.jellyfin_button);
                        rec.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //Create series recording with defaults
                                apiClient.getValue().GetDefaultLiveTvTimerInfo(mProgram.getId(), new Response<SeriesTimerInfoDto>() {
                                    @Override
                                    public void onResponse(SeriesTimerInfoDto response) {
                                        apiClient.getValue().CreateLiveTvSeriesTimerAsync(response, new EmptyResponse() {
                                            @Override
                                            public void onResponse() {
                                                // we have to re-retrieve the program to get the timer id
                                                apiClient.getValue().GetLiveTvProgramAsync(mProgram.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                                    @Override
                                                    public void onResponse(BaseItemDto response) {
                                                        mProgram = response;
                                                        mSelectedProgramView.setRecSeriesTimer(response.getSeriesTimerId());
                                                        mSelectedProgramView.setRecTimer(response.getTimerId());
                                                        if (mSeriesSettingsButton != null) mSeriesSettingsButton.setVisibility(View.VISIBLE);
                                                        Utils.showToast(mActivity, R.string.msg_set_to_record);
                                                        dismiss();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(Exception ex) {
                                                Timber.e(ex, "Error creating recording");
                                                Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        Timber.e(exception, "Error creating recording");
                                        Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                    }
                                });
                            }
                        });
                        mDButtonRow.addView(rec);
                    }

                    // manage series button
                    mSeriesSettingsButton = new Button(mActivity);
                    mSeriesSettingsButton.setText(mActivity.getResources().getString(R.string.lbl_series_settings));
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

            if (local.getTime() > now.getTime()) {
                // add tune to button for programs that haven't started yet
                createTuneButton();
            }

            createFavoriteButton();


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

    public android.widget.ImageButton createFavoriteButton() {
        ChannelInfoDto channel = TvManager.getChannel(TvManager.getAllChannelsIndex(mProgram.getChannelId()));
        boolean isFav = channel.getUserData() != null && channel.getUserData().getIsFavorite();

        android.widget.ImageButton fave = addImgButton(mDButtonRow, isFav ? R.drawable.ic_heart_red : R.drawable.ic_heart);
        fave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apiClient.getValue().UpdateFavoriteStatusAsync(channel.getId(), TvApp.getApplication().getCurrentUser().getId(), !channel.getUserData().getIsFavorite(), new Response<UserItemDataDto>() {
                    @Override
                    public void onResponse(UserItemDataDto response) {
                        channel.setUserData(response);
                        fave.setImageDrawable(ContextCompat.getDrawable(mActivity, response.getIsFavorite() ? R.drawable.ic_heart_red : R.drawable.ic_heart));
                        mTvGuide.refreshFavorite(channel.getId());
                        TvApp.getApplication().dataRefreshService.setLastFavoriteUpdate(System.currentTimeMillis());
                    }
                });
            }
        });

        return fave;
    }

    private android.widget.ImageButton addImgButton(LinearLayout layout, int imgResource) {
        android.widget.ImageButton btn = new android.widget.ImageButton(mActivity);
        btn.setImageDrawable(ContextCompat.getDrawable(mActivity, imgResource));
        btn.setBackgroundResource(R.drawable.jellyfin_button);
        layout.addView(btn);
        return btn;
    }

    private Button addButton(LinearLayout layout, int stringResource) {
        Button btn = new Button(mActivity);
        btn.setText(mActivity.getResources().getString(stringResource));
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
        if (mRecordPopup == null) mRecordPopup = new RecordPopup(mActivity, mAnchor, mPosLeft, mPosTop, mPopup.getWidth());
        apiClient.getValue().GetLiveTvSeriesTimerAsync(mProgram.getSeriesTimerId(), new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                mRecordPopup.setContent(mProgram, response, mSelectedProgramView, recordSeries);
                mRecordPopup.show();
            }
        });
    }

}

