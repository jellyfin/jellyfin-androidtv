package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 7/22/2015.
 */
public class ClockUserView extends RelativeLayout {
    public ClockUserView(Context context) {
        super(context);
        init(context);
    }

    public ClockUserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.clock_user_bug, null, false);
        this.addView(v);
        if (!isInEditMode()) {
            Typeface font = TvApp.getApplication().getDefaultFont();
            ((TextClock)v.findViewById(R.id.clock)).setTypeface(font);
            TextView username = ((TextView) v.findViewById(R.id.userName));
            username.setTypeface(font);
            username.setText(TvApp.getApplication().getCurrentUser().getName());
            ImageView userImage = (ImageView) v.findViewById(R.id.userImage);
            if (TvApp.getApplication().getCurrentUser().getHasPrimaryImage()) {
                Picasso.with(context).load(Utils.getPrimaryImageUrl(TvApp.getApplication().getCurrentUser(), TvApp.getApplication().getApiClient())).error(R.drawable.user).resize(30,30).centerInside().into(userImage);
            } else {
                userImage.setImageResource(R.drawable.user);
            }

        }
    }
}
