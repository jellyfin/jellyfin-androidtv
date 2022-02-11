package org.jellyfin.androidtv.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.AudioBehavior;
import org.koin.java.KoinJavaComponent;

import java.util.Arrays;
import java.util.Iterator;

import timber.log.Timber;

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

    public static int getMaxBitrate() {
        String maxRate = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getMaxBitrate());
        Long autoRate = KoinJavaComponent.<AutoBitrate>get(AutoBitrate.class).getBitrate();
        if (maxRate.equals(UserPreferences.MAX_BITRATE_AUTO) && autoRate != null) {
            return autoRate.intValue();
        } else {
            return (int) (Float.parseFloat(maxRate) * 1_000_000);
        }
    }

    public static int getThemeColor(@NonNull Context context, int resourceId) {
        TypedArray styledAttributes = context.getTheme()
                .obtainStyledAttributes(new int[]{resourceId});
        int themeColor = styledAttributes.getColor(0, 0);
        styledAttributes.recycle();

        return themeColor;
    }

    public static boolean downMixAudio() {
        // FIXME: Require context
        AudioManager am = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);
        if (am.isBluetoothA2dpOn()) {
            Timber.i("Downmixing audio due to wired headset");
            return true;
        }

        return KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getAudioBehaviour()) == AudioBehavior.DOWNMIX_TO_STEREO;
    }

    public static long getSafeSeekPosition(long position, long duration) {
        if (position < 0 || duration < 0)
            return 0;
        if (position >= duration)
            return Math.max(duration - 1000, 0);
        return position;
    }
}
