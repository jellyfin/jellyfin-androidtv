package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 6/3/2015.
 */
public class RecordPopup {
    final int SERIES_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 540);
    final int NORMAL_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 400);
    final List<String> DayValues = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

    PopupWindow mPopup;
    String mProgramId;
    IRecordingIndicatorView mSelectedView;
    View mAnchorView;
    int mPosLeft;
    int mPosTop;
    boolean mRecordSeries;

    BaseActivity mActivity;
    TextView mDTitle;
    TextView mDSummary;
    LinearLayout mDTimeline;
    View mSeriesOptions;
    GridLayout mWeekdayOptions;
    CheckBox[] mWeekdayChecks = new CheckBox[7];
    EditText mPrePadding;
    EditText mPostPadding;
    CheckBox mPreRequired;
    CheckBox mPostRequired;
    CheckBox mOnlyNew;
    CheckBox mAnyTime;
    CheckBox mAnyChannel;
    Button mOkButton;
    Button mCancelButton;

    public RecordPopup(BaseActivity activity, View anchorView, int left, int top, int width) {
        mActivity = activity;
        mAnchorView = anchorView;
        mPosLeft = left;
        mPosTop = top;
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.program_record_popup, null);
        Typeface roboto = Typeface.createFromAsset(mActivity.getAssets(), "fonts/Roboto-Light.ttf");
        mPopup = new PopupWindow(layout, width, NORMAL_HEIGHT);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
        mDTitle = (TextView)layout.findViewById(R.id.title);
        mDTitle.setTypeface(roboto);
        mDSummary = (TextView)layout.findViewById(R.id.summary);
        mDSummary.setTypeface(roboto);

        mPrePadding = (EditText) layout.findViewById(R.id.prePadding);
        mPostPadding = (EditText) layout.findViewById(R.id.postPadding);
        mPreRequired = (CheckBox) layout.findViewById(R.id.prePadReq);
        mPostRequired = (CheckBox) layout.findViewById(R.id.postPadReq);

        mOnlyNew = (CheckBox) layout.findViewById(R.id.onlyNew);
        mAnyChannel = (CheckBox) layout.findViewById(R.id.anyChannel);
        mAnyTime = (CheckBox) layout.findViewById(R.id.anyTime);

        mSeriesOptions = layout.findViewById(R.id.seriesOptions);
        mWeekdayOptions = (GridLayout) layout.findViewById(R.id.weekdayOptions);
        int i = -1;
        for (String day : DateFormatSymbols.getInstance().getWeekdays()) {
            if (i < 0) {
                //first one is blank
                i++;
                continue;
            }
            CheckBox cbx = new CheckBox(mActivity);
            cbx.setText(day);
            cbx.setTextSize(14);
            mWeekdayChecks[i++] = cbx;
            mWeekdayOptions.addView(cbx);
        }

        mOkButton = (Button) layout.findViewById(R.id.okButton);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvApp.getApplication().getApiClient().GetDefaultLiveTvTimerInfo(mProgramId, new Response<SeriesTimerInfoDto>() {
                    @Override
                    public void onResponse(SeriesTimerInfoDto response) {
                        response.setPrePaddingSeconds(Integer.valueOf(mPrePadding.getText().toString())*60);
                        response.setPostPaddingSeconds(Integer.valueOf(mPostPadding.getText().toString())*60);
                        response.setIsPrePaddingRequired(mPreRequired.isChecked());
                        response.setIsPostPaddingRequired(mPostRequired.isChecked());

                        if (mRecordSeries) {
                            response.setDays(new ArrayList<String>());
                            for (int i = 0; i < 7; i++) {
                                if (mWeekdayChecks[i].isChecked()) response.getDays().add(DayValues.get(i));
                            }
                            response.setRecordNewOnly(mOnlyNew.isChecked());
                            response.setRecordAnyChannel(mAnyChannel.isChecked());
                            response.setRecordAnyTime(mAnyTime.isChecked());

                            TvApp.getApplication().getApiClient().CreateLiveTvSeriesTimerAsync(response, new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    mPopup.dismiss();
                                    mActivity.sendMessage(CustomMessage.ActionComplete);
                                    // we have to re-retrieve the program to get the timer id
                                    TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramId, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                        @Override
                                        public void onResponse(BaseItemDto response) {
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

                        } else {
                            TvApp.getApplication().getApiClient().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    mPopup.dismiss();
                                    mActivity.sendMessage(CustomMessage.ActionComplete);
                                    // we have to re-retrieve the program to get the timer id
                                    TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramId, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                        @Override
                                        public void onResponse(BaseItemDto response) {
                                            mSelectedView.setRecTimer(response.getTimerId());
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

    public void setContent(BaseItemDto program, SeriesTimerInfoDto defaults, IRecordingIndicatorView selectedView, boolean recordSeries) {
        mProgramId = program.getId();
        mRecordSeries = recordSeries;
        mSelectedView = selectedView;

        mDTitle.setText(program.getName());
        mDSummary.setText(program.getOverview());
        if (mDSummary.getLineCount() < 2) {
            mDSummary.setGravity(Gravity.CENTER);
        } else {
            mDSummary.setGravity(Gravity.LEFT);
        }

        //if already started then can't require pre padding
        if (!recordSeries) {
            Date local = Utils.convertToLocalDate(program.getStartDate());
            Date now = new Date();
            mPreRequired.setEnabled(local.getTime() > now.getTime());
        }

        // build timeline info
        setTimelineRow(mDTimeline, program);

        // set defaults
        mPrePadding.setText(String.valueOf(defaults.getPrePaddingSeconds()/60));
        mPostPadding.setText(String.valueOf(defaults.getPostPaddingSeconds()/60));
        mPreRequired.setChecked(defaults.getIsPrePaddingRequired());
        mPostRequired.setChecked(defaults.getIsPostPaddingRequired());

        if (recordSeries) {
            mPopup.setHeight(SERIES_HEIGHT);
            mSeriesOptions.setVisibility(View.VISIBLE);

            // select proper days
            int i = 0;
            for (CheckBox day : mWeekdayChecks) {
                day.setChecked(defaults.getDays().contains(DayValues.get(i)));
                i++;
            }

            // and other options
            mAnyChannel.setChecked(defaults.getRecordAnyChannel());
            mOnlyNew.setChecked(defaults.getRecordNewOnly());
            mAnyTime.setChecked(defaults.getRecordAnyTime());

        } else {
            mPopup.setHeight(NORMAL_HEIGHT);
            mSeriesOptions.setVisibility(View.GONE);
        }

    }

    public void show() {
        mPopup.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, mPosLeft, mPosTop);
        mOkButton.requestFocus();

    }

    public void dismiss() {
        if (mPopup != null && mPopup.isShowing()) mPopup.dismiss();
    }

    private void setTimelineRow(LinearLayout timelineRow, BaseItemDto program) {
        timelineRow.removeAllViews();
        Date local = Utils.convertToLocalDate(program.getStartDate());
        TextView on = new TextView(mActivity);
        on.setText(mActivity.getString(R.string.lbl_on));
        timelineRow.addView(on);
        TextView channel = new TextView(mActivity);
        channel.setText(program.getChannelName());
        channel.setTypeface(null, Typeface.BOLD);
        channel.setTextColor(mActivity.getResources().getColor(android.R.color.holo_blue_light));
        timelineRow.addView(channel);
        TextView datetime = new TextView(mActivity);
        datetime.setText(Utils.getFriendlyDate(local)+ " @ "+android.text.format.DateFormat.getTimeFormat(mActivity).format(local)+ " ("+ DateUtils.getRelativeTimeSpanString(local.getTime())+")");
        timelineRow.addView(datetime);

    }

}

