package org.jellyfin.androidtv.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jellyfin.androidtv.BuildConfig;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.AudioBehavior;

import java.util.Arrays;
import java.util.Iterator;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

/**
 * A collection of utility methods, all static.
 */
public class Utils {
    /**
     * Shows a (long) toast
     *
     * @param context
     * @param msg
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a (long) toast.
     *
     * @param context
     * @param resourceId
     */
    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show();
    }

    public static int convertDpToPixel(@NonNull Context ctx, int dp) {
        return convertDpToPixel(ctx, (float) dp);
    }

    public static int convertDpToPixel(@NonNull Context ctx, float dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static boolean isTrue(Boolean value) {
        return value != null && value;
    }

    public static String getVersionString(Context context) {
        return context.getString(R.string.lbl_version) + BuildConfig.VERSION_NAME;
    }

    public static String firstToUpper(String value) {
        if (value == null || value.length() == 0) return "";
        return value.substring(0, 1).toUpperCase() + (value.length() > 1 ? value.substring(1) : "");
    }

    /**
     * A null safe version of {@code String.equalsIgnoreCase}.
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equalsIgnoreCase(str2);
    }

    public static <T> T getSafeValue(T value, T defaultValue) {
        if (value == null) return defaultValue;
        return value;
    }

    public static boolean isEmpty(String value) {
        return value == null || value.equals("");
    }

    public static boolean isNonEmpty(String value) {
        return value != null && !value.equals("");
    }

    public static String join(String separator, Iterable<String> items) {
        StringBuilder builder = new StringBuilder();

        Iterator<String> iterator = items.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next());

            if (iterator.hasNext()) {
                builder.append(separator);
            }
        }

        return builder.toString();
    }

    public static String join(String separator, String... items) {
        return join(separator, Arrays.asList(items));
    }

    public static boolean versionGreaterThanOrEqual(String firstVersion, String secondVersion) {
        try {
            String[] firstVersionComponents = firstVersion.split("[.]");
            String[] secondVersionComponents = secondVersion.split("[.]");
            int firstLength = firstVersionComponents.length;
            int secondLength = secondVersionComponents.length;
            int firstMajor = firstLength > 0 ? Integer.parseInt(firstVersionComponents[0]) : 0;
            int secondMajor = secondLength > 0 ? Integer.parseInt(secondVersionComponents[0]) : 0;
            int firstMinor = firstLength > 1 ? Integer.parseInt(firstVersionComponents[1]) : 0;
            int secondMinor = secondLength > 1 ? Integer.parseInt(secondVersionComponents[1]) : 0;
            int firstBuild = firstLength > 2 ? Integer.parseInt(firstVersionComponents[2]) : 0;
            int secondBuild = secondLength > 0 ? Integer.parseInt(secondVersionComponents[2]) : 0;
            int firstRelease = firstLength > 3 ? Integer.parseInt(firstVersionComponents[3]) : 0;
            int secondRelease = secondLength > 3 ? Integer.parseInt(secondVersionComponents[3]) : 0;

            if (firstMajor < secondMajor) return false;
            if (firstMajor == secondMajor && firstMinor < secondMinor) return false;
            if (firstMajor == secondMajor && firstMinor == secondMinor && firstBuild < secondBuild) return false;
            if (firstMajor == secondMajor && firstMinor == secondMinor && firstBuild == secondBuild && firstRelease < secondRelease) return false;

            return true;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    public static int getMaxBitrate() {
        String maxRate = get(UserPreferences.class).get(UserPreferences.Companion.getMaxBitrate());
        Long autoRate = get(AutoBitrate.class).getBitrate();
        float factor = Float.parseFloat(maxRate) * 10;
        return Math.min(autoRate != null && factor == 0 ? autoRate.intValue() : ((int) factor * 100000), 100000000);
    }

    public static PopupMenu createPopupMenu(Context context, View view, int gravity) {
        return new PopupMenu(context, view, gravity);
    }

    public static int getThemeColor(@Nullable Context context, int resourceId) {
        if (context == null) {
            return 0;
        }

        TypedArray styledAttributes = context.getTheme()
                .obtainStyledAttributes(new int[]{resourceId});
        int themeColor = styledAttributes.getColor(0, 0);
        styledAttributes.recycle();

        return themeColor;
    }

    public static boolean downMixAudio() {
        AudioManager am = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);
        if (am.isBluetoothA2dpOn()) {
            Timber.i("Downmixing audio due to wired headset");
            return true;
        }

        return (DeviceUtils.isFireTv() && !DeviceUtils.is50()) || get(UserPreferences.class).get(UserPreferences.Companion.getAudioBehaviour()) == AudioBehavior.DOWNMIX_TO_STEREO;
    }
}
