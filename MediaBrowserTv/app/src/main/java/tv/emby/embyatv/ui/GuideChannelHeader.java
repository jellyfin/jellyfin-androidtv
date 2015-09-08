package tv.emby.embyatv.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import mediabrowser.model.livetv.ChannelInfoDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.livetv.LiveTvGuideActivity;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/4/2015.
 */
public class GuideChannelHeader extends RelativeLayout {
    final int IMAGE_WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 50);
    final int IMAGE_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 30);
    final int HEADER_WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 160);

    private ImageView mChannelImage;
    private ChannelInfoDto mChannel;
    private Context mActivity;

    public GuideChannelHeader(Context context, ChannelInfoDto channel) {
        super(context);
        initComponent(context, channel);
    }

    private void initComponent(Context context, ChannelInfoDto channel) {
        mActivity = context;
        mChannel = channel;
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.channel_header, this, false);
        v.setLayoutParams(new AbsListView.LayoutParams(HEADER_WIDTH, LiveTvGuideActivity.ROW_HEIGHT));
        this.addView(v);
        this.setFocusable(false);
        ((TextView) findViewById(R.id.channelName)).setText(channel.getName());
        ((TextView) findViewById(R.id.channelNumber)).setText(channel.getNumber());
        mChannelImage = (ImageView) findViewById(R.id.channelImage);
    }

    public void loadImage() {
        Picasso.with(mActivity).load(Utils.getPrimaryImageUrl(mChannel, TvApp.getApplication().getApiClient())).resize(IMAGE_WIDTH, IMAGE_HEIGHT).centerInside().into(mChannelImage);
    }

}
