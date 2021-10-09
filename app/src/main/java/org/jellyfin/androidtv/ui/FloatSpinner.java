package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;

public class FloatSpinner extends FrameLayout {
    float mValue = 1;
    float mIncrement = .1f;
    TextView mTextValue;
    ValueChangedListener<Float> mValueChangedListener;


    public FloatSpinner(Context context, ValueChangedListener<Float> listener) {
        super(context);
        mValueChangedListener = listener;
        init(context);
    }

    public FloatSpinner(Context context) {
        super(context);
        init(context);
    }

    public FloatSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.number_spinner, this, true);
        if (!isInEditMode()) {
            mTextValue = (TextView) v.findViewById(R.id.txtValue);
            (v.findViewById(R.id.btnIncrease)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setValue(mValue + mIncrement);
                }
            });
            (v.findViewById(R.id.btnDecrease)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setValue(mValue-mIncrement);
                }
            });
        }
    }

    public void setOnChangeListener(ValueChangedListener<Float> listener) {
        mValueChangedListener = listener;
    }

    public void setValue(float value) {
        mValue = value;
        mTextValue.setText(String.format(getResources().getConfiguration().locale, "%.1f", mValue));
        if (mValueChangedListener != null) {
            mValueChangedListener.onValueChanged(value);
        }
    }

    public float getValue() {
        return mValue;
    }

    public void setIncrement(float value) {
        mIncrement = value;
    }
}
