package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;

public class NumberSpinner extends FrameLayout {
    long mValue = 0;
    long mIncrement = 10;
    TextView mTextValue;
    ValueChangedListener<Long> mValueChangedListener;


    public NumberSpinner(Context context, ValueChangedListener<Long> listener) {
        super(context);
        mValueChangedListener = listener;
        init(context);
    }

    public NumberSpinner(Context context) {
        super(context);
        init(context);
    }

    public NumberSpinner(Context context, AttributeSet attrs) {
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

    public void setOnChangeListener(ValueChangedListener<Long> listener) {
        mValueChangedListener = listener;
    }

    public void setValue(long value) {
        mValue = value;
        mTextValue.setText(Long.toString(mValue));
        if (mValueChangedListener != null) {
            mValueChangedListener.onValueChanged(value);
        }
    }

    public long getValue() {
        return mValue;
    }

    public void setIncrement(long value) {
        mIncrement = value;
    }
}
