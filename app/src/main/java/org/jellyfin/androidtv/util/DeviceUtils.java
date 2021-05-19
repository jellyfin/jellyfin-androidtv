package org.jellyfin.androidtv.util;

import android.os.Build;

public class DeviceUtils {
    private static final String FIRE_TV_PREFIX = "AFT";
    private static final String FIRE_STICK_MODEL_GEN_1 = "AFTM";

    public static boolean isFireTv() {
        return Build.MODEL.startsWith(FIRE_TV_PREFIX);
    }

    public static boolean isFireTvStickGen1() {
        return Build.MODEL.equals(FIRE_STICK_MODEL_GEN_1);
    }

    public static boolean is50() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean is60() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
