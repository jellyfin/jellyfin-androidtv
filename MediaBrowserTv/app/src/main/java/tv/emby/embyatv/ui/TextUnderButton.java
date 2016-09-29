package tv.emby.embyatv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.model.GotFocusEvent;

/**
 * Created by Eric on 2/20/2015.
 */
public class TextUnderButton extends RelativeLayout {

    public static int STATE_PRIMARY = 0;
    public static int STATE_SECONDARY = 1;

    private TextView mLabel;
    private android.widget.ImageButton mButton;
    private int mPrimaryImage;
    private int mSecondaryImage;
    private int mState;

    public TextUnderButton(Context context, int imageResource, int size, final OnClickListener clicked) {
        this(context, imageResource, size, null, clicked);
    }

    public TextUnderButton(Context context, int imageResource, int size, String label, final OnClickListener clicked) {
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

        mButton.setImageResource(imageResource);
        mButton.setMaxHeight(size);
        mButton.setOnClickListener(clicked);

    }

    public void setText(String text) { mLabel.setText(text); }

    public void setState(int state) {
        mState = state;
        if (mSecondaryImage > 0) mButton.setImageResource(mState == STATE_SECONDARY ? mSecondaryImage : mPrimaryImage);
    }

    public void toggleState() {
        setState(mState == STATE_PRIMARY ? STATE_SECONDARY : STATE_PRIMARY);
    }

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
}
