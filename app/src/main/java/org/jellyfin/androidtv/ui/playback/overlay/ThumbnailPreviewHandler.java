package org.jellyfin.androidtv.ui.playback.overlay;

import android.graphics.Bitmap;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.leanback.widget.PlaybackSeekDataProvider;

import org.jellyfin.androidtv.R;

public class ThumbnailPreviewHandler {

    private final PlaybackSeekDataProvider seekProvider;
    private final ImageView thumbnailView;
    private final long skipForwardLength;

    public ThumbnailPreviewHandler(PlaybackSeekDataProvider seekProvider,
                                   ImageView thumbnailView,
                                   long skipForwardLength) {
        this.seekProvider = seekProvider;
        this.thumbnailView = thumbnailView;
        this.skipForwardLength = skipForwardLength;
    }

    public void updateThumbnailPreview(long previewSeekPosition) {
        if (seekProvider == null || thumbnailView == null) return;

        int thumbnailIndex = (int) (previewSeekPosition / skipForwardLength);

        PlaybackSeekDataProvider.ResultCallback thumbnailCallback = new PlaybackSeekDataProvider.ResultCallback() {
            @Override
            public void onThumbnailLoaded(Bitmap bitmap, int index) {
                if (bitmap == null) return;

                thumbnailView.setImageBitmap(bitmap);
                showThumbnailPreview();
            }
        };

        seekProvider.getThumbnail(thumbnailIndex, thumbnailCallback);
    }

    public void showThumbnailPreview() {
        if (thumbnailView == null) return;

        thumbnailView.setVisibility(View.VISIBLE);
    }

    public void hideThumbnailPreview() {
        if (thumbnailView == null || thumbnailView.getVisibility() != View.VISIBLE) return;

        Animation fadeOut = AnimationUtils.loadAnimation(thumbnailView.getContext(), R.anim.fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                thumbnailView.setVisibility(View.GONE);
                thumbnailView.setImageBitmap(null); // Clear the bitmap to free memory
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        thumbnailView.startAnimation(fadeOut);
    }

    public boolean isAvailable() {
        return seekProvider != null && thumbnailView != null;
    }
}
