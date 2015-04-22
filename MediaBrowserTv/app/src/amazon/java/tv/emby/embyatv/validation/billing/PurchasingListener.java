package tv.emby.embyatv.validation.billing;

import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;

import org.acra.ACRA;

import java.util.Set;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.validation.IabValidator;

/**
 * Implementation of {@link com.amazon.device.iap.PurchasingListener} that listens to Amazon
 * InAppPurchase SDK's events, and call {@link tv.emby.embyatv.validation.IabValidator} to handle the
 * purchase business logic.
 */
public class PurchasingListener implements com.amazon.device.iap.PurchasingListener {

    private final IabValidator iapManager;

    public PurchasingListener(final IabValidator iapManager) {
        this.iapManager = iapManager;
    }

    /**
     * This is the callback for {@link com.amazon.device.iap.PurchasingService#getUserData}. For
     * successful case, get the current user from {@link com.amazon.device.iap.model.UserDataResponse} and
     * call {@link tv.emby.embyatv.validation.IabValidator#setAmazonUserId} method to load the Amazon
     * user and related purchase information
     * 
     * @param response
     */
    @Override
    public void onUserDataResponse(final UserDataResponse response) {
        TvApp.getApplication().getLogger().Debug("onGetUserDataResponse: requestId (" + response.getRequestId()
                + ") userIdRequestStatus: "
                + response.getRequestStatus()
                + ")");

        final UserDataResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
        case SUCCESSFUL:
            TvApp.getApplication().getLogger().Debug("onUserDataResponse: get user id (" + response.getUserData().getUserId()
                    + ", marketplace ("
                    + response.getUserData().getMarketplace()
                    + ") ");
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            break;

        case FAILED:
        case NOT_SUPPORTED:
            TvApp.getApplication().getLogger().Debug("onUserDataResponse failed, status code is " + status);
            iapManager.setAmazonUserId(null, null);
            break;
        }
    }

    /**
     * This is the callback for {@link com.amazon.device.iap.PurchasingService#getProductData}.
     *
     * We don't actually need this because we have only one SKU that is always available
     */
    @Override
    public void onProductDataResponse(final ProductDataResponse response) {
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
        TvApp.getApplication().getLogger().Debug("onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
        case SUCCESSFUL:
            TvApp.getApplication().getLogger().Debug("onProductDataResponse: successful.  The item data map in this response includes the valid SKUs");
            final Set<String> unavailableSkus = response.getUnavailableSkus();
            TvApp.getApplication().getLogger().Debug("onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
            break;
        case FAILED:
        case NOT_SUPPORTED:
            TvApp.getApplication().getLogger().Debug("onProductDataResponse: failed, should retry request");
            iapManager.setAppUnlocked(false);
            break;
        }
    }

    /**
     * This is the callback for {@link com.amazon.device.iap.PurchasingService#getPurchaseUpdates}.
     * 
     * We will receive Consumable receipts from this callback if the consumable
     * receipts are not marked as "FULFILLED" in Amazon Appstore. So for every
     * single Consumable receipts in the response, we need to call
     * {@link tv.emby.embyatv.validation.IabValidator#handleReceipt} to fulfill the purchase.
     * 
     */
    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
        TvApp.getApplication().getLogger().Debug("onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
                + ") purchaseUpdatesResponseStatus ("
                + response.getRequestStatus()
                + ") userId ("
                + response.getUserData().getUserId()
                + ")");
        final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
        case SUCCESSFUL:
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            if (response.getReceipts().size() == 0) {
                iapManager.setAppUnlocked(false);
                TvApp.getApplication().getLogger().Info("App is NOT unlocked via purchase");
            } else {
                for (final Receipt receipt : response.getReceipts()) {
                    iapManager.handleReceipt(receipt);
                }
                if (response.hasMore()) {
                    PurchasingService.getPurchaseUpdates(true);
                }
            }
            break;
        case FAILED:
        case NOT_SUPPORTED:
            TvApp.getApplication().getLogger().Debug("onProductDataResponse: failed, should retry request");
            Utils.showToast(TvApp.getApplication(), R.string.msg_purchase_error);
            ACRA.getErrorReporter().handleException(new Exception("Error confirming purchase"), false);
            break;
        }

    }

    /**
     * This is the callback for {@link com.amazon.device.iap.PurchasingService#purchase}. For each
     * time the application sends a purchase request
     * {@link com.amazon.device.iap.PurchasingService#purchase}, Amazon Appstore will call this
     * callback when the purchase request is completed. If the RequestStatus is
     * Successful or AlreadyPurchased then application needs to call
     * {@link tv.emby.embyatv.validation.IabValidator#handleReceipt} to handle the purchase
     * fulfillment. If the RequestStatus is INVALID_SKU, NOT_SUPPORTED, or
     * FAILED, notify corresponding method of {@link tv.emby.embyatv.validation.IabValidator} .
     */
    @Override
    public void onPurchaseResponse(final PurchaseResponse response) {
        final String requestId = response.getRequestId().toString();
        final String userId = response.getUserData().getUserId();
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        TvApp.getApplication().getLogger().Debug("onPurchaseResponse: requestId (" + requestId
                + ") userId ("
                + userId
                + ") purchaseRequestStatus ("
                + status
                + ")");

        switch (status) {
        case SUCCESSFUL:
        case ALREADY_PURCHASED:
            final Receipt receipt = response.getReceipt();
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            TvApp.getApplication().getLogger().Debug("onPurchaseResponse: receipt json:" + receipt.toJSON());
            iapManager.handleReceipt(receipt);
            iapManager.purchaseComplete();
            break;
        case INVALID_SKU:
            TvApp.getApplication().getLogger().Debug(
                    "onPurchaseResponse: invalid SKU!  onProductDataResponse should have disabled buy button already.");
            iapManager.setAppUnlocked(false);
            break;
        case FAILED:
        case NOT_SUPPORTED:
            TvApp.getApplication().getLogger().Debug("onPurchaseResponse: failed so remove purchase request from local storage");
            iapManager.setAppUnlocked(false);
            Utils.showToast(TvApp.getApplication(), R.string.msg_purchase_error);
            break;
        }

    }

}
