package org.jellyfin.androidtv.validation;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.registration.RegistrationInfo;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.playback.SubtitleHelper;
import org.jellyfin.androidtv.util.Utils;
import tv.emby.iap.ErrorSeverity;
import tv.emby.iap.ErrorType;
import tv.emby.iap.IResultHandler;
import tv.emby.iap.IabValidator;
import tv.emby.iap.InAppProduct;
import tv.emby.iap.PurchaseResult;
import tv.emby.iap.ResultType;

/**
 * Created by Eric on 3/3/2015.
 */
public class AppValidator {

    private static final String k1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhAR9t5CAdBY+iOXd4QkZeSTttHtpQ48mLM+k2h0i54FWDLhn28CDUIDogQSZTLKBu0Qshp+i0KUjCD";
    private static final String k2 = "iiyQfQjYe0pgdpC7hk2ZzuOjy8C1Pb8GEhJYUoH7Pg/3ZnEZrV8kdNtfAu/TtvKGFkrhBCVrMQVN/TTKfZrq36IHC2HEqGAOin2CYV323ZjnSJpJQkGuOISy+I";
    private static final String k3 = "PvVi1EBf7+bfK3dqbv461xcSz0HtC5aJwDfvYS+fVE0X+7bLpbz93gPP07Il9ntKSCVYsmiv4PJ8uVfjVrFdaxEJowK89/+S1hD4AaDGLk90l7nfVKdXC7qpKu";
    private static final String k4 = "I+ZwyT8czrx8qyCvU5MQIDAQAB";


    public static String getKey() { return k1+k3+k2+k4;}
    public AppValidator() {
    }

    public void validate() {
        //Load our system info
        TvApp.getApplication().loadSystemInfo();

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
                    TvApp.getApplication().premiereNag();
                    TvApp.getApplication().getLogger().Debug("*** package name: "+TvApp.getApplication().getPackageName());
                    if (!TvApp.getApplication().checkPaidCache()) checkPurchase(InAppProduct.getCurrentUnlockSku(TvApp.getApplication().getPackageName()));
                }
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving registration info", exception);
                if (!TvApp.getApplication().checkPaidCache()) checkPurchase(InAppProduct.getCurrentUnlockSku(TvApp.getApplication().getPackageName()));
            }
        });

    }

    private void checkPurchase(final String sku) {
        final IabValidator validator = new IabValidator(TvApp.getApplication(), getKey());
        validator.checkInAppPurchase(sku, new IResultHandler<ResultType>() {
            @Override
            public void onResult(ResultType result) {
                TvApp.getApplication().getLogger().Info(sku + (result == ResultType.Success ? " is purchased." : " is NOT purchased."));
                TvApp.getApplication().setPaid(result == ResultType.Success);
                validator.dispose();
            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                TvApp.getApplication().getLogger().Error("Error checking purchase " + s);
                validator.dispose();
            }
        });

    }

}
