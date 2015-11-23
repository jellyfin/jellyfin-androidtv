package tv.emby.embyatv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.base.BaseActivity;

/**
 * Created by Eric on 11/21/2015.
 */
public class SongListView extends FrameLayout {
    Context mContext;
    LinearLayout mList;
    SongRowView.RowSelectedListener mRowSelectedListener;

    public SongListView(Context context) {
        super(context);
        inflateView(context);
    }

    public SongListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        inflateView(context);
    }

    private void inflateView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.song_list, this);
        mContext = context;
        mList = (LinearLayout) findViewById(R.id.songList);

    }

    public void setRowSelectedListener(SongRowView.RowSelectedListener listener) { mRowSelectedListener = listener; }

    public void addSongs(BaseItemDto[] songs) {
        int i = 0;
        for (BaseItemDto song : songs) {
            addSong(song, i++);
        }

    }

    public void addSong(BaseItemDto song, int ndx) {
        mList.addView(new SongRowView(mContext, song, ndx, mRowSelectedListener));
    }
}
