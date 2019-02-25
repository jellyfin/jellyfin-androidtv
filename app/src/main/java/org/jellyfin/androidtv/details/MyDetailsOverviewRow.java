package org.jellyfin.androidtv.details;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Row;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.model.dto.BaseItemDto;
import org.jellyfin.androidtv.model.InfoItem;
import org.jellyfin.androidtv.ui.ImageButton;
import org.jellyfin.androidtv.ui.TextUnderButton;

/**
 * Created by Eric on 5/22/2015.
 */
public class MyDetailsOverviewRow extends Row {
    private BaseItemDto mItem;
    private Drawable mImageDrawable;
    private Drawable mStudioDrawable;
    private String mSummary;
    private String mSummaryTitle;
    private String mSummarySubTitle;
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

    public String getSummaryTitle() {
        return mSummaryTitle;
    }

    public void setSummaryTitle(String mSummaryTitle) {
        this.mSummaryTitle = mSummaryTitle;
    }

    public String getSummarySubTitle() {
        return mSummarySubTitle;
    }

    public void setSummarySubTitle(String mSummarySubTitle) {
        this.mSummarySubTitle = mSummarySubTitle;
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
    public Drawable getStudioDrawable() { return mStudioDrawable; }

    public void setImageDrawable(Drawable drawable) { mImageDrawable = drawable; }
    public void setImageBitmap(Context context, Bitmap bm) { mImageDrawable = new BitmapDrawable(context.getResources(), bm); }
    public void setStudioBitmap(Context context, Bitmap bm) { mStudioDrawable = new BitmapDrawable(context.getResources(), bm); }

    public void addAction(TextUnderButton button) {
        mActions.add(button);
    }

    public void addAction(int ndx, TextUnderButton button) {
        mActions.add(ndx, button);
    }

    public void hideAction(TextUnderButton button) {
        button.setVisibility(View.GONE);
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
