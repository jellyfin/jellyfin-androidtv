package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.data.model.GotFocusEvent;
import org.jellyfin.androidtv.databinding.TextUnderButtonBinding;
import org.jellyfin.androidtv.util.Utils;

public class TextUnderButton extends RelativeLayout {
    private TextView mLabel;
    private ImageButton mButton;

    private GotFocusEvent mGotFocusListener;

    public TextUnderButton(Context context, int imageResource, int size, String label, final OnClickListener clicked) {
        this(context, imageResource, size, null, label, clicked);
    }

    public TextUnderButton(Context context, int imageResource, int size, Integer padding, String label, final OnClickListener clicked) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        TextUnderButtonBinding binding = TextUnderButtonBinding.inflate(inflater, this, true);

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
        mButton.setOnClickListener(clicked);
        mButton.setOnFocusChangeListener(focusChange);
        if (padding != null) {
            int amt = Utils.convertDpToPixel(context, padding);
            mButton.setPadding(amt, amt, amt, amt);
        }

    }

    public void setText(String text) { mLabel.setText(text); }

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

    @Override
    public void setEnabled(boolean enabled) {
        mButton.setFocusable(enabled);
        mButton.setFocusableInTouchMode(enabled);
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }
}
