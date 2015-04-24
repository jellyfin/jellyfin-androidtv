package tv.emby.embyatv.validation;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.registration.RegistrationInfo;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 3/3/2015.
 */
public class AppValidator {


    public AppValidator() {
    }

    public void validate() {
        //First we would check for feature registration as that will negate the need to check in-app purchases
        TvApp.getApplication().getApiClient().GetRegistrationInfo(TvApp.FEATURE_CODE, new Response<RegistrationInfo>() {
            @Override
            public void onResponse(RegistrationInfo response) {
                TvApp.getApplication().setRegistrationInfo(response);
                if (TvApp.getApplication().isRegistered()) {
                    TvApp.getApplication().getLogger().Info("Application is valid via supporter registration.");
                } else {
                    if (TvApp.getApplication().isTrial()) {
                        TvApp.getApplication().getLogger().Info("In supporter trial. Trial period expires "+ Utils.convertToLocalDate(response.getExpirationDate()));
                    }
                    new IabValidator().checkInAppPurchase();
                }
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving registration info", exception);
                new IabValidator().checkInAppPurchase();
            }
        });

    }

}
