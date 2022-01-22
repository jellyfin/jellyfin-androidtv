package org.jellyfin.androidtv.ui.itemdetail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.IntRange;
import androidx.leanback.widget.Row;

import org.jellyfin.androidtv.data.model.InfoItem;
import org.jellyfin.androidtv.ui.TextUnderButton;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import java.util.ArrayList;
import java.util.List;

public class MyDetailsOverviewRow extends Row {
    private BaseItemDto mItem;
    private Drawable mImageDrawable;
    private String mSummary;
    private int mProgress;
    private InfoItem mInfoItem1;
    private InfoItem mInfoItem2;
    private InfoItem mInfoItem3;


    private List<TextUnderButton> mActions = new ArrayList<>();

    public MyDetailsOverviewRow(BaseItemDto item) {
        mItem = item;
    }


    public String getSummary() {
        return mSummary;
    }

    public void setSummary(String mSummary) {
        this.mSummary = mSummary;
    }

    public void setProgress(@IntRange(from = 0, to = 100) int progress) {
        this.mProgress = progress;
    }

    public int getProgress() {
        return mProgress;
    }

    public List<TextUnderButton> getActions() { return mActions; }
    public int getVisibleActions() {
        int actions = 0;
        for (int i = 0; i < mActions.size(); i++) {
            if (mActions.get(i).isVisible()) actions++;
        }
        return actions;
    }

    public BaseItemDto getItem() { return mItem; }
    public Drawable getImageDrawable() { return mImageDrawable; }

    public void setImageBitmap(Context context, Bitmap bm) { mImageDrawable = new BitmapDrawable(context.getResources(), bm); }

    public void addAction(TextUnderButton button) {
        mActions.add(button);
    }

    public InfoItem getInfoItem1() {
        return mInfoItem1;
    }

    public void setInfoItem1(InfoItem mInfoItem1) {
        this.mInfoItem1 = mInfoItem1;
    }

    public InfoItem getInfoItem2() {
        return mInfoItem2;
    }

    public void setInfoItem2(InfoItem mInfoItem2) {
        this.mInfoItem2 = mInfoItem2;
    }

    public InfoItem getInfoItem3() {
        return mInfoItem3;
    }

    public void setInfoItem3(InfoItem mInfoItem3) {
        this.mInfoItem3 = mInfoItem3;
    }
}
