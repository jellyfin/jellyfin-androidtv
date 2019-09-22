package org.jellyfin.androidtv.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TimeUtilsTest {

    @Test
    public void secondsToMillis() {
        assertEquals(0, TimeUtils.secondsToMillis(0));
        assertEquals(1000, TimeUtils.secondsToMillis(1));
        assertEquals(1250, TimeUtils.secondsToMillis(1.25));
    }

    @Test
    public void minutesToMillis() {
        assertEquals(0, TimeUtils.minutesToMillis(0));
        assertEquals(60000, TimeUtils.minutesToMillis(1));
        assertEquals(75000, TimeUtils.minutesToMillis(1.25));
    }

    @Test
    public void hoursToMillis() {
        assertEquals(0, TimeUtils.hoursToMillis(0));
        assertEquals(3600000, TimeUtils.hoursToMillis(1));
        assertEquals(4500000, TimeUtils.hoursToMillis(1.25));
    }

    @Test
    public void formatMillis() {
        assertEquals("0:13", TimeUtils.formatMillis(13000));
        assertEquals("5:00", TimeUtils.formatMillis(300000));
        assertEquals("1:00:00", TimeUtils.formatMillis(3600000));
        assertEquals("1:16:03", TimeUtils.formatMillis(4563489));
    }
}