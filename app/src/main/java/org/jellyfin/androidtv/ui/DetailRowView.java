package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import org.jellyfin.androidtv.R;

public class DetailRowView extends FrameLayout {
    public DetailRowView(Context context) {
        super(context);
        inflateView(context);
    }

    public DetailRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView(context);
    }

    private void inflateView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.new_details_overview_row, this);
    }
}
