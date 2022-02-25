package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import org.jellyfin.androidtv.databinding.NewDetailsOverviewRowBinding;

public class DetailRowView extends FrameLayout {
    private NewDetailsOverviewRowBinding binding;

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
        binding = NewDetailsOverviewRowBinding.inflate(inflater, this, true);
    }

    public NewDetailsOverviewRowBinding getBinding() {
        return binding;
    }
}
