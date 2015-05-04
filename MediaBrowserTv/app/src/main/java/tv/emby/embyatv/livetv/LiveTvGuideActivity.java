package tv.emby.embyatv.livetv;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import tv.emby.embyatv.R;
import tv.emby.embyatv.base.BaseActivity;

/**
 * Created by Eric on 5/3/2015.
 */
public class LiveTvGuideActivity extends BaseActivity {

    private final int ROW_HEIGHT = 30;
    private final int DP_PER_MINUTE = 2;

    private LinearLayout mChannels;
    private LinearLayout mTimeline;
    private LinearLayout mProgramRows;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.live_tv_guide);

        mChannels = (LinearLayout) findViewById(R.id.channels);
        mTimeline = (LinearLayout) findViewById(R.id.timeline);
        mProgramRows = (LinearLayout) findViewById(R.id.programRows);

        ScrollView channelScroller = (ScrollView) findViewById(R.id.channelScroller);
        ScrollView programScroller = (ScrollView) findViewById(R.id.programVScroller);

        programScroller.setSmoothScrollingEnabled(true);
        channelScroller.setSmoothScrollingEnabled(true);

        //test
        for (int i = 0; i < 20; i++) {
            TextView channel = new TextView(this);
            channel.setText("Channel "+i);
            mChannels.addView(channel);
            LinearLayout programRow = new LinearLayout(this);
            for (int j = 0; j < 5; j++) {
                TextView program = new TextView(this);
                program.setText("Program "+j+" on channel "+i);
                program.setWidth(200 + (10 * i));
                program.setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
                program.setFocusable(true);
                programRow.addView(program);
            }
            mProgramRows.addView(programRow);
        }
    }

}
