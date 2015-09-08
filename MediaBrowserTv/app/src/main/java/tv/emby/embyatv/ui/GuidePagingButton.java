package tv.emby.embyatv.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tv.emby.embyatv.R;
import tv.emby.embyatv.livetv.ILiveTvGuide;
import tv.emby.embyatv.livetv.LiveTvGuideActivity;

/**
 * Created by Eric on 5/8/2015.
 */
public class GuidePagingButton extends RelativeLayout {

    private int startRow;
    private RelativeLayout us;


    public GuidePagingButton(Context context) {
        super(context);
    }

    public GuidePagingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GuidePagingButton(final Activity activity, final ILiveTvGuide guide, int start, String label) {
        super(activity);

        us = this;
        startRow = start;

        setBackgroundResource(R.drawable.gray_gradient);
        TextView txtLabel = new TextView(activity);
        txtLabel.setHeight(LiveTvGuideActivity.PAGEBUTTON_HEIGHT);
        txtLabel.setTextColor(Color.BLACK);
        setPadding(100,0,0,0);
        txtLabel.setFocusable(true);
        txtLabel.setText(label);
        addView(txtLabel);

        txtLabel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                guide.displayChannels(startRow, LiveTvGuideActivity.PAGE_SIZE);
            }
        });

        txtLabel.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (hasFocus) {
                    us.setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
                } else {
                    us.setBackgroundResource(R.drawable.gray_gradient);
                }

            }
        });
    }
}
