package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import tv.emby.embyatv.R;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/4/2015.
 */
public class GuideChannelHeader extends RelativeLayout {

    private TextView mChannelName;
    private TextView mChannelNumber;
    private ImageView mChannelImage;

    public GuideChannelHeader(Context context) {
        super(context);
        initComponent(context);
    }

    public GuideChannelHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initComponent(context);
    }

    public GuideChannelHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initComponent(context);
    }

    private void initComponent(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.channel_header, this, false);
        this.addView(v);
        this.setFocusable(false);
        mChannelName = (TextView) findViewById(R.id.channelName);
        mChannelNumber = (TextView) findViewById(R.id.channelNumber);
        mChannelImage = (ImageView) findViewById(R.id.channelImage);
    }

    public void setChannelName(String name) {
        mChannelName.setText(name);
    }
    public void setChannelNumber(String number) { mChannelNumber.setText(number); }
    public void setChannelImage(String url) {
        mChannelImage.setImageURI(Uri.parse(url));
    }


    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            setBackgroundColor(getResources().getColor(R.color.lb_default_brand_color));
        } else {
            setBackgroundColor(0);
        }
    }
}
