package org.jellyfin.androidtv.util;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {
    private static final int MILLIS_PER_SEC = 1000;
    private static final int MILLIS_PER_MIN = 60 * MILLIS_PER_SEC;
    private static final int MILLIS_PER_HR = 60 * MILLIS_PER_MIN;

    private static final int SECS_PER_MIN = 60;
    private static final int SECS_PER_HR = 60 * SECS_PER_MIN;

    /**
     * Formats time in milliseconds to hh:mm:ss string format.
     *
     * @param millis
     * @return
     */
    public static String formatMillis(long millis) {
        long hr = millis / MILLIS_PER_HR;
        millis %= MILLIS_PER_HR;
        long min = millis / MILLIS_PER_MIN;
        millis %= MILLIS_PER_MIN;
        long sec = millis / MILLIS_PER_SEC;

        StringBuilder builder = new StringBuilder();
        // Hours
        if (hr > 0) {
            builder.append(hr)
                    .append(":");
        }
        // Minutes
        if (min >= 0) {
            if (min < 9 && hr > 0) {
                builder.append("0");
            }
            builder.append(min)
                    .append(":");
        }
        // Seconds
        if (sec < 10) {
            builder.append("0");
        }
        builder.append(sec);

        return builder.toString();
    }

    public static String formatSeconds(int seconds) {
        // Seconds
        if (seconds < SECS_PER_MIN) {
            return TvApp.getApplication().getString(R.string.lbl_seconds);
        }

        StringBuilder builder = new StringBuilder();
        // Minutes
        if (seconds < SECS_PER_HR) {
            builder.append(seconds / SECS_PER_MIN)
                    .append(" ");
            if (seconds < 2 * SECS_PER_MIN) {
                builder.append(TvApp.getApplication().getString(R.string.lbl_minute));
            } else {
                builder.append(TvApp.getApplication().getString(R.string.lbl_minutes));
            }
            return builder.toString();
        }

        // Hours
        builder.append(seconds / SECS_PER_HR)
                .append(" ");
        if (seconds < 2 * SECS_PER_HR) {
            builder.append(TvApp.getApplication().getString(R.string.lbl_hour));
        } else {
            builder.append(TvApp.getApplication().getString(R.string.lbl_hours));
        }
        return builder.toString();
    }

    public static Date convertToLocalDate(Date utcDate) {
        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        Date convertedDate = new Date(utcDate.getTime() + timeZone.getRawOffset());

        if (timeZone.inDaylightTime(convertedDate)) {
            Date dstDate = new Date(convertedDate.getTime() + timeZone.getDSTSavings());

            if (timeZone.inDaylightTime(dstDate)) {
                return dstDate;
            }
        }

        return convertedDate;
    }

    public static Date convertToUtcDate(Date localDate) {
        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        Date convertedDate = new Date(localDate.getTime() - timeZone.getRawOffset());

        if (timeZone.inDaylightTime(localDate)) {
            Date dstDate = new Date(convertedDate.getTime() - timeZone.getDSTSavings());

            if (timeZone.inDaylightTime(dstDate)) {
                return dstDate;
            }
        }

        return convertedDate;
    }
}
