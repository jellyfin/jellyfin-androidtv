package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.util.Utils;

public class AudioDelayPopup {
    private PopupWindow mPopup;
    private View mAnchor;
    private NumberSpinner mDelaySpinner;

    public AudioDelayPopup(Context context, View anchor, ValueChangedListener<Long> listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.audio_delay_popup, null);

        int width = Utils.convertDpToPixel(context, 240);
        int height = Utils.convertDpToPixel(context, 130);

        mPopup = new PopupWindow(layout, width, height);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss

        mAnchor = anchor;

        mDelaySpinner = layout.findViewById(R.id.numberSpinner);
        mDelaySpinner.setOnChangeListener(listener);
    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void show(long value) {
        mDelaySpinner.setValue(value);
        mPopup.showAtLocation(mAnchor, Gravity.CENTER_VERTICAL, mAnchor.getRight() - 60, mAnchor.getTop());
    }

    public void dismiss() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }
}
