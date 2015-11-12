package tv.emby.embyatv.validation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.QueryStringDictionary;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.http.HttpHeaders;
import mediabrowser.apiinteraction.http.HttpRequest;
import mediabrowser.model.connect.ConnectUser;
import mediabrowser.model.registration.AppstoreRegRequest;
import mediabrowser.model.registration.RegistrationInfo;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.util.DelayedMessage;
import tv.emby.embyatv.util.Utils;
import tv.emby.iap.ErrorSeverity;
import tv.emby.iap.ErrorType;
import tv.emby.iap.IResultHandler;
import tv.emby.iap.IabValidator;
import tv.emby.iap.InAppProduct;
import tv.emby.iap.PurchaseActivity;
import tv.emby.iap.ResultType;

public class UnlockActivity extends BaseActivity {

    InAppProduct currentProduct;
    IabValidator validator;
    String email;
    static String mbAdminUrl = "https://mb3admin.com/test/admin/service/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Confirm compatibility
        if (TvApp.getApplication().getCurrentSystemInfo() == null || !Utils.versionGreaterThanOrEqual(TvApp.getApplication().getCurrentSystemInfo().getVersion(), "3.0.5768.7")) {
            new AlertDialog.Builder(this)
                    .setTitle("Incompatible Server Version")
                    .setMessage("Please update your Emby Server to be compatible with this app.")
                    .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        } else {
            setContentView(R.layout.activity_unlock);

            final Activity activity = this;
            validator = new IabValidator(this, AppValidator.getKey());
            validator.validateProductsAsync(new IResultHandler<ResultType>() {
                @Override
                public void onResult(ResultType resultType) {
                    Button next = (Button) findViewById(R.id.buttonNext);
                    if (TvApp.getApplication().isPaid()) {
                        next.setVisibility(View.GONE);
                    } else {
                        next.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                purchase(validator.getUnlockProduct());
                                new AlertDialog.Builder(activity)
                                        .setTitle(activity.getString(R.string.lbl_unlock))
                                        .setMessage(activity.getString(R.string.msg_just_unlock_confirm))
                                        .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                // Do nothing.
                                            }
                                        }).setPositiveButton(activity.getString(R.string.lbl_unlock_app), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        purchase(validator.getUnlockProduct());
                                    }
                                }).show();
                            }
                        });
                    }

                    Button monthly = (Button) findViewById(R.id.buttonMonthly);
                    final InAppProduct monthlyProduct = validator.getPremiereMonthly();
                    monthly.setText(getText(R.string.btn_monthly_prefix)+" "+monthlyProduct.getPrice()+getText(R.string.btn_monthly_suffix));
                    monthly.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // get email
                            getEmailAddress(getString(R.string.msg_email_entry1), new Response<String>() {
                                @Override
                                public void onResponse(String response) {
                                    if (!TextUtils.isEmpty(response)) {
                                        final String save = response;
                                        // confirm it
                                        getEmailAddress(getString(R.string.msg_email_entry2), new Response<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                if (response.equals(save)) {
                                                    // all good - save and purchase
                                                    email = response;
                                                    purchase(monthlyProduct);
                                                } else {
                                                    Utils.showToast(activity, getString(R.string.msg_entries_not_match));
                                                }
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                Utils.showToast(activity, getString(R.string.msg_invalid_email));
                                            }
                                        });

                                    }
                                }

                                @Override
                                public void onError(Exception exception) {
                                    Utils.showToast(activity, getString(R.string.msg_invalid_email));
                                }
                            });
                        }
                    });
                    monthly.requestFocus();

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
    }

    private void getEmailAddress(String msg, final Response<String> response) {
//        response.onResponse("ebr@ebrsoft.com");
        final EditText email = new EditText(this);
        email.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        new AlertDialog.Builder(this)
                .setTitle("Email Address")
                .setMessage(msg)
                .setView(email)
                .setPositiveButton(getString(R.string.btn_done), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = email.getText().toString();
                        if (text.contains("@") && text.contains(".")) response.onResponse(text);
                        else response.onError(new Exception("Invalid Email Address"));
                    }
                })
                .show();
    }

    private void purchase(InAppProduct product) {
        TvApp.getApplication().getLogger().Info("Starting purchase of "+product.getTitle());
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
                if (currentProduct.getSku().equals(InAppProduct.getCurrentUnlockSku(TvApp.getApplication().getPackageName()))) {
                    TvApp.getApplication().setPaid(true);
                    finish();
                } else {
                    if (currentProduct.getEmbyFeatureCode() != null) {
                        if (data == null) {
                            handleRegError("No data returned from IAB library");
                        } else {
                            // validate and register with the server
                            final ProgressDialog processing = ProgressDialog.show(this, getString(R.string.lbl_please_wait), "Processing...");
                            AppstoreRegRequest request = new AppstoreRegRequest();
                            request.setStore(data.getStringExtra("store"));
                            request.setApplication(TvApp.getApplication().getPackageName());
                            request.setProduct(currentProduct.getSku());
                            request.setFeature(currentProduct.getEmbyFeatureCode());
                            request.setType(currentProduct.getProductType().toString());
                            if (data.getStringExtra("storeId") != null) request.setStoreId(data.getStringExtra("storeId"));
                            request.setStoreToken(data.getStringExtra("storeToken"));
                            request.setEmail(email);
                            request.setAmt(currentProduct.getPrice());

//                            TvApp.getApplication().getLogger().Debug("Parms: "+TvApp.getApplication().getSerializer().SerializeToString(request));

                            TvApp.getApplication().getApiClient().RegisterAppstoreSaleAsync(request, new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    TvApp.getApplication().getLogger().Info("Successfully purchased " + currentProduct.getTitle());
                                    new AppValidator().validate();
                                    processing.dismiss();
                                    finish();

                                }

                                @Override
                                public void onError(Exception exception) {
                                    handleRegError(exception.getLocalizedMessage());
                                    processing.dismiss();
                                }
                            });

                        }
                    }
                }
                break;
            case RESULT_CANCELED:
                TvApp.getApplication().getLogger().Info("Purchase cancelled.");
                if (data != null) TvApp.getApplication().getLogger().Error(data.getStringExtra("data"));
                break;
        }

    }

    protected void handleRegError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Registration Error")
                .setMessage("There was an error registering with the Emby server.  Please contact Emby support at http://emby.media/community. "+msg)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (validator != null) validator.dispose();
    }
}
