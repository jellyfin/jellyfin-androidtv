package tv.emby.embyatv.presentation;

/**
 * Created by Eric on 12/29/2014.
 * Modified ImageCard with no fade on the badge
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.ImageCardView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * A card view with an {@link ImageView} as its main region.
 */
public class MyImageCardView extends ImageCardView {

    private ImageView mFadeMask;
    private ImageView mBanner;
    private int BANNER_SIZE = Utils.convertDpToPixel(TvApp.getApplication(), 50);

    public MyImageCardView(Context context) {
        this(context, null);
    }

    public MyImageCardView(Context context, boolean showInfo) {
        this(context);

        if (!showInfo) setCardType(CARD_TYPE_MAIN_ONLY);
    }

    public MyImageCardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.imageCardViewStyle);
    }

    public MyImageCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mFadeMask = (ImageView) this.getRootView().findViewById(R.id.fade_mask);
        mFadeMask.setVisibility(GONE);

    }

    @Override
    public void setBadgeImage(Drawable drawable) {
        super.setBadgeImage(drawable);
        mFadeMask.setVisibility(GONE);
    }

    public void setBanner(int bannerResource) {
        if (mBanner == null) {
            mBanner = new ImageView(getContext());
            mBanner.setLayoutParams(new ViewGroup.LayoutParams(BANNER_SIZE, BANNER_SIZE));

            ((ViewGroup)getRootView()).addView(mBanner);
        }

        mBanner.setImageResource(bannerResource);
        mBanner.setVisibility(VISIBLE);
    }

    @Override
    public void setMainImageDimensions(int width, int height) {
        super.setMainImageDimensions(width, height);
        if (mBanner != null) mBanner.setX(width - BANNER_SIZE);
    }

    public void clearBanner() {
        if (mBanner != null) {
            mBanner.setVisibility(GONE);
        }
    }
}

