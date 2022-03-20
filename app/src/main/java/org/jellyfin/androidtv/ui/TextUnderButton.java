package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import org.jellyfin.androidtv.data.model.GotFocusEvent;
import org.jellyfin.androidtv.databinding.TextUnderButtonBinding;
import org.jellyfin.androidtv.util.Utils;

public class TextUnderButton extends RelativeLayout {
    private TextView mLabel;
    private ImageView mButton;

    private GotFocusEvent mGotFocusListener;

    public TextUnderButton(Context context, @DrawableRes int imageResource, int size, String label, final OnClickListener clicked) {
        this(context, imageResource, size, null, label, clicked);
    }

    public TextUnderButton(Context context, @DrawableRes int imageResource, int size, Integer padding, String label, final OnClickListener clicked) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        TextUnderButtonBinding binding = TextUnderButtonBinding.inflate(inflater, this, true);

        setFocusable(true);
        setClickable(true);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setOnClickListener(clicked);
        setOnFocusChangeListener(focusChange);

        mLabel = binding.label;
        if (label == null) {
            mLabel.setVisibility(GONE);
        } else {
            mLabel.setText(label);
        }

        mButton = binding.imageButton;

        if (label != null) mButton.setContentDescription(label);

        mButton.setImageResource(imageResource);
        mButton.setMaxHeight(size);
        if (padding != null) {
            int amt = Utils.convertDpToPixel(context, padding);
            mButton.setPadding(amt, amt, amt, amt);
        }
    }

    public void setText(String text) {
        mLabel.setText(text);
    }

    protected OnFocusChangeListener focusChange = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus && mGotFocusListener != null) mGotFocusListener.gotFocus(v);
        }
    };

    public void setGotFocusListener(GotFocusEvent event) {
        mGotFocusListener = event;
    }

    @Override
    public void setEnabled(boolean enabled) {
        setFocusable(enabled);
        setFocusableInTouchMode(enabled);
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }
}
