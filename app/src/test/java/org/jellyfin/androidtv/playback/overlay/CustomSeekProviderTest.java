package org.jellyfin.androidtv.playback.overlay;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class CustomSeekProviderTest {

    @Test
    public void testGetSeekPositions_withSimpleDuration() {
        CustomSeekProvider customSeekProvider = new CustomSeekProvider(90_000);

        long[] expected = new long[]{0L, 30_000L, 60_000L, 90_000L};
        long[] actual = customSeekProvider.getSeekPositions();

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testGetSeekPositions_withOddDuration() {
        CustomSeekProvider customSeekProvider = new CustomSeekProvider(130_000);

        long[] expected = new long[]{0L, 30_000, 60_000, 90_000, 120_000, 130_000};
        long[] actual = customSeekProvider.getSeekPositions();

        assertArrayEquals(expected, actual);
    }
}