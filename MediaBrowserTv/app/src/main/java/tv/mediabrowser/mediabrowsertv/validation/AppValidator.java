package tv.mediabrowser.mediabrowsertv.validation;

import android.app.Activity;
import android.text.format.DateUtils;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.configuration.ServerConfiguration;
import mediabrowser.model.entities.MBRegistrationRecord;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.util.Utils;

/**
 * Created by Eric on 3/3/2015.
 */
public class AppValidator {

    public static void validate(final Activity activity) {
        TvApp.getApplication().getLogger().Info("Supporter app");
    }
}
