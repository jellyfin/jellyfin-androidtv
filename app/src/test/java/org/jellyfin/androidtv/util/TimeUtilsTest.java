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
    public void formatMillis() {
        assertEquals("0:00", TimeUtils.formatMillis(0));
        assertEquals("0:13", TimeUtils.formatMillis(13000));
        assertEquals("5:00", TimeUtils.formatMillis(300000));
        assertEquals("9:01", TimeUtils.formatMillis(541000));
        assertEquals("9:59", TimeUtils.formatMillis(599000));
        assertEquals("26:00", TimeUtils.formatMillis(1560000));
        assertEquals("26:01", TimeUtils.formatMillis(1561000));
        assertEquals("26:43", TimeUtils.formatMillis(1603000));
        assertEquals("1:00:00", TimeUtils.formatMillis(3600000));
        assertEquals("1:01:01", TimeUtils.formatMillis(3661000));
        assertEquals("1:09:15", TimeUtils.formatMillis(4155000));
        assertEquals("1:16:03", TimeUtils.formatMillis(4563489));
        assertEquals("12:00:00", TimeUtils.formatMillis(43200000));
    }
}
