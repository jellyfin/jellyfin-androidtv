package org.jellyfin.androidtv.ui.playback;

import android.view.View;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.SubtitleView;

/** Keeps ordinary unpositioned text away from a clipping edge without changing bitmap cue coordinates. */
@OptIn(markerClass = UnstableApi.class)
public final class SubtitleViewSafety {
    public static final int MINIMUM_BOTTOM_MARGIN_DP = 8;
    private static final float MAXIMUM_OFFSET_FRACTION = 0.8f;

    private SubtitleViewSafety() {}

    /** Zero requested offset still leaves room for descenders and their outline, in viewport pixels. */
    public static float bottomPaddingFraction(int height, float density, float requestedOffset) {
        float offset = Float.isFinite(requestedOffset)
                ? Math.max(0, Math.min(MAXIMUM_OFFSET_FRACTION, requestedOffset))
                : SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION;
        if (height <= 0 || !Float.isFinite(density) || density <= 0) return offset;
        return Math.min(MAXIMUM_OFFSET_FRACTION, Math.max(offset, MINIMUM_BOTTOM_MARGIN_DP * density / height));
    }

    /** Recompute the pixel safety margin after the video viewport changes shape; remove this listener on release. */
    public static View.OnLayoutChangeListener apply(SubtitleView view, float requestedOffset) {
        View.OnLayoutChangeListener listener = (changedView, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> update(view, requestedOffset);
        view.addOnLayoutChangeListener(listener);
        update(view, requestedOffset);
        return listener;
    }

    private static void update(SubtitleView view, float requestedOffset) {
        view.setBottomPaddingFraction(bottomPaddingFraction(
                view.getHeight() - view.getPaddingTop() - view.getPaddingBottom(),
                view.getResources().getDisplayMetrics().density, requestedOffset));
    }
}
