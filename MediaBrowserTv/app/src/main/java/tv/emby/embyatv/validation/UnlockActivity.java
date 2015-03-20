package tv.emby.embyatv.validation;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.jakewharton.disklrucache.Util;

import java.util.UUID;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.util.billing.IabHelper;
import tv.emby.embyatv.util.billing.IabResult;
import tv.emby.embyatv.util.billing.Purchase;

public class UnlockActivity extends Activity {

    private IabHelper iabHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        final Activity activity = this;
        iabHelper = new IabHelper(TvApp.getApplication(), Utils.getKey());

        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    //Failed to connect to Google Play
                    TvApp.getApplication().getLogger().Info("Failed to connect to Google Play: " + result.getMessage());
                    Utils.showToast(TvApp.getApplication(), getString(R.string.msg_unable_connect_google));
                    finish();

                } else {
                    TvApp.getApplication().getLogger().Info("IAB Initialized.");
                }
            }
        });

        Button next = (Button) findViewById(R.id.buttonNext);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String check = UUID.randomUUID().toString();
                iabHelper.launchPurchaseFlow(activity, AppValidator.SKU_UNLOCK, 1000, new IabHelper.OnIabPurchaseFinishedListener() {
                    @Override
                    public void onIabPurchaseFinished(IabResult result, Purchase info) {
                        if (!result.isSuccess()) {
                            TvApp.getApplication().getLogger().Error("Error unlocking app. "+result.getMessage());
                            Utils.showToast(TvApp.getApplication(), getString(R.string.msg_purchase_error));
                        } else {
                            if (info.getSku().equals(AppValidator.SKU_UNLOCK) && info.getDeveloperPayload().equals(check)) {
                                TvApp.getApplication().getLogger().Info("Application unlocked with purchase.");
                                TvApp.getApplication().setPaid(true);
                                Utils.showToast(TvApp.getApplication(), getString(R.string.msg_thank_you_unlocked));
                                activity.finish();
                            } else {
                                TvApp.getApplication().getLogger().Error("Invalid purchase. "+info.getOriginalJson());
                                Utils.showToast(TvApp.getApplication(), getString(R.string.msg_invalid_purchase));
                            }
                        }
                    }


                }, check);
            }
        });

        Button cancel = (Button) findViewById(R.id.buttonCancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (iabHelper != null) {
            iabHelper.dispose();
            TvApp.getApplication().getLogger().Info("IAB Disposed in unlock activity destroy.");
        }
    }

}
