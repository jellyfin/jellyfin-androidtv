package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
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
import mediabrowser.model.livetv.TimerInfoDto;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;
import org.jellyfin.androidtv.base.CustomMessage;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by Eric on 6/3/2015.
 */
public class RecordPopup {
    final int SERIES_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 420);
    final int NORMAL_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 330);

    PopupWindow mPopup;
    String mProgramId;
    SeriesTimerInfoDto mCurrentOptions;
    IRecordingIndicatorView mSelectedView;
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

    public RecordPopup(BaseActivity activity, View anchorView, int left, int top, int width) {
        mActivity = activity;
        mAnchorView = anchorView;
        mPosLeft = left;
        mPosTop = top;
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.new_program_record_popup, null);
        Typeface roboto = Typeface.createFromAsset(mActivity.getAssets(), "fonts/Roboto-Light.ttf");
        mPopup = new PopupWindow(layout, width, NORMAL_HEIGHT);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
        mDTitle = (TextView)layout.findViewById(R.id.title);
        mDTitle.setTypeface(roboto);

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

                    TvApp.getApplication().getApiClient().UpdateLiveTvSeriesTimerAsync(mCurrentOptions, new EmptyResponse() {
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

                    TvApp.getApplication().getApiClient().UpdateLiveTvTimerAsync(updated, new EmptyResponse() {
                        @Override
                        public void onResponse() {
                            mPopup.dismiss();
                            mActivity.sendMessage(CustomMessage.ActionComplete);
                            // we have to re-retrieve the program to get the timer id
                            TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramId, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
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

    public void setContent(BaseItemDto program, SeriesTimerInfoDto current, IRecordingIndicatorView selectedView, boolean recordSeries) {
        mProgramId = program.getId();
        mCurrentOptions = current;
        mRecordSeries = recordSeries;
        mSelectedView = selectedView;

        mDTitle.setText(program.getName());

        // build timeline info
        setTimelineRow(mDTimeline, program);

        // set defaults
        mPrePadding.setSelection(getPaddingNdx(current.getPrePaddingSeconds()));
        mPostPadding.setSelection(getPaddingNdx(current.getPostPaddingSeconds()));

        if (recordSeries) {
            mPopup.setHeight(SERIES_HEIGHT);
            mSeriesOptions.setVisibility(View.VISIBLE);

            // and other options
            mAnyChannel.setChecked(current.getRecordAnyChannel());
            mOnlyNew.setChecked(current.getRecordNewOnly());
            mAnyTime.setChecked(current.getRecordAnyTime());

        } else {
            mPopup.setHeight(NORMAL_HEIGHT);
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

    private void setTimelineRow(LinearLayout timelineRow, BaseItemDto program) {
        timelineRow.removeAllViews();
        if (program.getStartDate() == null) return;

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

