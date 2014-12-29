package tv.mediabrowser.mediabrowsertv;

/**
 * Created by Eric on 12/29/2014.
 * Modified ImageCard with no fade on the badge
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.BaseCardView;
import android.support.v17.leanback.widget.ImageCardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

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

