package org.jellyfin.androidtv.ui;

import static org.koin.java.KoinJavaComponent.inject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerInfoDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import kotlin.Lazy;

public class RecordPopup {
    PopupWindow mPopup;
    String mProgramId;
    SeriesTimerInfoDto mCurrentOptions;
    RecordingIndicatorView mSelectedView;
    View mAnchorView;
    int mPosLeft;
    int mPosTop;
    boolean mRecordSeries;

    BaseActivity mActivity;
    TextView mDTitle;
    LinearLayout mDTimeline;
    View mSeriesOptions;
    Spinner mPrePadding;
    Spinner mPostPadding;

    CheckBox mOnlyNew;
    CheckBox mAnyTime;
    CheckBox mAnyChannel;
    Button mOkButton;
    Button mCancelButton;

    String MINUTE = TvApp.getApplication().getString(R.string.lbl_minute);
    String MINUTES = TvApp.getApplication().getString(R.string.lbl_minutes);
    String HOURS = TvApp.getApplication().getString(R.string.lbl_hours);
    ArrayList<String> mPaddingDisplayOptions = new ArrayList<>(Arrays.asList(TvApp.getApplication().getString(R.string.lbl_on_schedule),"1  "+MINUTE,"5  "+MINUTES,"15 "+MINUTES,"30 "+MINUTES,"60 "+MINUTES,"90 "+MINUTES,"2  "+HOURS,"3  "+HOURS));
    ArrayList<Integer> mPaddingValues = new ArrayList<>(Arrays.asList(0,60,300,900,1800,3600,5400,7200,10800));

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

    public RecordPopup(BaseActivity activity, View anchorView, int left, int top, int width) {
        mActivity = activity;
        mAnchorView = anchorView;
        mPosLeft = left;
        mPosTop = top;
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.new_program_record_popup, null);
        int popupHeight = Utils.convertDpToPixel(activity, 330);
        mPopup = new PopupWindow(layout, width, popupHeight);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss
        mDTitle = (TextView)layout.findViewById(R.id.title);

        mPrePadding = (Spinner) layout.findViewById(R.id.prePadding);
        mPrePadding.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, mPaddingDisplayOptions));
        mPrePadding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentOptions.setPrePaddingSeconds(mPaddingValues.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mPostPadding = (Spinner) layout.findViewById(R.id.postPadding);
        mPostPadding.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, mPaddingDisplayOptions));
        mPostPadding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentOptions.setPostPaddingSeconds(mPaddingValues.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mOnlyNew = (CheckBox) layout.findViewById(R.id.onlyNew);
        mAnyChannel = (CheckBox) layout.findViewById(R.id.anyChannel);
        mAnyTime = (CheckBox) layout.findViewById(R.id.anyTime);

        mSeriesOptions = layout.findViewById(R.id.seriesOptions);

        mOkButton = (Button) layout.findViewById(R.id.okButton);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordSeries) {
                    mCurrentOptions.setRecordNewOnly(mOnlyNew.isChecked());
                    mCurrentOptions.setRecordAnyChannel(mAnyChannel.isChecked());
                    mCurrentOptions.setRecordAnyTime(mAnyTime.isChecked());

                    apiClient.getValue().UpdateLiveTvSeriesTimerAsync(mCurrentOptions, new EmptyResponse() {
                        @Override
                        public void onResponse() {
                            mPopup.dismiss();
                            mActivity.sendMessage(CustomMessage.ActionComplete);
                            Utils.showToast(mActivity, R.string.msg_settings_updated);
                        }

                        @Override
                        public void onError(Exception ex) {
                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                        }

                    });

                } else {
                    TimerInfoDto updated = new TimerInfoDto();
                    updated.setProgramId(mProgramId);
                    updated.setPrePaddingSeconds(mCurrentOptions.getPrePaddingSeconds());
                    updated.setPostPaddingSeconds(mCurrentOptions.getPostPaddingSeconds());
                    updated.setIsPrePaddingRequired(mCurrentOptions.getIsPrePaddingRequired());
                    updated.setIsPostPaddingRequired(mCurrentOptions.getIsPostPaddingRequired());

                    apiClient.getValue().UpdateLiveTvTimerAsync(updated, new EmptyResponse() {
                        @Override
                        public void onResponse() {
                            mPopup.dismiss();
                            mActivity.sendMessage(CustomMessage.ActionComplete);
                            // we have to re-retrieve the program to get the timer id
                            apiClient.getValue().GetLiveTvProgramAsync(mProgramId, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    mSelectedView.setRecTimer(response.getTimerId());
                                    mSelectedView.setRecSeriesTimer(response.getSeriesTimerId());
                                }
                            });
                            Utils.showToast(mActivity, R.string.msg_set_to_record);
                        }

                        @Override
                        public void onError(Exception ex) {
                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                        }
                    });
                }
            }
        });

        mCancelButton = (Button) layout.findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopup.dismiss();
            }
        });

        mDTimeline = (LinearLayout) layout.findViewById(R.id.timeline);
    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void setContent(Context context, BaseItemDto program, SeriesTimerInfoDto current, RecordingIndicatorView selectedView, boolean recordSeries) {
        mProgramId = program.getId();
        mCurrentOptions = current;
        mRecordSeries = recordSeries;
        mSelectedView = selectedView;

        mDTitle.setText(program.getName());

        // build timeline info
        setTimelineRow(context, mDTimeline, program);

        // set defaults
        mPrePadding.setSelection(getPaddingNdx(current.getPrePaddingSeconds()));
        mPostPadding.setSelection(getPaddingNdx(current.getPostPaddingSeconds()));

        if (recordSeries) {
            mPopup.setHeight(Utils.convertDpToPixel(context, 420));
            mSeriesOptions.setVisibility(View.VISIBLE);

            // and other options
            mAnyChannel.setChecked(current.getRecordAnyChannel());
            mOnlyNew.setChecked(current.getRecordNewOnly());
            mAnyTime.setChecked(current.getRecordAnyTime());

        } else {
            mPopup.setHeight(Utils.convertDpToPixel(context, 330));
            mSeriesOptions.setVisibility(View.GONE);
        }

    }

    private int getPaddingNdx(int seconds) {
        for (int i = 0; i < mPaddingValues.size(); i++) {
            if (mPaddingValues.get(i) > seconds) return i-1;
        }

        return 0;
    }

    public void show() {
        mPopup.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, mPosLeft, mPosTop);
        mOkButton.requestFocus();

    }

    public void dismiss() {
        if (mPopup != null && mPopup.isShowing()) mPopup.dismiss();
    }

    private void setTimelineRow(Context context, LinearLayout timelineRow, BaseItemDto program) {
        timelineRow.removeAllViews();
        if (program.getStartDate() == null) return;

        Date local = TimeUtils.convertToLocalDate(program.getStartDate());
        TextView on = new TextView(mActivity);
        on.setText(mActivity.getString(R.string.lbl_on));
        timelineRow.addView(on);
        TextView channel = new TextView(mActivity);
        channel.setText(program.getChannelName());
        channel.setTypeface(null, Typeface.BOLD);
        channel.setTextColor(mActivity.getResources().getColor(android.R.color.holo_blue_light));
        timelineRow.addView(channel);
        TextView datetime = new TextView(mActivity);
        datetime.setText(TimeUtils.getFriendlyDate(context, local)+ " @ "+android.text.format.DateFormat.getTimeFormat(mActivity).format(local)+ " ("+ DateUtils.getRelativeTimeSpanString(local.getTime())+")");
        timelineRow.addView(datetime);

    }

}

