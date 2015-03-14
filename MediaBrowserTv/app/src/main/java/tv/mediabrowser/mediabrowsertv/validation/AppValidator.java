package tv.mediabrowser.mediabrowsertv.validation;

import android.app.Activity;
import android.text.format.DateUtils;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.configuration.ServerConfiguration;
import mediabrowser.model.entities.MBRegistrationRecord;
import mediabrowser.model.registration.RegistrationInfo;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.util.Utils;
import tv.mediabrowser.mediabrowsertv.util.billing.IabHelper;
import tv.mediabrowser.mediabrowsertv.util.billing.IabResult;

/**
 * Created by Eric on 3/3/2015.
 */
public class AppValidator {

    private IabHelper iabHelper;

    public AppValidator() {
        iabHelper = new IabHelper(TvApp.getApplication(), Utils.getKey());
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
                    checkInAppPurchase();
                }
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving registration info", exception);
                checkInAppPurchase();
            }
        });

    }

    private void checkInAppPurchase() {
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    //Failed to connect to Google Play
                    TvApp.getApplication().getLogger().Info("Failed to connect to Google Play: " + result.getMessage());

                } else {
                    TvApp.getApplication().getLogger().Info("IAB Initialized");
                    dispose();
                }
            }
        });

    }

    public void dispose() {
        if (iabHelper != null) iabHelper.dispose();
    }
}
