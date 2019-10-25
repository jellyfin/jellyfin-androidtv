package org.jellyfin.androidtv.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void versionGreaterThanOrEqual() {
        assertTrue(Utils.versionGreaterThanOrEqual("10.0.0", "3.5.4"));
        assertTrue(Utils.versionGreaterThanOrEqual("10.0.0", "10.0.0"));
        assertFalse(Utils.versionGreaterThanOrEqual("10.0.0", "10.5.0"));
        // Invalid input for a version string should return false
        assertFalse(Utils.versionGreaterThanOrEqual("10.0.0", "Test"));
        assertFalse(Utils.versionGreaterThanOrEqual("Test", "10.0.0"));
    }
}