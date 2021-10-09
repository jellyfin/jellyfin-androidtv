package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.util.Utils;

public class PlaybackSpeedPopup {
    private PopupWindow mPopup;
    private View mAnchor;
    private FloatSpinner mSpeedSpinner;

    public PlaybackSpeedPopup(Context context, View anchor, ValueChangedListener<Float> listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.playback_speed_popup, null);

        int width = Utils.convertDpToPixel(context, 240);
        int height = Utils.convertDpToPixel(context, 130);

        mPopup = new PopupWindow(layout, width, height);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss

        mAnchor = anchor;

        mSpeedSpinner = layout.findViewById(R.id.floatSpinner);
        mSpeedSpinner.setOnChangeListener(listener);
    }

    @Nullable
    public PopupWindow getPopupWindow() {
        return mPopup;
    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void show(float value) {
        mSpeedSpinner.setValue(value);
        mPopup.showAsDropDown(mAnchor, 0, 0, Gravity.END);
    }

    public void dismiss() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }
}
