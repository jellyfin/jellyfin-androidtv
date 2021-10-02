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
import org.jellyfin.androidtv.ui.livetv.ILiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.koin.java.KoinJavaComponent;

public class GuideChannelHeader extends RelativeLayout {
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
        int headerWidth = Utils.convertDpToPixel(context, 160);
        v.setLayoutParams(new AbsListView.LayoutParams(headerWidth, LiveTvGuideActivity.ROW_HEIGHT));
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
        int imageWidth = Utils.convertDpToPixel(mContext, 50);
        int imageHeight = Utils.convertDpToPixel(mContext, 30);

        Glide.with(mContext)
                .load(ImageUtils.getPrimaryImageUrl(mChannel, KoinJavaComponent.<ApiClient>get(ApiClient.class)))
                .override(imageWidth, imageHeight)
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
            setBackgroundColor(Utils.getThemeColor(mContext, android.R.attr.colorAccent));

            mTvGuide.setSelectedProgram(this);
        } else {
            setBackground(ContextCompat.getDrawable(mContext, R.drawable.light_border));
        }
    }

}
