package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;

import static org.koin.java.KoinJavaComponent.get;

public class GuideChannelHeader extends RelativeLayout {
    final int IMAGE_WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 50);
    final int IMAGE_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 30);
    final int HEADER_WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 160);

    private ImageView mChannelImage;
    private ChannelInfoDto mChannel;
    private Context mContext;
    private CustomPlaybackOverlayFragment mActivity;

    public GuideChannelHeader(Context context, CustomPlaybackOverlayFragment fragment, ChannelInfoDto channel, boolean focusable) {
        super(context);
        initComponent(context, fragment, channel, focusable);
    }

    private void initComponent(Context context, CustomPlaybackOverlayFragment fragment, ChannelInfoDto channel, boolean focusable) {
        mContext = context;
        mChannel = channel;
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.channel_header, this, false);
        v.setLayoutParams(new AbsListView.LayoutParams(HEADER_WIDTH, LiveTvGuideActivity.ROW_HEIGHT));
        this.addView(v);
        this.setFocusable(false);
        ((TextView) findViewById(R.id.channelName)).setText(channel.getName());
        ((TextView) findViewById(R.id.channelNumber)).setText(channel.getNumber());
        mChannelImage = (ImageView) findViewById(R.id.channelImage);

        if (fragment != null) {
            mActivity = fragment;
            this.setFocusable(focusable);
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.switchChannelKeepGuide(channel.getId());
                }
            });
        }
    }

    public void loadImage() {
        Glide.with(mContext)
                .load(ImageUtils.getPrimaryImageUrl(mChannel, get(ApiClient.class)))
                .override(IMAGE_WIDTH, IMAGE_HEIGHT)
                .centerInside()
                .into(mChannelImage);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            setBackgroundColor(Utils.getThemeColor(getContext(), android.R.attr.colorAccent));

            mActivity.setSelectedProgram(this);
        } else {
            setBackground(getResources().getDrawable(R.drawable.light_border));
        }
//        TvApp.getApplication().getLogger().Debug("Focus on " + mProgram.getName() + " was " + (gainFocus ? "gained" : "lost"));
    }

}
