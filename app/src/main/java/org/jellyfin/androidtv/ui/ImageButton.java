package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.model.GotFocusEvent;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by Eric on 2/20/2015.
 */
public class ImageButton extends AppCompatImageView {

    public static int STATE_PRIMARY = 0;
    public static int STATE_SECONDARY = 1;

    private TextView mHelpView;
    private String mHelpText = "";
    private int mPrimaryImage;
    private int mSecondaryImage;
    private int mState;

    private GotFocusEvent mGotFocusListener;

    public ImageButton(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setOnFocusChangeListener(focusChangeListener);
    }

    public ImageButton(Context context, int imageResource, int size, final OnClickListener clicked) {
        this(context, imageResource, size, "", null, clicked);
    }

    public ImageButton(Context context, int imageResource, int size, String helpText, TextView helpView, final OnClickListener clicked) {
        super(context, null, R.style.spaced_buttons);
        setImageResource(imageResource);
        setMaxHeight(size);
        setAdjustViewBounds(true);
        setAlpha(.8f);
        setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        setFocusable(true);
        setOnClickListener(clicked);
        mHelpView = helpView;
        mHelpText = helpText;
        setOnFocusChangeListener(focusChangeListener);
    }

    public void setHelpView(TextView view) {
        mHelpView = view;
    }

    public void setHelpText(String text) {
        mHelpText = text;
    }

    public void setState(int state) {
        mState = state;
        if (mSecondaryImage > 0) setImageResource(mState == STATE_SECONDARY ? mSecondaryImage : mPrimaryImage);
    }

    private OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                if (mHelpView != null) mHelpView.setText(mHelpText);
                v.setBackgroundResource(R.drawable.btn_focus);
                if (mGotFocusListener != null) mGotFocusListener.gotFocus(v);
            } else {
                if (mHelpView != null) mHelpView.setText("");
                v.setBackgroundColor(0);
            }
        }
    };

    public void setGotFocusListener(GotFocusEvent event) {
        mGotFocusListener = event;
    }

    public void setPrimaryImage(int mPrimaryImage) {
        this.mPrimaryImage = mPrimaryImage;
    }

    public void setSecondaryImage(int mSecondaryImage) {
        this.mSecondaryImage = mSecondaryImage;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : .4f);
        setFocusable(enabled);
        setFocusableInTouchMode(enabled);
    }
}
