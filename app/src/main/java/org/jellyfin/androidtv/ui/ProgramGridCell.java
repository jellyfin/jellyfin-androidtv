package org.jellyfin.androidtv.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.livetv.ILiveTvGuide;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import java.util.Date;

public class ProgramGridCell extends RelativeLayout implements IRecordingIndicatorView {

    private ILiveTvGuide mActivity;
    private TextView mProgramName;
    private LinearLayout mInfoRow;
    private BaseItemDto mProgram;
    private ImageView mRecIndicator;
    private int mBackgroundColor = 0;
    private boolean isLast;
    private boolean isFirst;
    private final int IND_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 10);

    public ProgramGridCell(Context context, ILiveTvGuide activity, BaseItemDto program) {
        super(context);
        initComponent((Activity) context, activity, program);
    }

    private void initComponent(Activity context, ILiveTvGuide activity, BaseItemDto program) {
        mActivity = activity;
        mProgram = program;
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.program_grid_cell, this, false);
        this.addView(v);

        mProgramName = findViewById(R.id.programName);
        mInfoRow = findViewById(R.id.infoRow);
        mProgramName.setText(program.getName());
        mProgram = program;
        mProgramName.setFocusable(false);
        mInfoRow.setFocusable(false);
        mRecIndicator = findViewById(R.id.recIndicator);

        setCellBackground();

        if (program.getStartDate() != null && program.getEndDate() != null) {
            Date localStart = TimeUtils.convertToLocalDate(program.getStartDate());
            if (localStart.getTime() + 60000 < activity.getCurrentLocalStartDate()) {
                mProgramName.setText("<< "+mProgramName.getText());
                TextView time = new TextView(context);
                time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                time.setTextSize(12);
                time.setText(android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(TimeUtils.convertToLocalDate(program.getStartDate())));
                mInfoRow.addView(time);
            }
        }

        if (TvManager.getPrefs().showNewIndicator && BaseItemUtils.isNew(program) && (!TvManager.getPrefs().showPremiereIndicator || !Utils.isTrue(program.getIsPremiere()))) {
            InfoLayoutHelper.addSpacer(context, mInfoRow, "  ", 10);
            InfoLayoutHelper.addBlockText(context, mInfoRow, TvApp.getApplication().getString(R.string.lbl_new), 10, Color.GRAY, R.drawable.dark_green_gradient);
        }

        if (TvManager.getPrefs().showPremiereIndicator && Utils.isTrue(program.getIsPremiere())) {
            InfoLayoutHelper.addSpacer(context, mInfoRow, "  ", 10);
            InfoLayoutHelper.addBlockText(context, mInfoRow, TvApp.getApplication().getString(R.string.lbl_premiere), 10, Color.GRAY, R.drawable.dark_green_gradient);
        }

        if (TvManager.getPrefs().showRepeatIndicator && Utils.isTrue(program.getIsRepeat())) {
            InfoLayoutHelper.addSpacer(context, mInfoRow, "  ", 10);
            InfoLayoutHelper.addBlockText(context, mInfoRow, TvApp.getApplication().getString(R.string.lbl_repeat), 10, Color.GRAY, R.color.lb_default_brand_color);
        }

        if (program.getOfficialRating() != null && !program.getOfficialRating().equals("0")) {
            InfoLayoutHelper.addSpacer(context, mInfoRow, "  ", 10);
            InfoLayoutHelper.addBlockText(context, mInfoRow, program.getOfficialRating(), 10);
        }

        if (TvManager.getPrefs().showHDIndicator && Utils.isTrue(program.getIsHD())) {
            InfoLayoutHelper.addSpacer(context, mInfoRow, "  ", 10);
            InfoLayoutHelper.addBlockText(context, mInfoRow, "HD", 10);
        }

        if (program.getSeriesTimerId() != null) {
            mRecIndicator.setImageResource(program.getTimerId() != null ? R.drawable.ic_record_series_red : R.drawable.ic_record_series);
        } else if (program.getTimerId() != null) {
            mRecIndicator.setImageResource(R.drawable.ic_record_red);
        }

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showProgramOptions();
            }
        });

    }

    public void setCellBackground() {
        if (TvManager.getPrefs().colorCodeGuide) {
            if (Utils.isTrue(mProgram.getIsMovie())) {
                mBackgroundColor = getResources().getColor(R.color.guide_movie_bg);
            } else if (Utils.isTrue(mProgram.getIsNews())) {
                mBackgroundColor = getResources().getColor(R.color.guide_news_bg);
            } else if (Utils.isTrue(mProgram.getIsSports())) {
                mBackgroundColor = getResources().getColor(R.color.guide_sports_bg);
            } else if (Utils.isTrue(mProgram.getIsKids())) {
                mBackgroundColor = getResources().getColor(R.color.guide_kids_bg);
            }

            setBackgroundColor(mBackgroundColor);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            setBackgroundColor(Utils.getThemeColor(android.R.attr.colorAccent));

            mActivity.setSelectedProgram(this);
        } else {
            setBackgroundColor(mBackgroundColor);
        }

//        TvApp.getApplication().getLogger().Debug("Focus on " + mProgram.getName() + " was " + (gainFocus ? "gained" : "lost"));
    }

    public BaseItemDto getProgram() { return mProgram; }

    public void setLast() { isLast = true; }
    public boolean isLast() { return isLast; }
    public void setFirst() { isFirst = true; }
    public boolean isFirst() { return isFirst; }

    public void setRecTimer(String id) {
        mProgram.setTimerId(id);
        mRecIndicator.setImageResource(id != null ? (mProgram.getSeriesTimerId() != null ? R.drawable.ic_record_series_red : R.drawable.ic_record_red) : mProgram.getSeriesTimerId() != null ? R.drawable.ic_record_series : R.drawable.blank10x10);
    }
    public void setRecSeriesTimer(String id) {
        mProgram.setSeriesTimerId(id);
        mRecIndicator.setImageResource(id != null ? R.drawable.ic_record_series_red : R.drawable.blank10x10);
    }
}
