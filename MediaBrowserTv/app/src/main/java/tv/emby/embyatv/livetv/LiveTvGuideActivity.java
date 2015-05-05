package tv.emby.embyatv.livetv;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.zip.Inflater;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.ui.GuideChannelHeader;
import tv.emby.embyatv.ui.HorizontalScrollViewListener;
import tv.emby.embyatv.ui.ObservableHorizontalScrollView;
import tv.emby.embyatv.ui.ObservableScrollView;
import tv.emby.embyatv.ui.ProgramGridCell;
import tv.emby.embyatv.ui.ScrollViewListener;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/3/2015.
 */
public class LiveTvGuideActivity extends BaseActivity {

    private final int ROW_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(),50);
    private final int PIXELS_PER_MINUTE = Utils.convertDpToPixel(TvApp.getApplication(),6);

    private TextView mDisplayDate;
    private LinearLayout mChannels;
    private LinearLayout mTimeline;
    private LinearLayout mProgramRows;
    private ScrollView mChannelScroller;
    private HorizontalScrollView mTimelineScroller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.live_tv_guide);

        mDisplayDate = (TextView) findViewById(R.id.displayDate);
        mChannels = (LinearLayout) findViewById(R.id.channels);
        mTimeline = (LinearLayout) findViewById(R.id.timeline);
        mProgramRows = (LinearLayout) findViewById(R.id.programRows);

        mChannelScroller = (ScrollView) findViewById(R.id.channelScroller);
        ObservableScrollView programVScroller = (ObservableScrollView) findViewById(R.id.programVScroller);
        programVScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                mChannelScroller.scrollTo(x,y);
            }
        });

        mTimelineScroller = (HorizontalScrollView) findViewById(R.id.timelineHScroller);
        ObservableHorizontalScrollView programHScroller = (ObservableHorizontalScrollView) findViewById(R.id.programHScroller);
        programHScroller.setScrollViewListener(new HorizontalScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableHorizontalScrollView scrollView, int x, int y, int oldx, int oldy) {
                mTimelineScroller.scrollTo(x,y);
            }
        });

        programVScroller.setSmoothScrollingEnabled(true);
        mChannelScroller.setSmoothScrollingEnabled(true);
        mChannels.setFocusable(false);

        fillTimeLine();

        //test
        int halfHour = 30 * PIXELS_PER_MINUTE;
        int hour = 60 * PIXELS_PER_MINUTE;
        for (int i = 0; i < 20; i++) {
            GuideChannelHeader channel = new GuideChannelHeader(this);
            channel.setChannelName("Channel ");
            channel.setChannelNumber(String.valueOf(i));
            channel.setChannelImage("http://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/NBC_logo.svg/2000px-NBC_logo.svg.png");
            channel.setLayoutParams(new ViewGroup.LayoutParams(Utils.convertDpToPixel(this, 200), ROW_HEIGHT));
            mChannels.addView(channel);
            LinearLayout programRow = new LinearLayout(this);
            for (int j = 0; j < 5; j++) {
                ProgramGridCell program = new ProgramGridCell(this);

                program.setProgramName("Program " + j + " on channel " + i);
                program.setLayoutParams(new ViewGroup.LayoutParams(new Random().nextInt(10) > 5 ? halfHour : hour , ROW_HEIGHT));
                program.setFocusable(true);
                programRow.addView(program);
            }
            mProgramRows.addView(programRow);
        }
        mProgramRows.requestFocus();
    }

    private void fillTimeLine() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.MINUTE, 0);

        mDisplayDate.setText(android.text.format.DateFormat.getDateFormat(this).format(start.getTime()));
        Calendar current = (Calendar) start.clone();
        Calendar end = (Calendar) start.clone();
        int oneHour = 60 * PIXELS_PER_MINUTE;
        end.add(Calendar.HOUR, 24);
        while (current.before(end)) {
            TextView time = new TextView(this);
            time.setText(android.text.format.DateFormat.getTimeFormat(this).format(current.getTime()));
            time.setWidth(oneHour);
            mTimeline.addView(time);
            current.add(Calendar.HOUR_OF_DAY, 1);
        }

    }

}
