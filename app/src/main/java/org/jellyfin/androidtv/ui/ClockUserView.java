package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.apiclient.interaction.ApiClient;

import static org.koin.java.KoinJavaComponent.get;

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
            TextView username = ((TextView) v.findViewById(R.id.userName));
            username.setText(TvApp.getApplication().getCurrentUser().getName());
            ImageView userImage = (ImageView) v.findViewById(R.id.userImage);
            if (TvApp.getApplication().getCurrentUser().getPrimaryImageTag() != null) {
                Glide.with(context)
                        .load(ImageUtils.getPrimaryImageUrl(TvApp.getApplication().getCurrentUser(), get(ApiClient.class)))
                        .error(R.drawable.ic_user)
                        .override(30,30)
                        .centerInside()
                        .into(userImage);
            } else {
                userImage.setImageResource(R.drawable.ic_user);
            }

        }
    }
}
