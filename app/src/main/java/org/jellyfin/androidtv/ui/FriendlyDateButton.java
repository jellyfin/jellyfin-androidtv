package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.TimeUtils;

import java.time.LocalDateTime;

public class FriendlyDateButton extends FrameLayout {
    private LocalDateTime dateVal;

    public FriendlyDateButton(Context context, LocalDateTime thisDate, OnClickListener listener) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.friendly_date_button, this, false);
        this.addView(v);
        setFocusable(true);
        setOnClickListener(listener);

        dateVal = thisDate;

        ((TextView)v.findViewById(R.id.friendlyName)).setText(TimeUtils.getFriendlyDate(context, thisDate, true));
        ((TextView)v.findViewById(R.id.date)).setText(DateTimeExtensionsKt.getDateFormatter(context).format(thisDate));

    }

    public LocalDateTime getDate() { return dateVal; }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            setBackgroundColor(getResources().getColor(androidx.leanback.R.color.lb_default_brand_color));
        } else {
            setBackgroundColor(0);
        }
    }
}
