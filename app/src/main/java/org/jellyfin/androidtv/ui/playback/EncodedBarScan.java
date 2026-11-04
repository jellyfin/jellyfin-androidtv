package org.jellyfin.androidtv.ui.playback;

/**
 * Finds black bars encoded into video frames from downscaled luma samples, and keeps a stable result:
 * a new crop is accepted only after several matching frames, and dark or asymmetric frames (night scenes,
 * fades) never discard a crop that is already known.
 */
public final class EncodedBarScan {
    /** Luma (0-255) above which a pixel belongs to the picture rather than a bar. */
    static final int PICTURE_LUMA = 28;
    /** Matching frames required before a new crop replaces the current one. */
    static final int REQUIRED_MATCHES = 3;

    private int pictureTop = -1;
    private int pictureBottom = -1;
    private int rows;
    private int candidateTop = -1;
    private int candidateBottom = -1;
    private int matches;

    /** Rows [first, last + 1) that contain picture, or null when the frame is too dark to measure. */
    static int[] pictureRows(int[] luma, int width, int height) {
        if (width <= 0 || height <= 0 || luma.length < width * height) return null;
        int needed = Math.max(2, width / 50);
        int first = -1;
        int last = -1;
        for (int y = 0; y < height; y++) {
            int bright = 0;
            for (int x = y * width, end = x + width; x < end && bright < needed; x++) {
                if (luma[x] > PICTURE_LUMA) bright++;
            }
            if (bright >= needed) {
                if (first < 0) first = y;
                last = y;
            }
        }
        // Less than a third of the frame lit is a dark scene, not a reliable picture edge.
        if (first < 0 || (last + 1 - first) * 3 < height) return null;
        return new int[] {first, last + 1};
    }

    /** Feeds one sampled frame; returns true when the accepted picture rows changed. */
    public boolean update(int[] luma, int width, int height) {
        if (height != rows) {
            rows = height;
            pictureTop = pictureBottom = candidateTop = candidateBottom = -1;
            matches = 0;
        }
        int[] picture = pictureRows(luma, width, height);
        if (picture == null) return false;

        int top = picture[0];
        int bottom = picture[1];
        int tolerance = Math.max(1, height / 50);
        int topBar = top;
        int bottomBar = height - bottom;
        if (topBar <= tolerance && bottomBar <= tolerance) {
            // Lit to both edges: full-frame picture.
            top = 0;
            bottom = height;
        } else if (Math.abs(topBar - bottomBar) > tolerance || Math.min(topBar, bottomBar) < height / 25) {
            // One-sided darkness is scene content (a dark floor or sky); keep whatever is known.
            return false;
        }

        if (top == pictureTop && bottom == pictureBottom) {
            matches = 0;
            return false;
        }
        if (Math.abs(top - candidateTop) <= 1 && Math.abs(bottom - candidateBottom) <= 1) {
            matches++;
        } else {
            candidateTop = top;
            candidateBottom = bottom;
            matches = 1;
        }
        if (matches < REQUIRED_MATCHES) return false;
        pictureTop = top;
        pictureBottom = bottom;
        matches = 0;
        return true;
    }

    /** True once a crop, or a full-frame picture, has been accepted. */
    public boolean measured() {
        return pictureTop >= 0;
    }

    /** Accepted picture top as a fraction of the frame height, or 0 when no bars are known. */
    public float topFraction() {
        return pictureTop <= 0 || rows <= 0 ? 0 : (float) pictureTop / rows;
    }

    /** Accepted picture bottom as a fraction of the frame height, or 1 when no bars are known. */
    public float bottomFraction() {
        return pictureBottom <= 0 || pictureBottom >= rows ? 1 : (float) pictureBottom / rows;
    }

    /** The accepted crop as {rows, top, bottom}, for {@link #restore}; null before one is measured. */
    public int[] snapshot() {
        return measured() ? new int[] {rows, pictureTop, pictureBottom} : null;
    }

    public void restore(int[] crop) {
        reset();
        rows = crop[0];
        pictureTop = crop[1];
        pictureBottom = crop[2];
    }

    public void reset() {
        rows = 0;
        pictureTop = pictureBottom = candidateTop = candidateBottom = -1;
        matches = 0;
    }
}
