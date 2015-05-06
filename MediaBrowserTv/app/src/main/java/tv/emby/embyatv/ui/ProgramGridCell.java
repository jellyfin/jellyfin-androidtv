package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mediabrowser.model.livetv.ProgramInfoDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/4/2015.
 */
public class ProgramGridCell extends RelativeLayout {

    private TextView mProgramName;
    private LinearLayout mInfoRow;
    private ProgramInfoDto mProgram;
    private int mBackgroundColor = 0;

    public ProgramGridCell(Context context, ProgramInfoDto program) {
        super(context);
        initComponent(context, program);
    }

    private void initComponent(Context context, ProgramInfoDto program) {
        mProgram = program;
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.program_grid_cell, this, false);
        this.addView(v);

        mProgramName = (TextView) findViewById(R.id.programName);
        mInfoRow = (LinearLayout) findViewById(R.id.infoRow);
        mProgramName.setText(program.getName());
        mProgram = program;

        if (program.getIsMovie())
            mBackgroundColor = getResources().getColor(R.color.guide_movie_bg);
        else if (program.getIsNews())
            mBackgroundColor = getResources().getColor(R.color.guide_news_bg);
        else if (program.getIsSports())
            mBackgroundColor = getResources().getColor(R.color.guide_sports_bg);
        else if (program.getIsKids())
            mBackgroundColor = getResources().getColor(R.color.guide_kids_bg);

        setBackgroundColor(mBackgroundColor);

        if (program.getStartDate() != null && program.getEndDate() != null) {
            TextView time = new TextView(context);
            time.setText(android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(program.getStartDate()))
                    + "-" + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(program.getEndDate())));
            mInfoRow.addView(time);
        }

    }

    public void addInfo(View view) {
        mInfoRow.addView(view);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
        } else {
            setBackgroundColor(mBackgroundColor);
        }

        TvApp.getApplication().getLogger().Debug("Focus on "+mProgram.getName()+ " was " +(gainFocus ? "gained" : "lost"));
    }
}
