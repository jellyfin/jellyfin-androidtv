package org.jellyfin.androidtv.ui.card;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.BaseCardView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.databinding.ViewCardLegacyImageBinding;
import org.jellyfin.androidtv.ui.AsyncImageView;
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.util.ContextExtensionsKt;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.Utils;

import java.text.NumberFormat;

/**
 * Modified ImageCard with no fade on the badge
 * A card view with an {@link ImageView} as its main region.
 */
public class LegacyImageCardView extends BaseCardView {
    private ViewCardLegacyImageBinding binding = ViewCardLegacyImageBinding.inflate(LayoutInflater.from(getContext()), this);
    private ImageView mBanner;
    private int BANNER_SIZE = Utils.convertDpToPixel(getContext(), 50);
    private int noIconMargin = Utils.convertDpToPixel(getContext(), 5);
    private NumberFormat nf = NumberFormat.getInstance();

    public LegacyImageCardView(Context context, boolean showInfo) {
        super(context, null, androidx.leanback.R.attr.imageCardViewStyle);

        if (!showInfo) {
            setCardType(CARD_TYPE_MAIN_ONLY);
        }

        binding.mainImage.setClipToOutline(true);

        // "hack" to trigger KeyProcessor to open the menu for this item on long press
        setOnLongClickListener(v -> {
            Activity activity = ContextExtensionsKt.getActivity(getContext());
            if (activity == null) return false;
            // Make sure the view is focused so the created menu uses it as anchor
            if (!v.requestFocus()) return false;
            return activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU));
        });

        setForeground(null);
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

    public final AsyncImageView getMainImageView() {
        return binding.mainImage;
    }

    public void setPlayingIndicator(boolean playing) {
        if (playing) {
            // TODO use decent animation for equalizer icon
            binding.extraBadge.setBackgroundResource(R.drawable.ic_play);
            binding.extraBadge.setVisibility(VISIBLE);
        } else {
            binding.extraBadge.setBackgroundResource(R.drawable.blank10x10);
        }
    }

    public void setMainImageDimensions(int width, int height) {
        setMainImageDimensions(width, height, ImageView.ScaleType.CENTER_CROP);
    }

    public void setMainImageDimensions(int width, int height, ImageView.ScaleType scaleType) {
        ViewGroup.LayoutParams lp = binding.mainImage.getLayoutParams();
        lp.width = Math.round(width * getResources().getDisplayMetrics().density);
        lp.height = Math.round(height * getResources().getDisplayMetrics().density);
        binding.mainImage.setLayoutParams(lp);
        binding.mainImage.setScaleType(scaleType);
        if (mBanner != null) mBanner.setX(lp.width - BANNER_SIZE);
        ViewGroup.LayoutParams lp2 = binding.resumeProgress.getLayoutParams();
        lp2.width = lp.width;
        binding.resumeProgress.setLayoutParams(lp2);
    }

    public void setTitleText(CharSequence text) {
        if (binding.title == null) {
            return;
        }

        binding.title.setText(text);
        setTextMaxLines();
    }

    public void setOverlayText(String text) {
        if (getCardType() == BaseCardView.CARD_TYPE_MAIN_ONLY) {
            binding.overlayText.setText(text);
            binding.nameOverlay.setVisibility(VISIBLE);
        } else {
            binding.nameOverlay.setVisibility(GONE);
        }
    }

    public void setOverlayInfo(BaseRowItem item) {
        if (binding.overlayText == null) return;

        if (getCardType() == BaseCardView.CARD_TYPE_MAIN_ONLY && item.getShowCardInfoOverlay()) {
            switch (item.getBaseItem().getType()) {
                case PHOTO:
                    insertCardData(item.getBaseItem().getPremiereDate() != null ? DateTimeExtensionsKt.getDateFormatter(getContext()).format(item.getBaseItem().getPremiereDate()) : item.getFullName(getContext()), R.drawable.ic_camera, true);
                    break;
                case PHOTO_ALBUM:
                    insertCardData(item.getFullName(getContext()), R.drawable.ic_photos, true);
                    break;
                case VIDEO:
                    insertCardData(item.getFullName(getContext()), R.drawable.ic_movie, true);
                    break;
                case FOLDER:
                    insertCardData(item.getFullName(getContext()), R.drawable.ic_folder, true);
                    break;
                case PLAYLIST:
                case MUSIC_ARTIST:
                case PERSON:
                default:
                    binding.overlayText.setText(item.getFullName(getContext()));
                    break;
            }
            if (item instanceof BaseItemDtoBaseRowItem) {
                binding.overlayCount.setText(((BaseItemDtoBaseRowItem) item).getChildCountStr());
            } else {
                binding.overlayCount.setText(null);
            }
            binding.nameOverlay.setVisibility(VISIBLE);
        }
    }

    public void insertCardData (@Nullable String fullName, @NonNull int icon, @NonNull boolean iconVisible) {
        binding.overlayText.setText(fullName);
        if (iconVisible) {
            binding.iconImage.setImageResource(icon);
            binding.icon.setVisibility(VISIBLE);
        }
    }

    public CharSequence getTitle() {
        if (binding.title == null) {
            return null;
        }

        return binding.title.getText();
    }

    public void setContentText(CharSequence text) {
        if (binding.contentText == null) {
            return;
        }

        binding.contentText.setText(text);
        setTextMaxLines();
    }

    public CharSequence getContentText() {
        if (binding.contentText == null) {
            return null;
        }

        return binding.contentText.getText();
    }

    public void setRating(String rating) {
        if (rating != null) {
            binding.badgeText.setText(rating);
            binding.badgeText.setVisibility(VISIBLE);
        } else {
            binding.badgeText.setText("");
            binding.badgeText.setVisibility(GONE);
        }
    }

    public void setBadgeImage(Drawable drawable) {
        if (binding.extraBadge == null) {
            return;
        }

        if (drawable != null) {
            binding.extraBadge.setImageDrawable(drawable);
            binding.extraBadge.setVisibility(View.VISIBLE);
        } else {
            binding.extraBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void setTextMaxLines() {
        if (TextUtils.isEmpty(getTitle())) {
            binding.contentText.setMaxLines(2);
        } else {
            binding.contentText.setMaxLines(1);
        }

        if (TextUtils.isEmpty(getContentText())) {
            binding.title.setMaxLines(2);
        } else {
            binding.title.setMaxLines(1);
        }
    }

    public void clearBanner() {
        if (mBanner != null) {
            mBanner.setVisibility(GONE);
        }
    }

    public void setUnwatchedCount(int count) {
        if (count > 0) {
            binding.unwatchedCount.setText(count > 99 ? getContext().getString(R.string.watch_count_overflow) : nf.format(count));
            binding.unwatchedCount.setVisibility(VISIBLE);
            binding.checkMark.setVisibility(INVISIBLE);
            binding.watchedIndicator.setVisibility(VISIBLE);
        } else if (count == 0) {
            binding.checkMark.setVisibility(VISIBLE);
            binding.unwatchedCount.setVisibility(INVISIBLE);
            binding.watchedIndicator.setVisibility(VISIBLE);
        } else {
            binding.watchedIndicator.setVisibility(GONE);
        }
    }

    public void setProgress(int pct) {
        if (pct > 0) {
            binding.resumeProgress.setProgress(pct);
            binding.resumeProgress.setVisibility(VISIBLE);
        } else {
            binding.resumeProgress.setVisibility(GONE);
        }
    }

    public void showFavIcon(boolean show) {
        binding.favIcon.setVisibility(show ? VISIBLE : GONE);
    }
}
