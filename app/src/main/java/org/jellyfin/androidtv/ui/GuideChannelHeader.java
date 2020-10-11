package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.livetv.ILiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideActivity;
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
    private ImageView mFavImage;
    private ChannelInfoDto mChannel;
    private Context mContext;
    private ILiveTvGuide mTvGuide;

    public GuideChannelHeader(Context context, ILiveTvGuide tvGuide, ChannelInfoDto channel) {
        super(context);
        initComponent(context, tvGuide, channel);
    }

    private void initComponent(Context context, ILiveTvGuide tvGuide, ChannelInfoDto channel) {
        mContext = context;
        mChannel = channel;
        mTvGuide = tvGuide;
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.channel_header, this, false);
        v.setLayoutParams(new AbsListView.LayoutParams(HEADER_WIDTH, LiveTvGuideActivity.ROW_HEIGHT));
        this.addView(v);
        this.setFocusable(true);
        ((TextView) findViewById(R.id.channelName)).setText(channel.getName());
        ((TextView) findViewById(R.id.channelNumber)).setText(channel.getNumber());
        mChannelImage = findViewById(R.id.channelImage);
        mFavImage = findViewById(R.id.favImage);

        if (mChannel.getUserData() != null && mChannel.getUserData().getIsFavorite())
            mFavImage.setVisibility(View.VISIBLE);
    }

    public void loadImage() {
        Glide.with(mContext)
                .load(ImageUtils.getPrimaryImageUrl(mChannel, get(ApiClient.class)))
                .override(IMAGE_WIDTH, IMAGE_HEIGHT)
                .centerInside()
                .into(mChannelImage);
    }

    public ChannelInfoDto getChannel() { return mChannel; }

    public void refreshFavorite() {
        if (mChannel.getUserData() != null && mChannel.getUserData().getIsFavorite())
            mFavImage.setVisibility(View.VISIBLE);
        else
            mFavImage.setVisibility(View.GONE);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            setBackgroundColor(Utils.getThemeColor(getContext(), android.R.attr.colorAccent));

            mTvGuide.setSelectedProgram(this);
        } else {
            setBackground(ContextCompat.getDrawable(getContext(), R.drawable.light_border));
        }
    }

}
