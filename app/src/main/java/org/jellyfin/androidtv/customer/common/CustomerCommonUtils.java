package org.jellyfin.androidtv.customer.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerCommonUtils {
    private static final Logger log = LoggerFactory.getLogger(CustomerCommonUtils.class);
    private static volatile Handler mainThreadHandler;

    private static Context context;

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
            log.error("CustomerCommonUtils.show context must not null");
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

