package org.jellyfin.androidtv.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;

import org.jellyfin.androidtv.R;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    private static final int MILLIS_PER_SEC = 1000;
    private static final long MILLIS_PER_MIN = TimeUnit.MINUTES.toMillis(1);
    private static final long MILLIS_PER_HR = TimeUnit.HOURS.toMillis(1);

    private static final int SECS_PER_MIN = 60;
    private static final long SECS_PER_HR = TimeUnit.HOURS.toSeconds(1);

    private static final String DURATION_TIME_FORMAT_NO_HOURS = "%d:%02d";
    private static final String DURATION_TIME_FORMAT_WITH_HOURS = "%d:%02d:%02d";

    public static long secondsToMillis(double seconds) {
        return Math.round(seconds * MILLIS_PER_SEC);
    }

    /**
     * Formats time in milliseconds to hh:mm:ss string format.
     *
     * @param millis Time in milliseconds
     * @return Formatted time
     */
    @SuppressLint("DefaultLocale")
    public static String formatMillis(long millis) {
        long hr = TimeUnit.MILLISECONDS.toHours(millis);
        millis %= MILLIS_PER_HR;
        long min = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis %= MILLIS_PER_MIN;
        long sec = TimeUnit.MILLISECONDS.toSeconds(millis);

        if (hr > 0) {
            return String.format(DURATION_TIME_FORMAT_WITH_HOURS, hr, min, sec);
        } else {
            return String.format(DURATION_TIME_FORMAT_NO_HOURS, min, sec);
        }
    }

    public static String formatSeconds(Context context, int seconds) {
        if (seconds < SECS_PER_MIN) {
            return ContextExtensionsKt.getQuantityString(context, R.plurals.seconds, seconds);
        } else if (seconds < SECS_PER_HR) {
            return ContextExtensionsKt.getQuantityString(context, R.plurals.minutes, seconds / SECS_PER_MIN);
        } else {
            return ContextExtensionsKt.getQuantityString(context, R.plurals.hours, seconds / SECS_PER_HR);
        }
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

    public static String getFriendlyDate(Context context, Date date) {
        return getFriendlyDate(context, date, false);
    }

    public static String getFriendlyDate(Context context, Date date, boolean relative) {
        LocalDateTime dateTime = getLocalDateTime(date);
        LocalDateTime now = LocalDateTime.now();

        if (dateTime.getYear() == now.getYear()) {
            if (dateTime.getDayOfYear() == now.getDayOfYear()) {
                return context.getString(R.string.lbl_today);
            }
            if (dateTime.getDayOfYear() == now.getDayOfYear() + 1) {
                return context.getString(R.string.lbl_tomorrow);
            }
            if (dateTime.getDayOfYear() < now.getDayOfYear() + 7 && dateTime.getDayOfYear() > now.getDayOfYear()) {
                return dateTime.format(DateTimeFormatter.ofPattern("EE", ContextExtensionsKt.getLocale(context)));
            }
            if (relative) {
                return context.getString(R.string.lbl_in_x_days, dateTime.getDayOfYear() - now.getDayOfYear());
            }
        }

        return DateFormat.getDateFormat(context).format(date);
    }

    public static Date getDate(LocalDateTime date) {
        return getDate(date, ZoneOffset.UTC);
    }

    public static Date getDate(LocalDateTime date, ZoneId zone) {
        if (date == null) return null;

        return Date.from(date.atZone(zone).toInstant());
    }
    public static LocalDateTime getLocalDateTime(Date date) {
        if (date == null) return null;

        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
