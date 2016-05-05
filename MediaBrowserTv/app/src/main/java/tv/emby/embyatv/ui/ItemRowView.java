package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 11/21/2015.
 */
public class ItemRowView extends FrameLayout {
    Context mContext;
    RelativeLayout mWholeRow;
    TextView mIndexNo;
    TextView mItemName;
    TextView mExtraName;
    TextView mRunTime;
    TextView mDivider;
    TextView mWatchedMark;
    Drawable normalBackground;

    int ourIndex;
    String formattedTime;

    BaseItemDto mBaseItem;

    RowSelectedListener rowSelectedListener;
    RowClickedListener rowClickedListener;
    ItemRowView us;

    public ItemRowView(Context context) {
        super(context);
        inflateView(context);
    }

    public ItemRowView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        inflateView(context);
    }

    public ItemRowView(Context context, BaseItemDto song, int ndx, RowSelectedListener rowSelectedListener, final RowClickedListener rowClickedListener) {
        super(context);
        inflateView(context);
        this.rowSelectedListener = rowSelectedListener;
        this.rowClickedListener = rowClickedListener;
        setItem(song, ndx);
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
        inflater.inflate(R.layout.item_row, this);
        mContext = context;
        mWholeRow = (RelativeLayout) findViewById(R.id.wholeRow);
        mIndexNo = (TextView) findViewById(R.id.indexNo);
        mItemName = (TextView) findViewById(R.id.songName);
        mExtraName = (TextView) findViewById(R.id.artistName);
        mRunTime = (TextView) findViewById(R.id.runTime);
        mDivider = (TextView) findViewById(R.id.divider);
        mWatchedMark = (TextView) findViewById(R.id.watchedMark);
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

    public void setItem(BaseItemDto item, int ndx) {
        mBaseItem = item;
        ourIndex = ndx + 1;
        mIndexNo.setText(Integer.toString(ourIndex));
        switch (item.getType()) {
            case "Audio":
                mItemName.setText(item.getName());
                String artist = item.getArtists() != null && item.getArtists().size() > 0 ? item.getArtists().get(0) : !TextUtils.isEmpty(item.getAlbumArtist()) ? item.getAlbumArtist() : null;
                if (!TextUtils.isEmpty(artist)) {
                    mExtraName.setText(artist);
                } else {
                    mExtraName.setVisibility(GONE);
                    mDivider.setVisibility(GONE);
                }
                break;
            default:
                String series = item.getSeriesName() != null ? item.getSeriesName() + " S" + item.getParentIndexNumber() + " E" + item.getIndexNumber() : null;
                if (!TextUtils.isEmpty(series)) {
                    mItemName.setText(series);
                    mExtraName.setText(item.getName());
                } else {
                    mItemName.setText(item.getName());
                    mExtraName.setVisibility(GONE);
                    mDivider.setVisibility(GONE);
                }
                updateWatched();
                break;
        }
        formattedTime = Utils.formatMillis(item.getRunTimeTicks() != null ? item.getRunTimeTicks()/10000 : 0);
        mRunTime.setText(formattedTime);
    }

    public void updateWatched() {
        if (mBaseItem == null) return;
        if ("Video".equals(mBaseItem.getMediaType()) && mBaseItem.getUserData() != null && mBaseItem.getUserData().getPlayed()) {
            mWatchedMark.setText(Html.fromHtml("&#x2713;"));
        } else {
            mWatchedMark.setText("");
        }
    }

    public void updateCurrentTime(long pos) {
        if (pos < 0) {
            mRunTime.setText(formattedTime);
        } else {
            mRunTime.setText(Utils.formatMillis(pos) + " / "+ formattedTime);
        }
    }

    public BaseItemDto getItem() { return mBaseItem; }

    public int getIndex() {return ourIndex-1;}

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
        return setPlaying(getItem().getId().equals(id));
    }

    public void setRowSelectedListener(RowSelectedListener listener) {
        rowSelectedListener = listener;
    }

    public static class RowSelectedListener {
        public void onRowSelected(ItemRowView row) {};
    }

    public static class RowClickedListener {
        public void onRowClicked(ItemRowView row) {};
    }

}
