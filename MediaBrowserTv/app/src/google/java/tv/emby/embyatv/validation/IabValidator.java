package tv.emby.embyatv.validation;

import org.acra.ACRA;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.validation.billing.IabHelper;
import tv.emby.embyatv.validation.billing.IabResult;
import tv.emby.embyatv.validation.billing.Inventory;

/**
 * Created by Eric on 4/10/2015.
 */
public class IabValidator {

    public static String SKU_UNLOCK = "tv.emby.embyatv.unlock";
    private IabHelper iabHelper;

    private static final String k1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhAR9t5CAdBY+iOXd4QkZeSTttHtpQ48mLM+k2h0i54FWDLhn28CDUIDogQSZTLKBu0Qshp+i0KUjCD";
    private static final String k2 = "iiyQfQjYe0pgdpC7hk2ZzuOjy8C1Pb8GEhJYUoH7Pg/3ZnEZrV8kdNtfAu/TtvKGFkrhBCVrMQVN/TTKfZrq36IHC2HEqGAOin2CYV323ZjnSJpJQkGuOISy+I";
    private static final String k3 = "PvVi1EBf7+bfK3dqbv461xcSz0HtC5aJwDfvYS+fVE0X+7bLpbz93gPP07Il9ntKSCVYsmiv4PJ8uVfjVrFdaxEJowK89/+S1hD4AaDGLk90l7nfVKdXC7qpKu";
    private static final String k4 = "I+ZwyT8czrx8qyCvU5MQIDAQAB";


    public IabValidator() {iabHelper = new IabHelper(TvApp.getApplication(), getKey());}

    public static String getKey() { return k1+k3+k2+k4;}

    public void checkInAppPurchase() {
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    //Failed to connect to Google Play
                    TvApp.getApplication().getLogger().Info("Failed to connect to Google Play: " + result.getMessage());
                    ACRA.getErrorReporter().handleException(new Exception("Could not connect to Play Store"), false);

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
                ACRA.getErrorReporter().handleException(new Exception("Error confirming purchase"), false);
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
