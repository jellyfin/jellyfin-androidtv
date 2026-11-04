package org.jellyfin.androidtv.ui.playback;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Moves captions out of the picture into the letterbox bars,
 * whether the player adds the bars or they are encoded into the video (found by {@link EncodedBarDetector}):
 * ordinary and bottom-aligned text and lower PGS images go to the lower bar, top-aligned text and upper PGS
 * images to the upper bar. ASS/SSA lines with their own placement are left where the author put them.
 * Captions that are not moved keep rendering in the picture exactly as before.
 * See https://developer.android.com/reference/androidx/media3/ui/SubtitleView and Media3 1.8 SubtitlePainter.
 */
@OptIn(markerClass = UnstableApi.class)
public final class LetterboxSubtitleOverlay extends FrameLayout {
    private static final float INNER_TEXT_PADDING = 0.125f;
    /** A caption taller than its bar may shrink to this share of its size to fit inside the bar. */
    private static final float MINIMUM_FIT_SCALE = 0.6f;
    // Media3 SsaParser places alignment-only lines at these fractions; overridden placement (\pos) differs.
    private static final float SSA_DEFAULT_BOTTOM_LINE = 0.95f;
    private static final float SSA_DEFAULT_CENTER_POSITION = 0.5f;

    private enum Bar { NONE, TOP, BOTTOM }

    private final PlayerView playerView;
    private final Player player;
    private final SubtitleView originalSubtitles;
    private final SubtitleView pictureSubtitles;
    private final SubtitleView barSubtitles;
    private final View videoSurface;
    private final EncodedBarDetector detector;
    private final float textSizeDp;
    private final float offsetFraction;
    private final TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    private final float spacingMultiplier;
    private final float spacingExtra;
    private final int margin;
    private final int[] viewportLocation = new int[2];
    private final int[] videoLocation = new int[2];
    private final int[] playerLocation = new int[2];
    private final int[] subtitleLocation = new int[2];
    private final Set<String> assSourceIds = new HashSet<>();
    private List<Cue> cues = Collections.emptyList();

    private final OnLayoutChangeListener layoutListener = (view, left, top, right, bottom,
            oldLeft, oldTop, oldRight, oldBottom) -> requestLayout();

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onCues(@NonNull CueGroup cueGroup) {
            cues = cueGroup.cues;
            updatePlacement();
        }

        @Override
        public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
            requestLayout();
        }

        @Override
        public void onTracksChanged(@NonNull Tracks tracks) {
            updatePlacement();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            // Never carry a previous item's captions or crop across a playlist or aspect-ratio transition.
            cues = Collections.emptyList();
            if (detector != null) detector.reset();
            restoreOriginalSubtitles();
        }
    };

    public LetterboxSubtitleOverlay(Context context, PlayerView playerView, CaptionStyleCompat style,
            float textSizeDp, float offsetFraction) {
        super(context);
        this.playerView = playerView;
        this.player = playerView.getPlayer();
        this.originalSubtitles = playerView.getSubtitleView();
        this.videoSurface = playerView.getVideoSurfaceView();
        this.textSizeDp = textSizeDp;
        this.offsetFraction = offsetFraction;
        margin = Math.round(SubtitleViewSafety.MINIMUM_BOTTOM_MARGIN_DP * getResources().getDisplayMetrics().density);
        textPaint.setTypeface(style.typeface);

        TypedArray attributes = context.obtainStyledAttributes(null,
                new int[] {android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier}, 0, 0);
        spacingExtra = attributes.getDimensionPixelSize(0, 0);
        spacingMultiplier = attributes.getFloat(1, 1);
        attributes.recycle();

        // Stands in for the player's own subtitle view while some captions are moved out of the picture.
        pictureSubtitles = new SubtitleView(context);
        pictureSubtitles.setStyle(style);
        pictureSubtitles.setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
        addView(pictureSubtitles, new LayoutParams(0, 0));

        barSubtitles = new SubtitleView(context);
        barSubtitles.setStyle(style);
        barSubtitles.setBottomPaddingFraction(0);
        addView(barSubtitles, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setVisibility(INVISIBLE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        playerView.addOnLayoutChangeListener(layoutListener);
        if (videoSurface != null) videoSurface.addOnLayoutChangeListener(layoutListener);
        player.addListener(listener);
        cues = player.getCurrentCues().cues;

        detector = EncodedBarDetector.isSupported(videoSurface)
                ? new EncodedBarDetector(videoSurface, new EncodedBarDetector.Playback() {
                    @Override
                    public boolean isPlaying() {
                        return player.isPlaying();
                    }

                    @Override
                    public String itemKey() {
                        MediaItem item = player.getCurrentMediaItem();
                        // The path names the video; the query (with credentials) is left out.
                        return item == null || item.localConfiguration == null ? null
                                : item.localConfiguration.uri.getPath();
                    }
                }, this::updatePlacement) : null;
        if (detector != null) detector.start();
    }

    /**
     * Track ids (such as {@code JF_EXTERNAL:3}) whose source was ASS/SSA before the server converted them.
     * Conversion drops the author's placement markers, so only unplaced lines from these tracks are moved.
     */
    public void setAssSourceIds(Collection<String> ids) {
        assSourceIds.clear();
        assSourceIds.addAll(ids);
        updatePlacement();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updatePlacement();
    }

    private void updatePlacement() {
        if (originalSubtitles == null || videoSurface == null || cues.isEmpty()
                || playerView.getResizeMode() != AspectRatioFrameLayout.RESIZE_MODE_FIT
                || player.getVideoSize().height <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            restoreOriginalSubtitles();
            return;
        }

        getLocationInWindow(viewportLocation);
        playerView.getLocationInWindow(playerLocation);
        // Guide/PiP contraction uses a smaller video viewport; leave those captions with the existing player.
        if (playerView.getWidth() != getWidth() || playerView.getHeight() != getHeight()
                || playerLocation[0] != viewportLocation[0] || playerLocation[1] != viewportLocation[1]) {
            restoreOriginalSubtitles();
            return;
        }

        videoSurface.getLocationInWindow(videoLocation);
        int width = getWidth();
        int height = getHeight();
        int videoLeft = videoLocation[0] - viewportLocation[0];
        int videoTop = videoLocation[1] - viewportLocation[1];
        int videoHeight = videoSurface.getHeight();
        // Horizontal cue positions are kept, so the picture must span the viewport width.
        if (videoLeft != 0 || videoSurface.getWidth() != width || videoHeight <= 0) {
            restoreOriginalSubtitles();
            return;
        }

        float topFraction = detector != null ? detector.topFraction() : 0;
        float bottomFraction = detector != null ? detector.bottomFraction() : 1;
        int pictureTop = videoTop + Math.round(topFraction * videoHeight);
        int pictureBottom = videoTop + Math.round(bottomFraction * videoHeight);
        boolean encodedBars = topFraction > 0 || bottomFraction < 1;
        int tolerance = encodedBars ? Math.max(LetterboxSubtitlePlacement.GEOMETRY_TOLERANCE, height / 50)
                : LetterboxSubtitlePlacement.GEOMETRY_TOLERANCE;
        if (!LetterboxSubtitlePlacement.hasBars(height, pictureTop, pictureBottom, tolerance)) {
            restoreOriginalSubtitles();
            return;
        }

        originalSubtitles.getLocationInWindow(subtitleLocation);
        Rect cueArea = new Rect(
                subtitleLocation[0] - viewportLocation[0] + originalSubtitles.getPaddingLeft(),
                subtitleLocation[1] - viewportLocation[1] + originalSubtitles.getPaddingTop(),
                subtitleLocation[0] - viewportLocation[0] + originalSubtitles.getWidth() - originalSubtitles.getPaddingRight(),
                subtitleLocation[1] - viewportLocation[1] + originalSubtitles.getHeight() - originalSubtitles.getPaddingBottom());
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSizeDp, getResources().getDisplayMetrics());
        if (cueArea.isEmpty() || !Float.isFinite(textSize) || textSize <= 0 || !Float.isFinite(offsetFraction)) {
            restoreOriginalSubtitles();
            return;
        }
        int requestedInset = Math.round(Math.max(0, offsetFraction) * height);

        Format format = selectedTextFormat();
        boolean ssaParsed = format != null && (MimeTypes.TEXT_SSA.equals(format.sampleMimeType)
                || (format.codecs != null && format.codecs.contains(MimeTypes.TEXT_SSA)));
        boolean convertedAss = format != null && format.id != null && isAssSource(format.id);

        List<Cue> moved = new ArrayList<>();
        List<Cue> kept = new ArrayList<>();
        List<Cue> topText = new ArrayList<>();
        List<Cue> bottomText = new ArrayList<>();
        List<Cue> topBitmaps = new ArrayList<>();
        List<Cue> bottomBitmaps = new ArrayList<>();
        int pictureMiddle = (pictureTop + pictureBottom) / 2;
        for (Cue cue : cues) {
            if (cue.bitmap != null) {
                Rect rect = bitmapRect(cue, cueArea);
                if (rect == null) kept.add(cue);
                else if (rect.centerY() > pictureMiddle) bottomBitmaps.add(cue);
                else topBitmaps.add(cue);
                continue;
            }
            Bar bar = textBar(cue, ssaParsed, convertedAss);
            if (bar == Bar.BOTTOM) bottomText.add(cue);
            else if (bar == Bar.TOP) topText.add(cue);
            else kept.add(cue);
        }

        // Independently placed text cues would overlap in a bar, so only a single caption per bar is moved.
        moveText(topText, Bar.TOP, moved, kept, width, height, pictureTop, pictureBottom, tolerance, textSize, cueArea, requestedInset);
        moveText(bottomText, Bar.BOTTOM, moved, kept, width, height, pictureTop, pictureBottom, tolerance, textSize, cueArea, requestedInset);
        moveBitmaps(topBitmaps, Bar.TOP, moved, kept, cueArea, width, height, pictureTop, pictureBottom, tolerance, requestedInset);
        moveBitmaps(bottomBitmaps, Bar.BOTTOM, moved, kept, cueArea, width, height, pictureTop, pictureBottom, tolerance, requestedInset);

        if (moved.isEmpty()) {
            restoreOriginalSubtitles();
            return;
        }

        pictureSubtitles.setBottomPaddingFraction(SubtitleViewSafety.bottomPaddingFraction(cueArea.height(),
                getResources().getDisplayMetrics().density, offsetFraction));
        pictureSubtitles.measure(MeasureSpec.makeMeasureSpec(cueArea.width(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(cueArea.height(), MeasureSpec.EXACTLY));
        pictureSubtitles.layout(cueArea.left, cueArea.top, cueArea.right, cueArea.bottom);
        pictureSubtitles.setCues(kept);

        // Keep the existing readable font size; sizing it relative to the thin bar would make the text tiny.
        barSubtitles.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        barSubtitles.setCues(moved);
        originalSubtitles.setVisibility(INVISIBLE);
        setVisibility(VISIBLE);
    }

    private void moveText(List<Cue> candidates, Bar bar, List<Cue> moved, List<Cue> kept, int width, int height,
            int pictureTop, int pictureBottom, int tolerance, float textSize, Rect cueArea, int requestedInset) {
        if (candidates.size() != 1) {
            kept.addAll(candidates);
            return;
        }
        Cue cue = candidates.get(0);
        float cueTextSize = cueTextSize(cue, textSize, cueArea);
        int textHeight = measureCaptionHeight(cue, cueTextSize, width);
        // Large text sizes make captions taller than the bar; shrink a little rather than cover the picture.
        int room = (bar == Bar.BOTTOM ? height - pictureBottom : pictureTop) - margin * 2;
        boolean resized = false;
        for (int attempt = 0; attempt < 3 && textHeight > room && room > 0; attempt++) {
            float minimum = cueTextSize(cue, textSize, cueArea) * MINIMUM_FIT_SCALE;
            if (cueTextSize <= minimum) break;
            cueTextSize = Math.max(minimum, cueTextSize * room / textHeight);
            textHeight = measureCaptionHeight(cue, cueTextSize, width);
            resized = true;
        }
        LetterboxSubtitlePlacement placement = textHeight <= 0 ? null : bar == Bar.BOTTOM
                ? LetterboxSubtitlePlacement.bottom(height, pictureTop, pictureBottom, tolerance, textHeight, margin, requestedInset)
                : LetterboxSubtitlePlacement.top(height, pictureTop, pictureBottom, tolerance, textHeight, margin);
        if (placement == null) {
            kept.add(cue);
            return;
        }
        Cue.Builder builder = cue.buildUpon();
        if (bar == Bar.BOTTOM) {
            builder.setLine((float) (placement.top + placement.height - placement.inset) / height, Cue.LINE_TYPE_FRACTION)
                    .setLineAnchor(Cue.ANCHOR_TYPE_END);
        } else {
            builder.setLine((float) (placement.top + placement.inset) / height, Cue.LINE_TYPE_FRACTION)
                    .setLineAnchor(Cue.ANCHOR_TYPE_START);
        }
        if (resized || cue.textSize != Cue.DIMEN_UNSET) builder.setTextSize(cueTextSize / height, Cue.TEXT_SIZE_TYPE_FRACTIONAL_IGNORE_PADDING);
        moved.add(builder.build());
    }

    private void moveBitmaps(List<Cue> group, Bar bar, List<Cue> moved, List<Cue> kept, Rect cueArea, int width,
            int height, int pictureTop, int pictureBottom, int tolerance, int requestedInset) {
        if (group.isEmpty()) return;
        // Multi-object PGS captions (separate lines or speakers) move together, keeping their layout.
        Rect union = null;
        List<Rect> rects = new ArrayList<>();
        for (Cue cue : group) {
            Rect rect = bitmapRect(cue, cueArea);
            rects.add(rect);
            if (union == null) union = new Rect(rect);
            else union.union(rect);
        }

        int shift = 0;
        if (bar == Bar.BOTTOM && union.top < pictureBottom) {
            LetterboxSubtitlePlacement placement = LetterboxSubtitlePlacement.bottom(height, pictureTop, pictureBottom,
                    tolerance, union.height(), margin, requestedInset);
            if (placement != null) shift = Math.max(0, placement.top + placement.height - placement.inset - union.bottom);
        } else if (bar == Bar.TOP && union.bottom > pictureTop) {
            LetterboxSubtitlePlacement placement = LetterboxSubtitlePlacement.top(height, pictureTop, pictureBottom,
                    tolerance, union.height(), margin);
            if (placement != null) shift = Math.min(0, placement.top + placement.inset - union.top);
        }
        // Images already in a bar were placed there by the disc author.
        if (shift == 0) {
            kept.addAll(group);
            return;
        }
        for (int i = 0; i < group.size(); i++) {
            Rect rect = rects.get(i);
            moved.add(group.get(i).buildUpon()
                    .setPosition((float) rect.left / width)
                    .setPositionAnchor(Cue.ANCHOR_TYPE_START)
                    .setLine((float) (rect.top + shift) / height, Cue.LINE_TYPE_FRACTION)
                    .setLineAnchor(Cue.ANCHOR_TYPE_START)
                    .setSize((float) rect.width() / width)
                    .setBitmapHeight((float) rect.height() / height)
                    .build());
        }
    }

    /** Where Media3's SubtitlePainter draws a bitmap cue, in viewport pixels. */
    @Nullable
    private static Rect bitmapRect(Cue cue, Rect area) {
        if (cue.bitmap == null || cue.size == Cue.DIMEN_UNSET || cue.line == Cue.DIMEN_UNSET
                || cue.position == Cue.DIMEN_UNSET) return null;
        float anchorX = area.left + area.width() * cue.position;
        float anchorY = area.top + area.height() * cue.line;
        int w = Math.round(area.width() * cue.size);
        int h = cue.bitmapHeight != Cue.DIMEN_UNSET ? Math.round(area.height() * cue.bitmapHeight)
                : Math.round(w * ((float) cue.bitmap.getHeight() / cue.bitmap.getWidth()));
        int x = Math.round(cue.positionAnchor == Cue.ANCHOR_TYPE_END ? anchorX - w
                : cue.positionAnchor == Cue.ANCHOR_TYPE_MIDDLE ? anchorX - w / 2f : anchorX);
        int y = Math.round(cue.lineAnchor == Cue.ANCHOR_TYPE_END ? anchorY - h
                : cue.lineAnchor == Cue.ANCHOR_TYPE_MIDDLE ? anchorY - h / 2f : anchorY);
        return w > 0 && h > 0 ? new Rect(x, y, x + w, y + h) : null;
    }

    private static Bar textBar(Cue cue, boolean ssaParsed, boolean convertedAss) {
        if (cue.text == null || cue.text.length() == 0 || cue.verticalType != Cue.TYPE_UNSET) return Bar.NONE;
        if (ssaParsed) {
            // Only dialogue placed by the default bottom-centre alignment; \pos, \move and \an overrides stay put.
            boolean defaultDialogue = cue.line == SSA_DEFAULT_BOTTOM_LINE && cue.lineType == Cue.LINE_TYPE_FRACTION
                    && cue.lineAnchor == Cue.ANCHOR_TYPE_END && cue.position == SSA_DEFAULT_CENTER_POSITION
                    && cue.positionAnchor == Cue.ANCHOR_TYPE_MIDDLE;
            return defaultDialogue ? Bar.BOTTOM : Bar.NONE;
        }
        if (cue.textSize != Cue.DIMEN_UNSET) return Bar.NONE;
        if (cue.line == Cue.DIMEN_UNSET) return Bar.BOTTOM;
        // Converted ASS keeps only crude alignment markers; any explicit placement is the author's choice.
        if (convertedAss || cue.lineType != Cue.LINE_TYPE_FRACTION) return Bar.NONE;
        // SubRip {\an7-9} / {\an1-3} alignment, and equivalent WebVTT/TTML edge placement.
        if (cue.lineAnchor == Cue.ANCHOR_TYPE_START && cue.line < 0.25f) return Bar.TOP;
        if (cue.lineAnchor == Cue.ANCHOR_TYPE_END && cue.line > 0.75f) return Bar.BOTTOM;
        return Bar.NONE;
    }

    private float cueTextSize(Cue cue, float defaultSize, Rect cueArea) {
        if (cue.textSize == Cue.DIMEN_UNSET) return defaultSize;
        if (cue.textSizeType == Cue.TEXT_SIZE_TYPE_ABSOLUTE) return cue.textSize;
        return cue.textSize * cueArea.height();
    }

    @Nullable
    private Format selectedTextFormat() {
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_TEXT) continue;
            for (int i = 0; i < group.length; i++) {
                if (group.isTrackSelected(i)) return group.getTrackFormat(i);
            }
        }
        return null;
    }

    private boolean isAssSource(String formatId) {
        // Side-loaded track ids are prefixed with their source index, e.g. "1:JF_EXTERNAL:3".
        for (String id : assSourceIds) {
            if (formatId.equals(id) || formatId.endsWith(":" + id)) return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private int measureCaptionHeight(Cue cue, float textSize, int viewWidth) {
        // Match Media3's canvas text wrapping, including a second layout at the measured text width.
        // The legacy StaticLayout constructor also supports this app's Android 5 minimum SDK.
        textPaint.setTextSize(textSize);
        int padding = (int) (textSize * INNER_TEXT_PADDING + 0.5f);
        int available = cue.size != Cue.DIMEN_UNSET ? Math.round(viewWidth * cue.size) : viewWidth;
        int width = available - padding * 2;
        if (width <= 0) return -1;
        Layout.Alignment alignment = cue.textAlignment != null ? cue.textAlignment : Layout.Alignment.ALIGN_CENTER;
        StaticLayout layout = new StaticLayout(cue.text, textPaint, width, alignment, spacingMultiplier, spacingExtra, true);
        if (cue.size == Cue.DIMEN_UNSET) {
            int textWidth = 0;
            for (int line = 0; line < layout.getLineCount(); line++) {
                textWidth = Math.max(textWidth, (int) Math.ceil(layout.getLineWidth(line)));
            }
            width = Math.min(viewWidth, textWidth + padding * 2);
            if (width <= 0) return -1;
            layout = new StaticLayout(cue.text, textPaint, width, alignment, spacingMultiplier, spacingExtra, true);
        }
        return layout.getHeight();
    }

    private void restoreOriginalSubtitles() {
        barSubtitles.setCues(null);
        pictureSubtitles.setCues(null);
        if (originalSubtitles != null) originalSubtitles.setVisibility(VISIBLE);
        setVisibility(INVISIBLE);
    }

    /** Removes callbacks before releasing the player, including callbacks retaining the old activity. */
    public void release() {
        player.removeListener(listener);
        playerView.removeOnLayoutChangeListener(layoutListener);
        if (videoSurface != null) videoSurface.removeOnLayoutChangeListener(layoutListener);
        if (detector != null) detector.release();
        cues = Collections.emptyList();
        restoreOriginalSubtitles();
        if (getParent() instanceof FrameLayout) ((FrameLayout) getParent()).removeView(this);
    }
}
