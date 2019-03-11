package org.jellyfin.androidtv.util;

import android.os.Build;

public class DeviceUtils {
    private static final String FIRE_TV_PREFIX = "AFT";
    private static final String FIRE_STICK_MODEL = "AFTM";
    private static final String NEXUS_MODEL = "Nexus Player";
    private static final String SHIELD_MODEL = "SHIELD Android TV";

    public static boolean isFireTv() {
        return Build.MODEL.startsWith(FIRE_TV_PREFIX);
    }

    public static boolean isFireTvStick() {
        return Build.MODEL.equals(FIRE_STICK_MODEL);
    }

    public static boolean isShield() {
        return Build.MODEL.equals(SHIELD_MODEL);
    }

    public static boolean isNexus() {
        return Build.MODEL.equals(NEXUS_MODEL);
    }

    public static boolean is50() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public static boolean is60() {
        return Build.VERSION.SDK_INT >= 23;
    }
}
