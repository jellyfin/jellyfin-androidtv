package tv.emby.embyatv.validation;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.registration.RegistrationInfo;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 3/3/2015.
 */
public class AppValidator {

    private IabValidator iabValidator;

    public AppValidator() {
        iabValidator = new IabValidator();
    }

    public void validate() {
        //First we would check for feature registration as that will negate the need to check in-app purchases
        TvApp.getApplication().getApiClient().GetRegistrationInfo(TvApp.FEATURE_CODE, new Response<RegistrationInfo>() {
            @Override
            public void onResponse(RegistrationInfo response) {
                TvApp.getApplication().setRegistrationInfo(response);
                if (TvApp.getApplication().isValid()) {
                    TvApp.getApplication().getLogger().Info("Application is valid via supporter registration.");
                    if (TvApp.getApplication().isTrial()) {
                        TvApp.getApplication().getLogger().Info("Trial period expires "+ Utils.convertToLocalDate(response.getExpirationDate()));
                    }
                } else {
                    //If that fails, then we check for our in-app billing purchase
                    iabValidator.checkInAppPurchase();
                }
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving registration info", exception);
                iabValidator.checkInAppPurchase();
            }
        });

    }

}
