package tv.emby.embyatv.ui;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import tv.emby.embyatv.R;

/**
 * Created by Eric on 2/20/2015.
 */
public class ImageButton extends ImageView {

    public ImageButton(Context context, int imageResource, int size, final String helpText, final TextView helpView, final OnClickListener clicked) {
        super(context, null, R.style.spaced_buttons);
        setImageResource(imageResource);
        setMaxHeight(size);
        setAdjustViewBounds(true);
        setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        setFocusable(true);
        setOnClickListener(clicked);
        setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    helpView.setText(helpText);
                    v.setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
                } else {
                    v.setBackgroundColor(0);
                }
            }
        });

    }
}
