package tv.emby.embyatv.presentation;

import android.media.MediaPlayer;
import android.os.Handler;

import java.util.Calendar;
import java.util.GregorianCalendar;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/24/2015.
 */
public class ThemeManager {

    private static boolean isHalloween() {
        Calendar today = new GregorianCalendar();
        return today.get(Calendar.MONTH) == 9 && (today.get(Calendar.DAY_OF_MONTH) == 30 || today.get(Calendar.DAY_OF_MONTH) == 31);
    }
    public static int getBrandColor() {
        return isHalloween() ? TvApp.getApplication().getResources().getColor(R.color.halloween_end) : Utils.getBrandColor();
    }

    public static void showWelcomeMessage() {
        if (isHalloween() && TvApp.getApplication().getCurrentActivity() != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MediaPlayer mp = MediaPlayer.create(TvApp.getApplication(), R.raw.howl);
                    mp.start();
                    TvApp.getApplication().getCurrentActivity().showMessage("Happy Halloween!", "Try some of our spooky suggestions", 10000, R.drawable.ghost, R.drawable.orange_gradient);
                }
            }, 2000);
        }
    }

    public static String[] getSpecialGenres() {
        if (isHalloween()) {
            return new String[] {"Halloween","Horror"};
        }

        return null;
    }

    public static String getSuggestionTitle() {
        return "Spooky Suggestions";
    }
}
