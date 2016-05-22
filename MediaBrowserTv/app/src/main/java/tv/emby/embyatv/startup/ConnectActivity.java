package tv.emby.embyatv.startup;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.connect.PinCreationResult;
import mediabrowser.model.connect.PinExchangeResult;
import mediabrowser.model.connect.PinStatusResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;


public class ConnectActivity extends Activity {

    PinCreationResult pinResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_connect);

        final IConnectionManager connectionManager = ((TvApp) getApplicationContext()).getConnectionManager();
        final Activity activity = this;

        connectionManager.CreatePin(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID), new Response<PinCreationResult>() {
            @Override
            public void onResponse(PinCreationResult response) {
                pinResult = response;
                TextView pin = (TextView) findViewById(R.id.textViewPin);
                pin.setText(response.getPin());
            }
        });

        Button next = (Button) findViewById(R.id.buttonNext);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectionManager.GetPinStatus(pinResult, new Response<PinStatusResult>() {
                    @Override
                    public void onResponse(PinStatusResult response) {
                        if (response.getIsConfirmed()) {
                            //Exchange and login
                            connectionManager.ExchangePin(pinResult, new Response<PinExchangeResult>() {
                                @Override
                                public void onResponse(PinExchangeResult response) {
                                    //Re-connect which should get proper connect info as signed in
                                    connectionManager.Connect(new Response<ConnectionResult>() {
                                        @Override
                                        public void onResponse(final ConnectionResult response) {
                                            TvApp.getApplication().setConnectLogin(true);
                                            TvApp.getApplication().getPrefs().edit().putString("pref_login_behavior", "0").apply();
                                            Utils.handleConnectionResponse(connectionManager, activity, response);
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            Utils.reportError(activity, "Error connecting");
                                        }
                                    });
                                }
                            });
                        } else {
                            Utils.showToast(TvApp.getApplication(), getString(R.string.msg_confirm_pin));
                        }
                    }
                });
            }
        });

        Button cancel = (Button) findViewById(R.id.buttonCancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button manual = (Button) findViewById(R.id.buttonManual);
        manual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.EnterManualServerAddress(activity);
            }
        });

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_connect, container, false);
            return rootView;
        }


    }
}
