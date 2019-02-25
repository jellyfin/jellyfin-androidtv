package org.jellyfin.androidtv.presentation;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.NumberFormat;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.livetv.ChannelInfoDto;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by spam on 6/28/2016.
 */
public class MyChannelCardView extends FrameLayout {
    private TextView mChannelName;
    private TextView mProgramName;
    private TextView mTimeSlot;
    private ProgressBar mProgress;

    public MyChannelCardView(Context context) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.quick_channel_card, this);
        mChannelName = (TextView) v.findViewById(R.id.name);
        mProgramName = (TextView) v.findViewById(R.id.program);
        mTimeSlot = (TextView) v.findViewById(R.id.time);
        mProgress = (ProgressBar) v.findViewById(R.id.progress);

    }

    public void setItem(final ChannelInfoDto channel) {
        mChannelName.setText(channel.getNumber() + " " + channel.getName());
        BaseItemDto program = channel.getCurrentProgram();
        if (program != null) {
            if (program.getEndDate() != null && System.currentTimeMillis() > Utils.convertToLocalDate(program.getEndDate()).getTime()) {
                //need to update program
                TvApp.getApplication().getApiClient().GetItemAsync(channel.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (response.getCurrentProgram() != null) updateDisplay(response.getCurrentProgram());
                        channel.setCurrentProgram(response.getCurrentProgram());
                    }
                });
            } else {
                updateDisplay(program);
            }
        } else {
            mProgramName.setText(R.string.no_program_data);
            mTimeSlot.setText("");
            mProgress.setProgress(0);
        }

    }

    private void updateDisplay(BaseItemDto program) {
        mProgramName.setText(program.getName());
        if (program.getStartDate() != null && program.getEndDate() != null) {
            mTimeSlot.setText(android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(program.getStartDate()))
                    + "-" + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(program.getEndDate())));
            long start = Utils.convertToLocalDate(program.getStartDate()).getTime();
            long current = System.currentTimeMillis() - start;
            if (current > 0)
            {
                long duration = Utils.convertToLocalDate(program.getEndDate()).getTime() - start;
                mProgress.setProgress((int)((current*100.0/duration)));
            } else {
                mProgress.setProgress(0);
            }

        } else {
            mTimeSlot.setText("");
            mProgress.setProgress(0);
        }

    }

}
