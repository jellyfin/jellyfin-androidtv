package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by Eric on 4/30/2015.
 */
public class TextButton extends Button {
    public TextButton(Context context) {
        super(context);
        setOnFocusChangeListener(focusChangeListener);
    }

    public TextButton(Context context, String text, int size, OnClickListener listener) {
        super(context);
        setOnFocusChangeListener(focusChangeListener);
        setOnClickListener(listener);
        setText(text);
        setBackgroundColor(0);
        if (size == 0) {
            setTextSize(16);
        } else {
            setTextSize(size);
            int trueSize = Utils.convertDpToPixel(context, size);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(trueSize*2 +15, trueSize + 40);

            setLayoutParams(lp);
        }
    }

    private OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                v.setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
            } else {
                v.setBackgroundColor(0);
            }
        }
    };


}
