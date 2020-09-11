package org.jellyfin.androidtv.ui.playback.overlay;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomSeekProviderTest {

    @Test
    public void testGetSeekPositions_withSimpleDuration() {
        VideoPlayerAdapter videoPlayerAdapter = mock(VideoPlayerAdapter.class);
        when(videoPlayerAdapter.canSeek()).thenReturn(true);
        when(videoPlayerAdapter.getDuration()).thenReturn(90_000L);

        CustomSeekProvider customSeekProvider = new CustomSeekProvider(videoPlayerAdapter);

        long[] expected = new long[]{0L, 30_000L, 60_000L, 90_000L};
        long[] actual = customSeekProvider.getSeekPositions();

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testGetSeekPositions_withOddDuration() {
        VideoPlayerAdapter videoPlayerAdapter = mock(VideoPlayerAdapter.class);
        when(videoPlayerAdapter.canSeek()).thenReturn(true);
        when(videoPlayerAdapter.getDuration()).thenReturn(130_000L);

        CustomSeekProvider customSeekProvider = new CustomSeekProvider(videoPlayerAdapter);

        long[] expected = new long[]{0L, 30_000, 60_000, 90_000, 120_000, 130_000};
        long[] actual = customSeekProvider.getSeekPositions();

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testGetSeekPositions_withSeekDisabled() {
        VideoPlayerAdapter videoPlayerAdapter = mock(VideoPlayerAdapter.class);
        when(videoPlayerAdapter.canSeek()).thenReturn(false);

        CustomSeekProvider customSeekProvider = new CustomSeekProvider(videoPlayerAdapter);

        long[] actual = customSeekProvider.getSeekPositions();

        assertEquals(0, actual.length);
    }
}
