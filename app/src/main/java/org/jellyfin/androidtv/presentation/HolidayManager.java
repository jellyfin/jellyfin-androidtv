package org.jellyfin.androidtv.presentation;

import android.os.Handler;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class HolidayManager {

    private static boolean isEnabled() {
        return TvApp.getApplication().getUserPreferences().getSeasonalGreetingsEnabled();
    }

    private static boolean isHalloween() {
        if (!isEnabled()) {
            return false;
        }

        Calendar today = new GregorianCalendar();
        return today.get(Calendar.MONTH) == Calendar.OCTOBER &&
                (today.get(Calendar.DAY_OF_MONTH) == 30 || today.get(Calendar.DAY_OF_MONTH) == 31);
    }

    private static boolean isHolidays() {
        if (!isEnabled()) {
            return false;
        }

        Calendar today = new GregorianCalendar();
        return today.get(Calendar.MONTH) == Calendar.DECEMBER &&
                (today.get(Calendar.DAY_OF_MONTH) > 20 && today.get(Calendar.DAY_OF_MONTH) < 27);
    }

    private static void showHolidayMessage(final BaseActivity activity, final String title, final String message, final Integer icon, final Integer background) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // make sure they haven't moved away from that activity
                if (TvApp.getApplication().getCurrentActivity() == null ||
                        TvApp.getApplication().getCurrentActivity() != activity) {
                    return;
                }
                activity.showMessage(title, message, 10000, icon, background);
            }
        }, 2000);
    }

    public static void showWelcomeMessage() {
        final BaseActivity currentActivity = TvApp.getApplication().getCurrentActivity();
        if (currentActivity != null) {
            if (isHalloween()) {
                showHolidayMessage(currentActivity,
                                   currentActivity.getString(R.string.title_suggestions_halloween),
                                   currentActivity.getString(R.string.desc_suggestions_halloween),
                                   R.drawable.ic_ghost,
                                   R.drawable.orange_gradient);
            } else if (isHolidays()) {
                showHolidayMessage(currentActivity,
                                   currentActivity.getString(R.string.title_suggestions_holiday),
                                   currentActivity.getString(R.string.desc_suggestions_holiday),
                                   R.drawable.ic_snowflake,
                                   R.drawable.holiday_gradient);
            }
        }
    }

    public static String[] getSpecialGenres() {
        if (isHalloween()) {
            return new String[]{"Halloween", "Horror"};
        }
        if (isHolidays()) {
            return new String[]{"Holiday", "Holidays", "Christmas"};
        }
        return null;
    }

    public static String getSuggestionTitle() {
        int labelId = R.string.lbl_suggestions;
        if (isHalloween()) {
            labelId = R.string.lbl_suggestions_halloween;
        } else if (isHolidays()) {
            labelId = R.string.lbl_suggestions_holiday;
        }
        return TvApp.getApplication().getCurrentActivity().getString(labelId);
    }
}
