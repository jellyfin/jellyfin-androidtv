package org.jellyfin.androidtv.playback.overlay;

import androidx.leanback.widget.PlaybackSeekDataProvider;

public class CustomSeekProvider extends PlaybackSeekDataProvider {

    private final long duration;
    private final long SEEK_LENGTH = 30_000;

    CustomSeekProvider(long duration) {
        this.duration = duration;
    }

    @Override
    public long[] getSeekPositions() {
        return splitIntoThirtySecondsParts(duration);
    }

    private long[] splitIntoThirtySecondsParts(long whole) {
        int partsSize = getPositionsArraySize(whole);

        long[] positionsArray = new long[partsSize];

        for (int i = 0; i < partsSize; i++) {
            positionsArray[i] = i * SEEK_LENGTH;
        }
        positionsArray[partsSize - 1] = whole; // Add end position

        return positionsArray;
    }

    private int getPositionsArraySize(long whole) {
        return ((int) Math.ceil((double) whole / (double) SEEK_LENGTH)) + 1;
    }
}
