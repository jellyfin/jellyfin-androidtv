package org.jellyfin.androidtv.util;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class DeviceUtils {
    // Chromecast with Google TV
    private static final String CHROMECAST_GOOGLE_TV = "Chromecast";

    private static final String FIRE_TV_PREFIX = "AFT";
    // Fire TV Stick Models
    private static final String FIRE_STICK_MODEL_GEN_1 = "AFTM";
    private static final String FIRE_STICK_MODEL_GEN_2 = "AFTT";
    private static final String FIRE_STICK_MODEL_GEN_3 = "AFTSSS";
    private static final String FIRE_STICK_LITE_MODEL = "AFTSS";
    private static final String FIRE_STICK_4K_MODEL = "AFTMM";
    private static final String FIRE_STICK_4K_MAX_MODEL = "AFTKA";
    // Fire TV Cube Models
    private static final String FIRE_CUBE_MODEL_GEN_1 = "AFTA";
    private static final String FIRE_CUBE_MODEL_GEN_2 = "AFTR";
    // Fire TV (Box) Models
    private static final String FIRE_TV_MODEL_GEN_1 = "AFTB";
    private static final String FIRE_TV_MODEL_GEN_2 = "AFTS";
    private static final String FIRE_TV_MODEL_GEN_3 = "AFTN";
    // Nvidia Shield TV Model
    private static final String SHIELD_TV_MODEL = "SHIELD Android TV";

    private static final String UNKNOWN = "Unknown";

    @NonNull
    static String getBuildModel() {
        // Stub to allow for mock injection
        return Build.MODEL != null ? Build.MODEL : UNKNOWN;
    }

    public static boolean isChromecastWithGoogleTV() {
        return getBuildModel().equals(CHROMECAST_GOOGLE_TV);
    }

    public static boolean isFireTv() {
        return getBuildModel().startsWith(FIRE_TV_PREFIX);
    }

    public static boolean isFireTvStickGen1() {
        return getBuildModel().equals(FIRE_STICK_MODEL_GEN_1);
    }

    public static boolean isFireTvStick4k() {
        return Arrays.asList(FIRE_STICK_4K_MODEL, FIRE_STICK_4K_MAX_MODEL)
            .contains(getBuildModel());
    }

    public static boolean isShieldTv() {
        return getBuildModel().equals(SHIELD_TV_MODEL);
    }

    public static boolean has4kVideoSupport() {
        String buildModel = getBuildModel();

        return !Arrays.asList(
                // These devices only support a max video resolution of 1080p
                FIRE_STICK_MODEL_GEN_1,
                FIRE_STICK_MODEL_GEN_2,
                FIRE_STICK_MODEL_GEN_3,
                FIRE_STICK_LITE_MODEL,
                FIRE_TV_MODEL_GEN_1,
                FIRE_TV_MODEL_GEN_2
        ).contains(buildModel) && !buildModel.equals(UNKNOWN);
    }

    public static boolean is60() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
