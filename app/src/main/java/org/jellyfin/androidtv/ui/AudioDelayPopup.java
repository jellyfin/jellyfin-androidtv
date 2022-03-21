package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;

import org.jellyfin.androidtv.databinding.AudioDelayPopupBinding;
import org.jellyfin.androidtv.util.Utils;

public class AudioDelayPopup {
    private PopupWindow mPopup;
    private View mAnchor;
    private NumberSpinnerView mDelaySpinner;

    public AudioDelayPopup(Context context, View anchor, ValueChangedListener<Long> listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        AudioDelayPopupBinding binding = AudioDelayPopupBinding.inflate(inflater, null, false);

        int width = Utils.convertDpToPixel(context, 240);
        int height = Utils.convertDpToPixel(context, 130);

        mPopup = new PopupWindow(binding.getRoot(), width, height);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss

        mAnchor = anchor;

        mDelaySpinner = binding.numberSpinner;
        mDelaySpinner.setValueChangedListener(listener);
    }

    @Nullable
    public PopupWindow getPopupWindow() {
        return mPopup;
    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void show(long value) {
        mDelaySpinner.setValue(value);
        mPopup.showAsDropDown(mAnchor, 0, 0, Gravity.END);
    }
}
