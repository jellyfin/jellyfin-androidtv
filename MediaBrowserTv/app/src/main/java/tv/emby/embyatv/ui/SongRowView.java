package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 11/21/2015.
 */
public class SongRowView extends FrameLayout {
    Context mContext;
    RelativeLayout mWholeRow;
    TextView mIndexNo;
    TextView mSongName;
    TextView mArtistName;
    TextView mRunTime;
    TextView mDivider;
    Drawable normalBackground;

    int ourIndex;
    String formattedTime;

    BaseItemDto mBaseItem;

    RowSelectedListener rowSelectedListener;
    RowClickedListener rowClickedListener;
    SongRowView us;

    public SongRowView(Context context) {
        super(context);
        inflateView(context);
    }

    public SongRowView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        inflateView(context);
    }

    public SongRowView(Context context, BaseItemDto song, int ndx, RowSelectedListener rowSelectedListener, final RowClickedListener rowClickedListener) {
        super(context);
        inflateView(context);
        this.rowSelectedListener = rowSelectedListener;
        this.rowClickedListener = rowClickedListener;
        setSong(song, ndx);
        us = this;
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playSoundEffect(SoundEffectConstants.CLICK);
                if (rowClickedListener != null) rowClickedListener.onRowClicked(us);
            }
        });
    }

    private void inflateView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.song_row, this);
        mContext = context;
        mWholeRow = (RelativeLayout) findViewById(R.id.wholeRow);
        mIndexNo = (TextView) findViewById(R.id.indexNo);
        mSongName = (TextView) findViewById(R.id.songName);
        mArtistName = (TextView) findViewById(R.id.artistName);
        mRunTime = (TextView) findViewById(R.id.runTime);
        mDivider = (TextView) findViewById(R.id.divider);
        normalBackground = mWholeRow.getBackground();
        setFocusable(true);

    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            mWholeRow.setBackgroundResource(R.color.lb_default_brand_color);
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
            if (rowSelectedListener != null) rowSelectedListener.onRowSelected(this);
        } else {
            mWholeRow.setBackground(normalBackground);
        }
    }

    public void setSong(BaseItemDto song, int ndx) {
        mBaseItem = song;
        ourIndex = ndx + 1;
        mIndexNo.setText(Integer.toString(ourIndex));
        mSongName.setText(song.getName());
        String artist = song.getArtists().size() > 0 ? song.getArtists().get(0) : !TextUtils.isEmpty(song.getAlbumArtist()) ? song.getAlbumArtist() : null;
        if (!TextUtils.isEmpty(artist)) {
            mArtistName.setText(artist);
        } else {
            mArtistName.setVisibility(GONE);
            mDivider.setVisibility(GONE);
        }
        formattedTime = Utils.formatMillis(song.getRunTimeTicks()/10000);
        mRunTime.setText(formattedTime);
    }

    public void updateCurrentTime(long pos) {
        if (pos < 0) {
            mRunTime.setText(formattedTime);
        } else {
            mRunTime.setText(Utils.formatMillis(pos) + " / "+ formattedTime);
        }
    }

    public BaseItemDto getSong() { return mBaseItem; }

    public boolean setPlaying(boolean playing) {
        if (playing) {
            mIndexNo.setBackgroundResource(R.drawable.eq_animation);
            mIndexNo.setText("");
            ((AnimationDrawable)mIndexNo.getBackground()).start();
        } else {
            mIndexNo.setBackgroundResource(R.drawable.blank10x10);
            mIndexNo.setText(Integer.toString(ourIndex));
        }
        return playing;
    }

    public boolean setPlaying(String id) {
        return setPlaying(getSong().getId().equals(id));
    }

    public void setRowSelectedListener(RowSelectedListener listener) {
        rowSelectedListener = listener;
    }

    public static class RowSelectedListener {
        public void onRowSelected(SongRowView row) {};
    }

    public static class RowClickedListener {
        public void onRowClicked(SongRowView row) {};
    }

}
