package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Created by Eric on 2/21/2015.
 */
public class GenreButton extends TextView {
    public GenreButton(Context context, Typeface font, int size, String text, String itemType) {
        super(context);
        setTypeface(font);
        setTextSize(size);
        setText(text);
        //setFocusable(true);
//        setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //Navigate to genre
//            }
//        });
//
//        setOnFocusChangeListener(new OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (hasFocus) {
//                    v.setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
//                } else {
//                    v.setBackgroundColor(0);
//                }
//
//            }
//        });

    }
}
