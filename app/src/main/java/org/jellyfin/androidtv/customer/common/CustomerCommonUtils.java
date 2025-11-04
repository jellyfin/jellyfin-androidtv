package org.jellyfin.androidtv.customer.common;

import static org.koin.java.KoinJavaComponent.inject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.jellyfin.androidtv.preference.UserPreferences;

import kotlin.Lazy;
import timber.log.Timber;

public class CustomerCommonUtils {
    private static volatile Handler mainThreadHandler;

    private static final Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);

    public static UserPreferences getUserPreferences() {
        return userPreferences.getValue();
    }

    public static String getSuitableSize(Number originBit, String suffix) {
        if (originBit == null) {
            return 0 + suffix;
        }

        int i = 0;
        String[] a = new String[] {
                "K", "M", "G", "T", "P"
        };
        long intPart = originBit.longValue() / 1024;
        while ((intPart / 1024) > 0) {
            i ++;
            intPart /= 1024;
        }

        return intPart + a[i] + suffix;
    }

    public static void show(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }

    public static void show(Context context, String message, int time) {
        if (context == null) {
            Timber.e("CustomerCommonUtils.show context must not null");
            return;
        }
        getMainHandler().post(() -> Toast.makeText(context, message, time).show());
    }

    public static Handler getMainHandler() {
        if (CustomerCommonUtils.mainThreadHandler == null) {
            synchronized (CustomerCommonUtils.class) {
                if (CustomerCommonUtils.mainThreadHandler == null) {
                    CustomerCommonUtils.mainThreadHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return CustomerCommonUtils.mainThreadHandler;
    }
}

