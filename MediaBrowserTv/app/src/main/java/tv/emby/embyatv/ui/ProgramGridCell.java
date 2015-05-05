package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tv.emby.embyatv.R;

/**
 * Created by Eric on 5/4/2015.
 */
public class ProgramGridCell extends RelativeLayout {

    private TextView mProgramName;
    private LinearLayout mInfoRow;

    public ProgramGridCell(Context context) {
        super(context);
        initComponent(context);
    }

    public ProgramGridCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        initComponent(context);
    }

    public ProgramGridCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initComponent(context);
    }

    private void initComponent(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.program_grid_cell, this, false);
        this.addView(v);

        mProgramName = (TextView) findViewById(R.id.programName);
        mInfoRow = (LinearLayout) findViewById(R.id.infoRow);
    }

    public void setProgramName(String name) {
        mProgramName.setText(name);
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
            setBackgroundColor(0);
        }
    }
}
