package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
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
import android.widget.TextView;

import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.connectionmanager.ConnectionManager;


public class ConnectActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_connect);

        IConnectionManager connectionManager = ((TvApp) getApplicationContext()).getConnectionManager();


        TextView pin = (TextView) findViewById(R.id.textViewPin);
        pin.setText("34545");

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
