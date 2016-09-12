package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import tv.emby.embyatv.R;
import tv.emby.embyatv.util.Utils;

/**
 * Created by spam on 9/12/2016.
 */
public class FriendlyDateButton extends FrameLayout {
    private long dateVal;

    public FriendlyDateButton(Context context, long thisDate, OnClickListener listener) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.friendly_date_button, this, false);
        this.addView(v);
        setFocusable(true);
        setOnClickListener(listener);

        dateVal = thisDate;
        Date date = new Date(thisDate);

        ((TextView)v.findViewById(R.id.friendlyName)).setText(Utils.getFriendlyDate(date, true));
        ((TextView)v.findViewById(R.id.date)).setText(android.text.format.DateFormat.getDateFormat(context).format(date));

    }

    public long getDate() { return dateVal; }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
        } else {
            setBackgroundColor(0);
        }

        //TvApp.getApplication().getLogger().Debug("Focus on "+mProgram.getName()+ " was " +(gainFocus ? "gained" : "lost"));
    }


}
