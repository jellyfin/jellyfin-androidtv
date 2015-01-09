package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.TextView;

import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.connectionmanager.ConnectionManager;
import mediabrowser.model.connect.PinCreationResult;
import mediabrowser.model.connect.PinExchangeResult;
import mediabrowser.model.connect.PinStatusResult;


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
                                    //Re-startup which should get proper connect info as signed in
                                    Intent startup = new Intent(TvApp.getApplication(), StartupActivity.class);
                                    startup.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    startActivity(startup);
                                }
                            });
                        } else {
                            Utils.showToast(TvApp.getApplication(), "Please confirm the above pin at mediabrowser.tv/pin");
                        }
                    }
                });
            }
        });

        Button cancel = (Button) findViewById(R.id.buttonSkip);
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
