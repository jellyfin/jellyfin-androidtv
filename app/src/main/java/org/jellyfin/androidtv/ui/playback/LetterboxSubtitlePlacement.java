package org.jellyfin.androidtv.ui.playback;

/** Geometry for moving a caption into the lower or upper letterbox bar. */
public final class LetterboxSubtitlePlacement {
    /** Rows the player's own letterbox bars may differ by after viewport rounding. */
    public static final int GEOMETRY_TOLERANCE = 2;

    public final int top;
    public final int height;
    /** Distance between the caption and the screen edge it is anchored to. */
    public final int inset;

    private LetterboxSubtitlePlacement(int top, int height, int inset) {
        this.top = top;
        this.height = height;
        this.inset = inset;
    }

    /**
     * Returns null when the fitted video has no lower bar. A caption taller than the bar keeps its bottom lines
     * in the bar and extends upward into the video, so as much of it as possible stays out of the picture.
     * Coordinates describe the actual video surface within the playback viewport, in pixels.
     */
    public static LetterboxSubtitlePlacement calculate(int width, int height, int videoLeft, int videoTop,
            int videoRight, int videoBottom, int captionHeight, int margin, int requestedBottomInset) {
        if (width <= 0 || videoLeft < 0 || videoRight <= videoLeft || videoRight > width) return null;
        // A fitted wide video spans the viewport width. Pillarboxing and smaller video windows do not qualify.
        if (videoLeft != 0 || videoRight != width) return null;
        return bottom(height, videoTop, videoBottom, GEOMETRY_TOLERANCE, captionHeight, margin, requestedBottomInset);
    }

    /**
     * Places a caption in the lower bar below the picture rows [pictureTop, pictureBottom) of the viewport.
     * The bars may be added by the player, encoded into the video, or both.
     */
    public static LetterboxSubtitlePlacement bottom(int height, int pictureTop, int pictureBottom, int tolerance,
            int captionHeight, int margin, int requestedBottomInset) {
        if (!hasBars(height, pictureTop, pictureBottom, tolerance) || captionHeight <= 0 || margin < 0
                || requestedBottomInset < 0) return null;
        int barHeight = height - pictureBottom;
        if (margin > barHeight / 2) return null;

        if (captionHeight > barHeight - margin * 2) {
            // Anchor at the lowest safe position; captions taller than half the screen stay with the video renderer.
            int regionHeight = captionHeight + margin * 2;
            if (regionHeight > height / 2) return null;
            return new LetterboxSubtitlePlacement(height - regionHeight, regionHeight, margin);
        }

        int maximumInset = barHeight - captionHeight - margin;
        int inset = Math.max(margin, Math.min(requestedBottomInset, maximumInset));
        return new LetterboxSubtitlePlacement(pictureBottom, barHeight, inset);
    }

    /** Mirror of {@link #bottom} for the upper bar: anchored below the top edge, extending downward when tall. */
    public static LetterboxSubtitlePlacement top(int height, int pictureTop, int pictureBottom, int tolerance,
            int captionHeight, int margin) {
        if (!hasBars(height, pictureTop, pictureBottom, tolerance) || captionHeight <= 0 || margin < 0) return null;
        if (margin > pictureTop / 2) return null;

        int regionHeight = Math.max(pictureTop, captionHeight + margin * 2);
        if (regionHeight > height / 2) return null;
        return new LetterboxSubtitlePlacement(0, regionHeight, margin);
    }

    /** True for a picture with bars of (nearly) equal height above and below it. */
    static boolean hasBars(int height, int pictureTop, int pictureBottom, int tolerance) {
        if (height <= 0 || pictureTop <= 0 || pictureBottom <= pictureTop || pictureBottom >= height) return false;
        return Math.abs((long) pictureTop - (height - pictureBottom)) <= tolerance;
    }
}
