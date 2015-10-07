package tv.emby.embyatv.validation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.List;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;
import tv.emby.iap.ErrorSeverity;
import tv.emby.iap.ErrorType;
import tv.emby.iap.IResultHandler;
import tv.emby.iap.IabValidator;
import tv.emby.iap.InAppProduct;
import tv.emby.iap.ResultType;

public class UnlockActivity extends Activity {

    IabValidator iabValidator;
    InAppProduct currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        final Activity activity = this;
        final Button next = (Button) findViewById(R.id.buttonNext);
        next.setEnabled(false);
        iabValidator = new IabValidator(TvApp.getApplication(), "");
        iabValidator.getAvailableProductsAsync(new IResultHandler<List<InAppProduct>>() {
            @Override
            public void onResult(List<InAppProduct> inAppProducts) {
                //spoof this for now
                for (InAppProduct product : inAppProducts) {
                    if (product.getSku().equals(InAppProduct.getCurrentUnlockSku(TvApp.getApplication().getPackageName()))) currentProduct = product;
                }
                next.setEnabled(true);
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iabValidator.purchase(activity, TvApp.getApplication().getSerializer().SerializeToString(currentProduct), new IResultHandler<ResultType>() {
                            @Override
                            public void onResult(ResultType resultType) {
                                TvApp.getApplication().getLogger().Info("Purchase of "+currentProduct.getTitle()+ (resultType == ResultType.Success ? " successful." : " failed."));
                                TvApp.getApplication().setPaid(resultType == ResultType.Success);
                            }

                            @Override
                            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                                TvApp.getApplication().getLogger().Error("Error purchasing "+currentProduct.getTitle()+". "+s);
                            }
                        });
                    }
                });

            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {

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
}
