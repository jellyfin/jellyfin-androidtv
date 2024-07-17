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

import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.util.ContextExtensionsKt;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto;
import org.jellyfin.sdk.model.api.TimerInfoDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import kotlin.Lazy;

public class RecordPopup {
    PopupWindow mPopup;
    UUID mProgramId;
    SeriesTimerInfoDto mCurrentOptions;
    RecordingIndicatorView mSelectedView;
    View mAnchorView;
    int mPosLeft;
    int mPosTop;
    boolean mRecordSeries;

    Context mContext;
    final Lifecycle lifecycle;
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

    ArrayList<String> mPaddingDisplayOptions;
    ArrayList<Integer> mPaddingValues = new ArrayList<>(Arrays.asList(0,60,300,900,1800,3600,5400,7200,10800));

    private Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);

    public RecordPopup(Context context, Lifecycle lifecycle, View anchorView, int left, int top, int width) {
        mContext = context;
        this.lifecycle = lifecycle;
        mAnchorView = anchorView;
        mPosLeft = left;
        mPosTop = top;

        mPaddingDisplayOptions = new ArrayList<>(Arrays.asList(
                context.getString(R.string.lbl_on_schedule),
                ContextExtensionsKt.getQuantityString(context, R.plurals.minutes, 1),
                ContextExtensionsKt.getQuantityString(context, R.plurals.minutes, 5),
                ContextExtensionsKt.getQuantityString(context, R.plurals.minutes, 15),
                ContextExtensionsKt.getQuantityString(context, R.plurals.minutes, 30),
                ContextExtensionsKt.getQuantityString(context, R.plurals.minutes, 60),
                ContextExtensionsKt.getQuantityString(context, R.plurals.minutes, 90),
                ContextExtensionsKt.getQuantityString(context, R.plurals.hours, 2),
                ContextExtensionsKt.getQuantityString(context, R.plurals.hours, 3)
        ));

        View layout = LayoutInflater.from(context).inflate(R.layout.new_program_record_popup, null);
        int popupHeight = Utils.convertDpToPixel(context, 330);
        mPopup = new PopupWindow(layout, width, popupHeight);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss
        mDTitle = layout.findViewById(R.id.title);

        mPrePadding = layout.findViewById(R.id.prePadding);
        mPrePadding.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, mPaddingDisplayOptions));
        mPrePadding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentOptions = RecordPopupHelperKt.copyWithPrePaddingSeconds(mCurrentOptions, mPaddingValues.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mPostPadding = (Spinner) layout.findViewById(R.id.postPadding);
        mPostPadding.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, mPaddingDisplayOptions));
        mPostPadding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentOptions = RecordPopupHelperKt.copyWithPostPaddingSeconds(mCurrentOptions, mPaddingValues.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mOnlyNew = layout.findViewById(R.id.onlyNew);
        mAnyChannel = layout.findViewById(R.id.anyChannel);
        mAnyTime = layout.findViewById(R.id.anyTime);

        mSeriesOptions = layout.findViewById(R.id.seriesOptions);

        mOkButton = layout.findViewById(R.id.okButton);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordSeries) {
                    mCurrentOptions = RecordPopupHelperKt.copyWithFilters(mCurrentOptions, mOnlyNew.isChecked(), mAnyChannel.isChecked(), mAnyTime.isChecked());

                    RecordPopupHelperKt.updateSeriesTimer(RecordPopup.this, mCurrentOptions, () -> {
                        mPopup.dismiss();
                        customMessageRepository.getValue().pushMessage(CustomMessage.ActionComplete.INSTANCE);
                        Utils.showToast(mContext, R.string.msg_settings_updated);
                        return null;
                    });
                } else {
                    TimerInfoDto updated = RecordPopupHelperKt.createProgramTimerInfo(mProgramId, mCurrentOptions);

                    RecordPopupHelperKt.updateTimer(RecordPopup.this, updated, () -> {
                        mPopup.dismiss();
                        customMessageRepository.getValue().pushMessage(CustomMessage.ActionComplete.INSTANCE);
                        // we have to re-retrieve the program to get the timer id
                        RecordPopupHelperKt.getLiveTvProgram(RecordPopup.this, mProgramId, program -> {
                            mSelectedView.setRecTimer(program.getTimerId());
                            mSelectedView.setRecSeriesTimer(program.getSeriesTimerId());
                            return null;
                        });
                        Utils.showToast(mContext, R.string.msg_set_to_record);
                        return null;
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

        LocalDateTime local = program.getStartDate();
        TextView on = new TextView(mContext);
        on.setText(mContext.getString(R.string.lbl_on));
        timelineRow.addView(on);
        TextView channel = new TextView(mContext);
        channel.setText(program.getChannelName());
        channel.setTypeface(null, Typeface.BOLD);
        channel.setTextColor(mContext.getResources().getColor(android.R.color.holo_blue_light));
        timelineRow.addView(channel);
        TextView datetime = new TextView(mContext);
        datetime.setText(new StringBuilder()
                .append(TimeUtils.getFriendlyDate(context, local))
                .append(" @ ")
                .append(DateTimeExtensionsKt.getTimeFormatter(mContext).format(local))
                .append(" (")
                .append(DateUtils.getRelativeTimeSpanString(local.toInstant(ZoneOffset.UTC).toEpochMilli(), Instant.now().toEpochMilli(), 0))
                .append(")")
        );
        timelineRow.addView(datetime);

    }

}

