package tv.emby.embyatv.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import tv.emby.embyatv.R;

/**
 * Created by Eric on 4/30/2015.
 */
public class TextButton extends Button {
    public TextButton(Context context) {
        super(context);
        setOnFocusChangeListener(focusChangeListener);
    }

    private OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                v.setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
            } else {
                v.setBackgroundColor(0);
            }
        }
    };


}
