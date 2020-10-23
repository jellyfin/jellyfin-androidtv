package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.model.GotFocusEvent;
import org.jellyfin.androidtv.util.Utils;

public class TextUnderButton extends RelativeLayout {

    public static int STATE_PRIMARY = 0;
    public static int STATE_SECONDARY = 1;

    private TextView mLabel;
    private android.widget.ImageButton mButton;
    private int mPrimaryImage;
    private int mSecondaryImage;
    private int mState;

    private GotFocusEvent mGotFocusListener;

    public TextUnderButton(Context context, int imageResource, int size, final OnClickListener clicked) {
        this(context, imageResource, size, null, null, clicked);
    }

    public TextUnderButton(Context context, int imageResource, int size, String label, final OnClickListener clicked) {
        this(context, imageResource, size, null, label, clicked);
    }

    public TextUnderButton(Context context, int imageResource, int size, Integer padding, String label, final OnClickListener clicked) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.text_under_button, this, false);
        this.addView(v);

        mLabel = (TextView) v.findViewById(R.id.label);
        if (label == null) {
            mLabel.setVisibility(GONE);
        } else {
            mLabel.setText(label);
        }

        mButton = (android.widget.ImageButton) v.findViewById(R.id.imageButton);

        if (label != null) mButton.setContentDescription(label);

        mButton.setImageResource(imageResource);
        mButton.setMaxHeight(size);
        mButton.setOnClickListener(clicked);
        mButton.setOnFocusChangeListener(focusChange);
        if (padding != null) {
            int amt = Utils.convertDpToPixel(context, padding);
            mButton.setPadding(amt, amt, amt, amt);
        }

    }

    public void setText(String text) { mLabel.setText(text); }

    public void setState(int state) {
        mState = state;
        if (mSecondaryImage > 0) mButton.setImageResource(mState == STATE_SECONDARY ? mSecondaryImage : mPrimaryImage);
    }

    public void toggleState() {
        setState(mState == STATE_PRIMARY ? STATE_SECONDARY : STATE_PRIMARY);
    }

    protected OnFocusChangeListener focusChange = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus && mGotFocusListener != null) mGotFocusListener.gotFocus(v);

        }
    };

    public void setGotFocusListener(GotFocusEvent event) { mGotFocusListener = event; }

    public void setImageResource(int resource) {
        mButton.setImageResource(resource);
    }

    public void setPrimaryImage(int mPrimaryImage) {
        this.mPrimaryImage = mPrimaryImage;
    }

    public void setSecondaryImage(int mSecondaryImage) {
        this.mSecondaryImage = mSecondaryImage;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mButton.setFocusable(enabled);
        mButton.setFocusableInTouchMode(enabled);
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }
}
