package org.jellyfin.androidtv.util;

import java.util.Random;

public class MathUtils {
    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {
        if (max <= min) return min;

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }
}
