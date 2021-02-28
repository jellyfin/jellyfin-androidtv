package org.jellyfin.androidtv.ui.presentation;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.BaseCardView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;

/**
 * Modified ImageCard with no fade on the badge
 * A card view with an {@link ImageView} as its main region.
 */
public class MyImageCardView extends BaseCardView {
    private ImageView mBanner;
    private ViewGroup mInfoOverlay;
    private ImageView mOverlayIcon;
    private TextView mOverlayName;
    private TextView mOverlayCount;
    private ImageView mImageView;
    private TextView mTitleView;
    private TextView mContentView;
    private ImageView mBadgeImage;
    private ImageView mFavIcon;
    private RelativeLayout mWatchedIndicator;
    private ImageView mWatchedMark;
    private TextView mUnwatchedCount;
    private ProgressBar mProgress;
    private TextView mBadgeText;
    private int BANNER_SIZE = Utils.convertDpToPixel(TvApp.getApplication(), 50);
    private int noIconMargin = Utils.convertDpToPixel(TvApp.getApplication(), 5);

    public MyImageCardView(Context context, boolean showInfo) {
        super(context, null, R.attr.imageCardViewStyle);

        if (!showInfo) {
            setCardType(CARD_TYPE_MAIN_ONLY);
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.image_card_view, this);

        mImageView = v.findViewById(R.id.main_image);
        mTitleView = v.findViewById(R.id.title_text);
        mContentView = v.findViewById(R.id.content_text);
        mBadgeImage = v.findViewById(R.id.extra_badge);
        mOverlayName = v.findViewById(R.id.overlay_text);
        mOverlayCount = v.findViewById(R.id.overlay_count);
        mOverlayIcon = v.findViewById(R.id.icon);
        mInfoOverlay = v.findViewById(R.id.name_overlay);
        mInfoOverlay.setVisibility(GONE);
        mFavIcon = v.findViewById(R.id.favIcon);
        mWatchedIndicator = v.findViewById(R.id.watchedIndicator);
        mWatchedMark = v.findViewById(R.id.checkMark);
        mUnwatchedCount = v.findViewById(R.id.unwatchedCount);
        mProgress = v.findViewById(R.id.resumeProgress);
        mBadgeText = v.findViewById(R.id.badge_text);

        mImageView.setClipToOutline(true);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            mTitleView.setTextColor(ContextCompat.getColor(getContext(), R.color.lb_basic_card_title_text_color));
            mContentView.setTextColor(ContextCompat.getColor(getContext(), R.color.lb_basic_card_title_text_color));
            mBadgeText.setTextColor(ContextCompat.getColor(getContext(), R.color.lb_basic_card_title_text_color));
            mBadgeImage.setAlpha(1.0f);
        } else {
            mTitleView.setTextColor(ContextCompat.getColor(getContext(), R.color.lb_basic_card_title_text_color_inactive));
            mContentView.setTextColor(ContextCompat.getColor(getContext(), R.color.lb_basic_card_title_text_color_inactive));
            mBadgeText.setTextColor(ContextCompat.getColor(getContext(), R.color.lb_basic_card_title_text_color_inactive));
            mBadgeImage.setAlpha(0.25f);
        }
    }

    public void setBanner(int bannerResource) {
        if (mBanner == null) {
            mBanner = new ImageView(getContext());
            mBanner.setLayoutParams(new ViewGroup.LayoutParams(BANNER_SIZE, BANNER_SIZE));

            ((ViewGroup) getRootView()).addView(mBanner);
        }

        mBanner.setImageResource(bannerResource);
        mBanner.setVisibility(VISIBLE);
    }

    public final ImageView getMainImageView() {
        return mImageView;
    }

    public void setPlayingIndicator(boolean playing) {
        if (playing) {
            // TODO use decent animation for equalizer icon
            mBadgeImage.setBackgroundResource(R.drawable.ic_play);
            mBadgeImage.setVisibility(VISIBLE);
        } else {
            mBadgeImage.setBackgroundResource(R.drawable.blank10x10);
        }
    }

    public void setMainImageDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mImageView.setLayoutParams(lp);
        if (mBanner != null) mBanner.setX(width - BANNER_SIZE);
        ViewGroup.LayoutParams lp2 = mProgress.getLayoutParams();
        lp2.width = width;
        mProgress.setLayoutParams(lp2);
    }

    public void setTitleText(CharSequence text) {
        if (mTitleView == null) {
            return;
        }

        mTitleView.setText(text);
        setTextMaxLines();
    }

    public void setOverlayText(String text) {
        if (getCardType() == BaseCardView.CARD_TYPE_MAIN_ONLY) {
            mOverlayName.setText(text);
            mInfoOverlay.setVisibility(VISIBLE);
            hideIcon();
        } else {
            mInfoOverlay.setVisibility(GONE);
        }
    }

    public void setOverlayInfo(BaseRowItem item) {
        if (mOverlayName == null) return;

        if (getCardType() == BaseCardView.CARD_TYPE_MAIN_ONLY && item.showCardInfoOverlay()) {
            switch (item.getBaseItemType()) {
                case Photo:
                    mOverlayName.setText(item.getBaseItem().getPremiereDate() != null ? android.text.format.DateFormat.getDateFormat(TvApp.getApplication()).format(TimeUtils.convertToLocalDate(item.getBaseItem().getPremiereDate())) : item.getFullName());
                    mOverlayIcon.setImageResource(R.drawable.ic_camera);
                    break;
                case PhotoAlbum:
                    mOverlayName.setText(item.getFullName());
                    mOverlayIcon.setImageResource(R.drawable.ic_photos);
                    break;
                case Video:
                    mOverlayName.setText(item.getFullName());
                    mOverlayIcon.setImageResource(R.drawable.ic_movie);
                    break;
                case Playlist:
                case MusicArtist:
                case Person:
                    mOverlayName.setText(item.getFullName());
                    hideIcon();
                    break;
                default:
                    mOverlayName.setText(item.getFullName());
                    mOverlayIcon.setImageResource(item.isFolder() ? R.drawable.ic_folder : R.drawable.blank30x30);
                    break;
            }
            mOverlayCount.setText(item.getChildCountStr());
            mInfoOverlay.setVisibility(VISIBLE);
        } else {
            mInfoOverlay.setVisibility(GONE);
        }
    }

    protected void hideIcon() {
        mOverlayIcon.setVisibility(GONE);
        RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) mOverlayName.getLayoutParams();
        parms.rightMargin = noIconMargin;
        parms.leftMargin = noIconMargin;
        mOverlayName.setLayoutParams(parms);
    }

    public CharSequence getTitleText() {
        if (mTitleView == null) {
            return null;
        }

        return mTitleView.getText();
    }

    public void setContentText(CharSequence text) {
        if (mContentView == null) {
            return;
        }

        mContentView.setText(text);
        setTextMaxLines();
    }

    public CharSequence getContentText() {
        if (mContentView == null) {
            return null;
        }

        return mContentView.getText();
    }

    public void setRating(String rating) {
        if (rating != null) {
            mBadgeText.setText(rating);
            mBadgeText.setVisibility(VISIBLE);
        } else {
            mBadgeText.setText("");
            mBadgeText.setVisibility(GONE);
        }
    }

    public void setBadgeImage(Drawable drawable) {
        if (mBadgeImage == null) {
            return;
        }

        if (drawable != null) {
            mBadgeImage.setImageDrawable(drawable);
            mBadgeImage.setVisibility(View.VISIBLE);
        } else {
            mBadgeImage.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void setTextMaxLines() {
        if (TextUtils.isEmpty(getTitleText())) {
            mContentView.setMaxLines(2);
        } else {
            mContentView.setMaxLines(1);
        }

        if (TextUtils.isEmpty(getContentText())) {
            mTitleView.setMaxLines(2);
        } else {
            mTitleView.setMaxLines(1);
        }
    }

    public void clearBanner() {
        if (mBanner != null) {
            mBanner.setVisibility(GONE);
        }
    }

    public void setUnwatchedCount(int count) {
        if (count > 0) {
            mUnwatchedCount.setText(count > 99 ? getContext().getString(R.string.watch_count_overflow) : Integer.toString(count));
            mUnwatchedCount.setVisibility(VISIBLE);
            mWatchedMark.setVisibility(INVISIBLE);
            mWatchedIndicator.setVisibility(VISIBLE);
        } else if (count == 0) {
            mWatchedMark.setVisibility(VISIBLE);
            mUnwatchedCount.setVisibility(INVISIBLE);
            mWatchedIndicator.setVisibility(VISIBLE);
        } else {
            mWatchedIndicator.setVisibility(GONE);
        }
    }

    public void setProgress(int pct) {
        if (pct > 0) {
            mProgress.setProgress(pct);
            mProgress.setVisibility(VISIBLE);
        } else {
            mProgress.setVisibility(GONE);
        }
    }

    public void showFavIcon(boolean show) {
        mFavIcon.setVisibility(show ? VISIBLE : GONE);
    }
}
