package org.jellyfin.androidtv.customer.action;

import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.util.Function;

public class TextSeekBarListener implements SeekBar.OnSeekBarChangeListener {
    private final TextView value;
    private final Function<Integer, String> valueGetter;

    public TextSeekBarListener(TextView value) {
        this(value, null);
    }

    public TextSeekBarListener(TextView value, Function<Integer, String> valueGetter) {
        this.value = value;
        this.valueGetter = valueGetter;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String v = valueGetter != null ? valueGetter.apply(progress) : String.valueOf(progress);
        value.setText(v);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
