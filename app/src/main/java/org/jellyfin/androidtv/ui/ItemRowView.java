package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.databinding.ItemRowBinding;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.MediaType;

import java.util.UUID;

public class ItemRowView extends FrameLayout {
    Context mContext;
    RelativeLayout mWholeRow;
    TextView mIndexNo;
    TextView mItemName;
    TextView mExtraName;
    TextView mRunTime;
    TextView mWatchedMark;
    Drawable normalBackground;

    int ourIndex;
    String formattedTime;

    BaseItemDto mBaseItem;

    RowSelectedListener rowSelectedListener;
    RowClickedListener rowClickedListener;

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
        final ItemRowView itemRowView = this;
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rowClickedListener != null) rowClickedListener.onRowClicked(itemRowView);
            }
        });
    }

    private void inflateView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ItemRowBinding binding = ItemRowBinding.inflate(inflater, this, true);
        mContext = context;
        mWholeRow = binding.wholeRow;
        mIndexNo = binding.indexNo;
        mItemName = binding.songName;
        mExtraName = binding.artistName;
        mRunTime = binding.runTime;
        mWatchedMark = binding.watchedMark;
        normalBackground = mWholeRow.getBackground();
        setFocusable(true);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            mWholeRow.setBackgroundResource(R.drawable.jellyfin_button);
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
            case AUDIO:
                mItemName.setText(item.getName());
                String artist = item.getArtists() != null && item.getArtists().size() > 0 ? item.getArtists().get(0) : !TextUtils.isEmpty(item.getAlbumArtist()) ? item.getAlbumArtist() : null;
                if (!TextUtils.isEmpty(artist)) {
                    mExtraName.setText(artist);
                } else {
                    mExtraName.setVisibility(GONE);
                }
                break;
            default:
                String series = item.getSeriesName() != null ? BaseItemExtensionsKt.getFullName(item, mContext) : null;
                if (!TextUtils.isEmpty(series)) {
                    mItemName.setText(series);
                    mExtraName.setText(item.getName());
                } else {
                    mItemName.setText(item.getName());
                    mExtraName.setVisibility(GONE);
                }
                updateWatched();
                break;
        }
        formattedTime = TimeUtils.formatMillis(item.getRunTimeTicks() != null ? item.getRunTimeTicks()/10000 : 0);
        mRunTime.setText(formattedTime);
    }

    public void updateWatched() {
        if (mBaseItem == null) return;
        if (MediaType.VIDEO.equals(mBaseItem.getMediaType()) && mBaseItem.getUserData() != null && mBaseItem.getUserData().getPlayed()) {
            mWatchedMark.setText("✓");
        } else {
            mWatchedMark.setText("");
        }
    }

    public void updateCurrentTime(long pos) {
        if (pos < 0) {
            mRunTime.setText(formattedTime);
        } else {
            mRunTime.setText(TimeUtils.formatMillis(pos) + " / "+ formattedTime);
        }
    }

    public BaseItemDto getItem() { return mBaseItem; }

    public int getIndex() {return ourIndex-1;}

    public boolean setPlaying(boolean playing) {
        if (playing) {
            // TODO use decent animation for equalizer icon
            mIndexNo.setBackgroundResource(R.drawable.ic_play);
            mIndexNo.setText("");
        } else {
            mIndexNo.setBackgroundResource(R.drawable.blank10x10);
            mIndexNo.setText(Integer.toString(ourIndex));
        }
        return playing;
    }

    public boolean setPlaying(UUID id) {
        return setPlaying(getItem().getId().equals(id));
    }

    public void setRowSelectedListener(RowSelectedListener listener) {
        rowSelectedListener = listener;
    }

    public interface RowSelectedListener {
        public void onRowSelected(ItemRowView row);
    }

    public interface RowClickedListener {
        public void onRowClicked(ItemRowView row);
    }
}
