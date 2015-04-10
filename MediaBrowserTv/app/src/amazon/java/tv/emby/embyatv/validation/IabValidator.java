package tv.emby.embyatv.validation;

import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 4/10/2015.
 */
public class IabValidator {

    public static String SKU_UNLOCK = "tv.emby.embyatv.unlock";

    private static final String k1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhAR9t5CAdBY+iOXd4QkZeSTttHtpQ48mLM+k2h0i54FWDLhn28CDUIDogQSZTLKBu0Qshp+i0KUjCD";
    private static final String k2 = "iiyQfQjYe0pgdpC7hk2ZzuOjy8C1Pb8GEhJYUoH7Pg/3ZnEZrV8kdNtfAu/TtvKGFkrhBCVrMQVN/TTKfZrq36IHC2HEqGAOin2CYV323ZjnSJpJQkGuOISy+I";
    private static final String k3 = "PvVi1EBf7+bfK3dqbv461xcSz0HtC5aJwDfvYS+fVE0X+7bLpbz93gPP07Il9ntKSCVYsmiv4PJ8uVfjVrFdaxEJowK89/+S1hD4AaDGLk90l7nfVKdXC7qpKu";
    private static final String k4 = "I+ZwyT8czrx8qyCvU5MQIDAQAB";


    public static String getKey() { return k1+k3+k2+k4;}

    public void checkInAppPurchase() {
        TvApp.getApplication().getLogger().Info("This would check the Amazon in app purchase...");
    }

}
