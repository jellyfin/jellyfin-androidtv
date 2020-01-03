package org.jellyfin.androidtv.playback;

import androidx.leanback.widget.PlaybackSeekDataProvider;

public class CustomSeekProvider extends PlaybackSeekDataProvider {

    private final long duration;

    public CustomSeekProvider(long duration) {
        this.duration = duration;
    }

    @Override
    public long[] getSeekPositions() {
        return splitIntoParts(duration);
    }

    private long[] splitIntoParts(long whole) {
        long[] arr = new long[30];
        long split = whole / 30;
        for (int i = 0; i < arr.length; i++) {
            long position = split * (i + 1);
            arr[i] = position;
        }
        return arr;
    }
}
