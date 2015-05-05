package tv.emby.embyatv.livetv;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.results.ChannelInfoDtoResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/5/2015.
 */
public class ChannelListAdapter extends BaseAdapter {

    final int IMAGE_WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 50);
    final int IMAGE_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 30);
    final int HEADER_WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 160);

    INotifyChannelsLoaded parent;
    Activity activity;
    LayoutInflater inflater;
    List<ChannelInfoDto> channels = new ArrayList<>();

    LinearLayout view;

    LiveTvChannelQuery query;

    public ChannelListAdapter(Activity activity, INotifyChannelsLoaded parent, LinearLayout view, LiveTvChannelQuery query) {
        this.activity = activity;
        this.parent = parent;
        this.query = query;
        this.view = view;
    }

    public void Retrieve() {
        channels.clear();
        TvApp.getApplication().getApiClient().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(ChannelInfoDtoResult response) {
                if (response.getTotalRecordCount() > 0) {
                    List<String> ids = new ArrayList<>();
                    int i = 0;
                    for (ChannelInfoDto channel : response.getItems()) {
                        ids.add(channel.getId());
                        channels.add(channel);
                        view.addView(getView(i++, null, view));
                    }
                    String[] idArray = new String[ids.size()];
                    parent.notifyChannelsLoaded(0, ids.toArray(idArray));
                }
            }
        });
    }

    @Override
    public int getCount() {
        return channels.size();
    }

    @Override
    public Object getItem(int position) {
        return channels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.channel_header, null);

        convertView.setLayoutParams(new AbsListView.LayoutParams(HEADER_WIDTH, LiveTvGuideActivity.ROW_HEIGHT));
        ChannelInfoDto item = channels.get(position);
        ((TextView) convertView.findViewById(R.id.channelName)).setText(item.getName());
        ((TextView) convertView.findViewById(R.id.channelNumber)).setText(item.getNumber());
        ImageView image = (ImageView) convertView.findViewById(R.id.channelImage);
        if (item.getHasPrimaryImage()) {
            Picasso.with(activity).load(Utils.getPrimaryImageUrl(item, TvApp.getApplication().getApiClient())).resize(IMAGE_WIDTH, IMAGE_HEIGHT).centerInside().into(image);
        } else {
            image.setImageURI(Uri.EMPTY);
        }

        return convertView;
    }
}
