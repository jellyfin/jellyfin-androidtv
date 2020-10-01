package org.jellyfin.androidtv.ui.playback.overlay;

import androidx.leanback.widget.PlaybackSeekDataProvider;

public class CustomSeekProvider extends PlaybackSeekDataProvider {

    private final long SEEK_LENGTH = 10_000;
    private final VideoPlayerAdapter videoPlayerAdapter;

    CustomSeekProvider(VideoPlayerAdapter videoPlayerAdapter) {
        this.videoPlayerAdapter = videoPlayerAdapter;
    }

    @Override
    public long[] getSeekPositions() {
        if (!videoPlayerAdapter.canSeek()) return new long[0];
        return splitIntoThirtySecondsParts(videoPlayerAdapter.getDuration());
    }

    private long[] splitIntoThirtySecondsParts(long duration) {
        int partsSize = getPositionsArraySize(duration);

        long[] positionsArray = new long[partsSize];

        for (int i = 0; i < partsSize; i++) {
            positionsArray[i] = i * SEEK_LENGTH;
        }
        positionsArray[partsSize - 1] = duration; // Add end position

        return positionsArray;
    }

    private int getPositionsArraySize(long duration) {
        return ((int) Math.ceil((double) duration / (double) SEEK_LENGTH)) + 1;
    }
}
