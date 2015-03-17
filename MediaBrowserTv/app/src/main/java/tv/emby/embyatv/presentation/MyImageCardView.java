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
import android.widget.ImageView;

/**
 * A card view with an {@link ImageView} as its main region.
 */
public class MyImageCardView extends ImageCardView {

    private ImageView mFadeMask;

    public MyImageCardView(Context context) {
        this(context, null);
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
}

