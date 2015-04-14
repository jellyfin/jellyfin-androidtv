package tv.emby.embyatv.validation;

import android.app.Activity;

import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;

import java.util.HashSet;
import java.util.Set;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.validation.billing.PurchasingListener;

/**
 * Created by Eric on 4/10/2015.
 */
public class IabValidator {

    public static String SKU_UNLOCK = "tv.emby.embyatv.unlock";

    private String amazonUserId;
    private String amazonMarketplace;

    private Activity purchaseActivity;

    public IabValidator() {
        PurchasingService.registerListener(TvApp.getApplication(), new PurchasingListener(this));
        final Set<String> productSkus =  new HashSet();
        productSkus.add(SKU_UNLOCK);
        PurchasingService.getProductData(productSkus);
        PurchasingService.getUserData();
    }

    public void setAmazonUserId(String id, String marketplace) {
        amazonUserId = id;
        amazonMarketplace = marketplace;
    }

    public void purchase(Activity activity) {
        purchaseActivity = activity;
        PurchasingService.purchase(SKU_UNLOCK);
    }

    public void purchaseComplete() {
        purchaseActivity.finish();
    }

    public void checkInAppPurchase() {
        TvApp.getApplication().getLogger().Info("Checking Amazon purchase...");
        PurchasingService.getPurchaseUpdates(true);
    }

    public void setAppUnlocked(boolean value) {
        TvApp.getApplication().setPaid(value);
    }

    public void handleReceipt(Receipt receipt) {
        if (receipt.isCanceled()) {
            setAppUnlocked(false);
        } else {
            if (receipt.getSku().equals(SKU_UNLOCK)) {
                setAppUnlocked(true);
                PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
                TvApp.getApplication().getLogger().Info("App is unlocked via purchase");
            } else {
                TvApp.getApplication().getLogger().Info("Invalid sku reported: "+receipt.getSku());
                PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.UNAVAILABLE);
            }
        }
    }

}
