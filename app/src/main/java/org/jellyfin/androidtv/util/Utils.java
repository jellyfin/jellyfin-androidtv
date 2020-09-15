package org.jellyfin.androidtv.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.jellyfin.androidtv.BuildConfig;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.AudioBehavior;
import org.jellyfin.androidtv.ui.startup.DpadPwActivity;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.serialization.GsonJsonSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

/**
 * A collection of utility methods, all static.
 */
public class Utils {
    // send the tone to the "alarm" stream (classic beeps go there) with 50% volume
    private static final ToneGenerator TONE_GENERATOR = new ToneGenerator(AudioManager.STREAM_ALARM, 50);

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

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static void beep() {
        beep(200);
    }

    public static void beep(int ms) {
        makeTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ms);
    }

    public static void makeTone(int type, int ms) {
        TONE_GENERATOR.startTone(type, ms);
    }

    public static boolean isTrue(Boolean value) {
        return value != null && value;
    }

    public static String readStringFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString(StandardCharsets.UTF_8.name());
    }

    public static String getVersionString() {
        return TvApp.getApplication().getString(R.string.lbl_version) + BuildConfig.VERSION_NAME;
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
        float factor = Float.parseFloat(maxRate) * 10;
        return Math.min(factor == 0 ? TvApp.getApplication().getAutoBitrate() : ((int) factor * 100000), 100000000);
    }

    public static PopupMenu createPopupMenu(Context context, View view, int gravity) {
        return new PopupMenu(context, view, gravity);
    }

    public static int getThemeColor(Context context, int resourceId) {
        TypedArray styledAttributes = context.getTheme()
                .obtainStyledAttributes(new int[]{resourceId});
        int themeColor = styledAttributes.getColor(0, 0);
        styledAttributes.recycle();

        return themeColor;
    }

    public static void processPasswordEntry(Activity activity, UserDto user) {
        processPasswordEntry(activity, user, null);
    }

    public static void processPasswordEntry(final Activity activity, final UserDto user, final String directItemId) {
        if (get(UserPreferences.class).get(UserPreferences.Companion.getPasswordDPadEnabled())) {
            Intent pwIntent = new Intent(activity, DpadPwActivity.class);
            pwIntent.putExtra("User", get(GsonJsonSerializer.class).SerializeToString(user));
            pwIntent.putExtra("ItemId", directItemId);
            pwIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            activity.startActivity(pwIntent);
        } else {
            Timber.d("Requesting dialog...");
            final EditText password = new EditText(activity);
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.password_prompt)
                    .setMessage(TvApp.getApplication().getString(R.string.password_prompt_message, user.getName()))
                    .setView(password)
                    .setPositiveButton(R.string.lbl_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String pw = password.getText().toString();
                            AuthenticationHelper.loginUser(user.getName(), pw, get(ApiClient.class), activity, directItemId);
                        }
                    }).show();
        }
    }

    public static boolean downMixAudio() {
        AudioManager am = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);
        if (am.isBluetoothA2dpOn()) {
            Timber.i("Downmixing audio due to wired headset");
            return true;
        }

        return (DeviceUtils.isFireTv() && !DeviceUtils.is50()) || get(UserPreferences.class).get(UserPreferences.Companion.getAudioBehaviour()) == AudioBehavior.DOWNMIX_TO_STEREO;
    }

    /**
     * Returns darker version of specified <code>color</code>.
     */
    public static int darker(int color, float factor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        return Color.argb(a,
                Math.max((int) (r * factor), 0),
                Math.max((int) (g * factor), 0),
                Math.max((int) (b * factor), 0));
    }
}
