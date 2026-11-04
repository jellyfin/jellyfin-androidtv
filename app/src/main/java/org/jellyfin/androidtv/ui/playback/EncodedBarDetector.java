package org.jellyfin.androidtv.ui.playback;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Samples the video surface with PixelCopy (Android 7+) and reports black bars that are encoded into the video
 * itself. Copies that fail, such as for protected or tunneled output, leave no bars known.
 * <p>
 * A copy runs on the render thread that also draws the UI, and on some devices takes most of a second, freezing
 * everything drawn over the video. So copies are made only while video is playing, once a second until the
 * picture is measured, and then not at all until the next item.
 */
public final class EncodedBarDetector {
    /** Whether the player is currently showing moving video. */
    public interface Playback {
        boolean isPlaying();

        /** Identifies the playing video, or null; a crop measured for it is reused when it plays again. */
        String itemKey();
    }

    private static final int SAMPLE_WIDTH = 96;
    private static final int SAMPLE_HEIGHT = 270;
    private static final long QUICK_INTERVAL_MS = 1000;
    private static final long SLOW_INTERVAL_MS = 30000;
    /** Successful copies made at the quick interval before giving up on frequent measuring (dark films). */
    private static final int QUICK_COPIES = 20;
    /** A copy slower than this stalls the display on this device, so only a few are made per item. */
    private static final long SLOW_COPY_MS = 100;
    /** Copy attempts per item on such devices: enough for the matching frames a crop needs. */
    private static final int SLOW_DEVICE_COPIES = EncodedBarScan.REQUIRED_MATCHES + 2;

    /** Crops measured this session, so resuming or replaying an item costs no frame copies. Main thread only. */
    private static final Map<String, int[]> KNOWN_CROPS = new LinkedHashMap<String, int[]>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, int[]> eldest) {
            return size() > 50;
        }
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final HandlerThread thread = new HandlerThread("EncodedBarDetector");
    private final Handler copyHandler;
    private final EncodedBarScan scan = new EncodedBarScan();
    private final View surface;
    private final Playback playback;
    private final Runnable onChange;
    private final int[] pixels = new int[SAMPLE_WIDTH * SAMPLE_HEIGHT];
    private final int[] luma = new int[SAMPLE_WIDTH * SAMPLE_HEIGHT];
    private Bitmap bitmap;
    private boolean running;
    private boolean copying;
    private long copyStarted;
    private int copies;
    private int attempts;
    private boolean slowDevice;

    private final Runnable sample = this::sample;

    public EncodedBarDetector(View surface, Playback playback, Runnable onChange) {
        this.surface = surface;
        this.playback = playback;
        this.onChange = onChange;
        thread.start();
        copyHandler = new Handler(thread.getLooper());
    }

    public static boolean isSupported(View surface) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && surface instanceof SurfaceView;
    }

    public void start() {
        if (running || !isSupported(surface)) return;
        running = true;
        if (bitmap == null) bitmap = Bitmap.createBitmap(SAMPLE_WIDTH, SAMPLE_HEIGHT, Bitmap.Config.ARGB_8888);
        schedule(QUICK_INTERVAL_MS);
    }

    public void stop() {
        running = false;
        mainHandler.removeCallbacks(sample);
    }

    /** Forget the current crop and measure again, for example when the next item starts. */
    public void reset() {
        scan.reset();
        copies = 0;
        attempts = 0;
        onChange.run();
        if (running && !copying) schedule(QUICK_INTERVAL_MS);
    }

    public float topFraction() {
        return scan.topFraction();
    }

    public float bottomFraction() {
        return scan.bottomFraction();
    }

    private void schedule(long delay) {
        mainHandler.removeCallbacks(sample);
        mainHandler.postDelayed(sample, delay);
    }

    private void sample() {
        if (!running || copying || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        SurfaceView view = (SurfaceView) surface;
        int width = view.getWidth();
        int height = view.getHeight();
        String key = playback.itemKey();
        int[] known = key != null ? KNOWN_CROPS.get(key) : null;
        if (known != null) {
            scan.restore(known);
            Timber.i("Reusing the encoded bars measured earlier for this video");
            onChange.run();
            return;
        }
        if (!playback.isPlaying() || width <= 0 || height <= 0 || !view.getHolder().getSurface().isValid()) {
            schedule(QUICK_INTERVAL_MS);
            return;
        }
        copying = true;
        copyHandler.post(() -> request(view));
    }

    /** Runs on the copy thread. */
    private void request(SurfaceView view) {
        copyStarted = SystemClock.elapsedRealtime();
        try {
            // The whole frame: a source rectangle is in video buffer pixels, not view pixels, and would crop the bars.
            PixelCopy.request(view, bitmap, this::onCopied, copyHandler);
        } catch (IllegalArgumentException e) {
            mainHandler.post(() -> finish(false, 0));
        }
    }

    /** Runs on the copy thread. */
    private void onCopied(int result) {
        long cost = SystemClock.elapsedRealtime() - copyStarted;
        boolean copied = result == PixelCopy.SUCCESS;
        if (copied) {
            bitmap.getPixels(pixels, 0, SAMPLE_WIDTH, 0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT);
            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                luma[i] = (((p >> 16) & 0xFF) * 77 + ((p >> 8) & 0xFF) * 150 + (p & 0xFF) * 29) >> 8;
            }
        }
        mainHandler.post(() -> finish(copied, cost));
    }

    private void finish(boolean copied, long cost) {
        // luma is only rewritten by the next copy, which waits for this flag.
        copying = false;
        if (!running) return;
        attempts++;
        if (cost > SLOW_COPY_MS) slowDevice = true;
        if (!copied) Timber.i("Encoded bar copy failed");
        if (copied) {
            copies++;
            if (scan.update(luma, SAMPLE_WIDTH, SAMPLE_HEIGHT)) onChange.run();
            if (scan.measured()) {
                String key = playback.itemKey();
                if (key != null) KNOWN_CROPS.put(key, scan.snapshot());
                Timber.i("Encoded bars measured after %d copies (last took %d ms): picture %.3f-%.3f",
                        copies, cost, scan.topFraction(), scan.bottomFraction());
                return;
            }
        }
        if (slowDevice && attempts >= SLOW_DEVICE_COPIES) {
            Timber.i("Encoded bar copies take %d ms here; stopped after %d attempts without a crop", cost, attempts);
            return;
        }
        schedule(copies < QUICK_COPIES ? QUICK_INTERVAL_MS : SLOW_INTERVAL_MS);
    }

    public void release() {
        // The bitmap is not recycled: a copy may still be writing into it.
        stop();
        thread.quitSafely();
    }
}
