package tv.emby.embyatv.validation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;
import tv.emby.iap.ErrorSeverity;
import tv.emby.iap.ErrorType;
import tv.emby.iap.IResultHandler;
import tv.emby.iap.IabValidator;
import tv.emby.iap.InAppProduct;
import tv.emby.iap.PurchaseActivity;
import tv.emby.iap.ResultType;

public class UnlockActivity extends Activity {

    InAppProduct currentProduct;
    IabValidator validator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        final Activity activity = this;
        validator = new IabValidator(this, AppValidator.getKey());
        validator.validateProductsAsync(new IResultHandler<ResultType>() {
            @Override
            public void onResult(ResultType resultType) {
                Button next = (Button) findViewById(R.id.buttonNext);
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        purchase(validator.getUnlockProduct());
                    }
                });

            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                Utils.showToast(activity, s);
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

    private void purchase(InAppProduct product) {
        currentProduct = product;
        Intent purchase = new Intent(this, PurchaseActivity.class);
        purchase.putExtra("sku", currentProduct.getSku());
        purchase.putExtra("googleKey", AppValidator.getKey());
        startActivityForResult(purchase, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK:
                TvApp.getApplication().getLogger().Info("Purchase of " +currentProduct.getTitle() + " successful");
                if (data != null) TvApp.getApplication().getLogger().Info("Purchase token: " + data.getStringExtra("storeToken"));
                if (currentProduct.getSku().equals(InAppProduct.getCurrentUnlockSku(TvApp.getApplication().getPackageName()))) TvApp.getApplication().setPaid(true);
                finish();
                break;
            case RESULT_CANCELED:
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (validator != null) validator.dispose();
    }
}
