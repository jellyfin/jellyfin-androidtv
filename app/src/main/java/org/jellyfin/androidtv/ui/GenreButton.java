package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Typeface;

import org.jellyfin.apiclient.model.dto.BaseItemType;

import androidx.appcompat.widget.AppCompatTextView;

public class GenreButton extends AppCompatTextView {
    public GenreButton(Context context, int size, String text, BaseItemType itemType) {
        super(context);
        setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
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
