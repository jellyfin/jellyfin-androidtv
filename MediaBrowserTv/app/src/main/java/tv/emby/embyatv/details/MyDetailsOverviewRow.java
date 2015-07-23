package tv.emby.embyatv.details;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Row;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.ui.ImageButton;

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

    private List<ImageButton> mActions = new ArrayList<>();

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

    public List<ImageButton> getActions() { return mActions; }

    public BaseItemDto getItem() { return mItem; }
    public Drawable getImageDrawable() { return mImageDrawable; }
    public Drawable getStudioDrawable() { return mStudioDrawable; }

    public void setImageDrawable(Drawable drawable) { mImageDrawable = drawable; }
    public void setImageBitmap(Context context, Bitmap bm) { mImageDrawable = new BitmapDrawable(context.getResources(), bm); }
    public void setStudioBitmap(Context context, Bitmap bm) { mStudioDrawable = new BitmapDrawable(context.getResources(), bm); }

    public void addAction(ImageButton button) {
        mActions.add(button);
    }

    public void addAction(int ndx, ImageButton button) {
        mActions.add(ndx, button);
    }

    public void hideAction(ImageButton button) {
        button.setVisibility(View.GONE);
    }
}
