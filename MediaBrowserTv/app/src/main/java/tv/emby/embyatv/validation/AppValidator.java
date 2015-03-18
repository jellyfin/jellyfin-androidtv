package tv.emby.embyatv.validation;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.registration.RegistrationInfo;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.util.billing.IabHelper;
import tv.emby.embyatv.util.billing.IabResult;
import tv.emby.embyatv.util.billing.Inventory;

/**
 * Created by Eric on 3/3/2015.
 */
public class AppValidator {

    private String SKU_UNLOCK = "tv.emby.embyatv.unlock";
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
                    TvApp.getApplication().getLogger().Info("IAB Initialized.  Checking for unlock purchase...");
                    iabHelper.queryInventoryAsync(mGotInventoryListener);
                }
            }
        });

    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
                TvApp.getApplication().getLogger().Error("Unable to retrieve IAB purchase information. "+result.getMessage());
            }
            else {
                // set our indicator of paid status
                TvApp.getApplication().setPaid(inventory.hasPurchase(SKU_UNLOCK));
                TvApp.getApplication().getLogger().Info("Application unlock status is: "+TvApp.getApplication().isPaid());
            }

            // no longer need connection to Google
            dispose();
        }
    };
    public void dispose() {
        if (iabHelper != null) iabHelper.dispose();
    }
}
