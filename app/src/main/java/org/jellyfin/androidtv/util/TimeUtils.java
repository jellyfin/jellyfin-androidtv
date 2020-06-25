package org.jellyfin.androidtv.util;

import android.text.format.DateFormat;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {
    private static final int MILLIS_PER_SEC = 1000;
    private static final int MILLIS_PER_MIN = 60 * MILLIS_PER_SEC;
    private static final int MILLIS_PER_HR = 60 * MILLIS_PER_MIN;

    private static final int SECS_PER_MIN = 60;
    private static final int SECS_PER_HR = 60 * SECS_PER_MIN;

    public static long secondsToMillis(double seconds) {
        return Math.round(seconds * MILLIS_PER_SEC);
    }

    public static long minutesToMillis(double minutes) {
        return Math.round(minutes * MILLIS_PER_MIN);
    }

    public static long hoursToMillis(double hours) {
        return Math.round(hours * MILLIS_PER_HR);
    }

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
            return seconds + " " + TvApp.getApplication().getString(R.string.lbl_seconds);
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

    public static int numYears(Date start, Date end) {
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(start);
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(end);
        return numYears(calStart, calEnd);
    }

    public static int numYears(Date start, Calendar end) {
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(start);
        return numYears(calStart, end);
    }

    public static int numYears(Calendar start, Calendar end) {
        int age = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        if (end.get(Calendar.MONTH) < start.get(Calendar.MONTH)) {
            age--;
        } else if (end.get(Calendar.MONTH) == start.get(Calendar.MONTH)
                && end.get(Calendar.DAY_OF_MONTH) < start.get(Calendar.DAY_OF_MONTH)) {
            age--;
        }
        return age;
    }

    public static String getFriendlyDate(Date date) {
        return getFriendlyDate(date, false);
    }

    public static String getFriendlyDate(Date date, boolean relative) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Calendar now = Calendar.getInstance();
        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                return TvApp.getApplication().getString(R.string.lbl_today);
            }
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) + 1) {
                return TvApp.getApplication().getString(R.string.lbl_tomorrow);
            }
            if (cal.get(Calendar.DAY_OF_YEAR) < now.get(Calendar.DAY_OF_YEAR) + 7 && cal.get(Calendar.DAY_OF_YEAR) > now.get(Calendar.DAY_OF_YEAR)) {
                return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
            }
            if (relative) {
                return TvApp.getApplication().getString(R.string.lbl_in_x_days, cal.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR));
            }
        }

        return DateFormat.getDateFormat(TvApp.getApplication()).format(date);
    }
}
