package org.jellyfin.androidtv.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;

import org.jellyfin.androidtv.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

        if(hr > 0) {
            return String.format(DURATION_TIME_FORMAT_WITH_HOURS, hr, min, sec);
        } else {
            return String.format(DURATION_TIME_FORMAT_NO_HOURS, min, sec);
        }
    }

    public static String formatSeconds(Context context, int seconds) {
        // Seconds
        if (seconds < SECS_PER_MIN) {
            return seconds + " " + context.getString(R.string.lbl_seconds);
        }

        StringBuilder builder = new StringBuilder();
        // Minutes
        if (seconds < SECS_PER_HR) {
            builder.append(seconds / SECS_PER_MIN)
                    .append(" ");
            if (seconds < 2 * SECS_PER_MIN) {
                builder.append(context.getString(R.string.lbl_minute));
            } else {
                builder.append(context.getString(R.string.lbl_minutes));
            }
            return builder.toString();
        }

        // Hours
        builder.append(seconds / SECS_PER_HR)
                .append(" ");
        if (seconds < 2 * SECS_PER_HR) {
            builder.append(context.getString(R.string.lbl_hour));
        } else {
            builder.append(context.getString(R.string.lbl_hours));
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

    public static String getFriendlyDate(Context context, Date date) {
        return getFriendlyDate(context, date, false);
    }

    public static String getFriendlyDate(Context context, Date date, boolean relative) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Calendar now = Calendar.getInstance();
        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                return context.getString(R.string.lbl_today);
            }
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) + 1) {
                return context.getString(R.string.lbl_tomorrow);
            }
            if (cal.get(Calendar.DAY_OF_YEAR) < now.get(Calendar.DAY_OF_YEAR) + 7 && cal.get(Calendar.DAY_OF_YEAR) > now.get(Calendar.DAY_OF_YEAR)) {
                return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
            }
            if (relative) {
                return context.getString(R.string.lbl_in_x_days, cal.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR));
            }
        }

        return DateFormat.getDateFormat(context).format(date);
    }
}
