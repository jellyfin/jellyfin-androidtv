package org.jellyfin.androidtv.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by Eric on 8/23/2015.
 */
public class AudioDelayPopup {

    final int WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 240);
    final int HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 130);

    PopupWindow mPopup;
    View mAnchor;
    NumberSpinner mDelaySpinner;

    public AudioDelayPopup(Context context, View anchor, ValueChangedListener<Long> listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.audio_delay_popup, null);
        mPopup = new PopupWindow(layout, WIDTH, HEIGHT);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss

        mAnchor = anchor;

        mDelaySpinner = (NumberSpinner) layout.findViewById(R.id.numberSpinner);
        mDelaySpinner.setOnChangeListener(listener);
    }
    
    

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void show(long value) {

        mDelaySpinner.setValue(value);
        mPopup.showAtLocation(mAnchor, Gravity.CENTER_VERTICAL, mAnchor.getRight()-60, mAnchor.getTop());

    }

    public void dismiss() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }
}
