package org.jellyfin.androidtv.ui.presentation;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.MediaStreamType;

import java.text.NumberFormat;

public class MyInfoCardView extends FrameLayout {
    private LinearLayout mInfoLayout;
    private TextView mTitle;
    private Context mContext;

    public MyInfoCardView(Context context) {
        super(context);
        mContext = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.media_info_card, this);
        mInfoLayout = (LinearLayout) v.findViewById(R.id.infoLayout1);
        mTitle = (TextView) v.findViewById(R.id.infoCardTitle);

    }

    public void setItem(MediaStream ms) {
        mTitle.setText(ms.getType().toString());
        mInfoLayout.removeAllViews();

        switch (ms.getType()) {

            case Audio:
            case Video:
            case Subtitle:
                if (ms.getType() != MediaStreamType.Video && ms.getLanguage() != null) addRow("Language: ", Utils.firstToUpper(ms.getLanguage()));
                if (ms.getCodec() != null) addRow("Codec: ", ms.getCodec().toUpperCase());
                if (ms.getProfile() != null) addRow("Profile: ",ms.getProfile());
                if (ms.getLevel() != null && ms.getLevel() > 0) addRow("Level: ", ms.getLevel().toString());
                if (ms.getChannelLayout() != null) addRow("Layout: ", ms.getChannelLayout());
                if (ms.getType() == MediaStreamType.Video) {
                    if (ms.getWidth() != null && ms.getHeight() != null) addRow("Resolution: ",ms.getWidth()+"x"+ms.getHeight());
                    if (ms.getIsAnamorphic() != null && ms.getIsAnamorphic()) addRow("Anamorphic", "");
                    if (ms.getIsInterlaced()) addRow("Interlaced","");
                    if (ms.getAspectRatio() != null) addRow("Aspect: ", ms.getAspectRatio());
                    if (ms.getRealFrameRate() != null) addRow("Framerate: ", ms.getRealFrameRate().toString());
                }
                if (ms.getBitRate() != null) addRow("Bitrate: ", (NumberFormat.getInstance().format(ms.getBitRate()/1024))+" kbps");
                if (ms.getIsDefault()) addRow("Default","");
                if (ms.getIsForced()) addRow("Forced","");
                if (ms.getIsExternal()) addRow("External","");


                break;
            case EmbeddedImage:
                break;
        }

    }

    private void addRow(String label, String value) {
        LinearLayout row = new LinearLayout(mContext);
        TextView labelView = new TextView(mContext);
        labelView.setText(label);
        labelView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        labelView.setTextSize(12);
        row.addView(labelView);
        TextView valueView = new TextView(mContext);
        valueView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        valueView.setText(value);
        valueView.setTextSize(12);
        row.addView(valueView);
        mInfoLayout.addView(row);
    }
}
