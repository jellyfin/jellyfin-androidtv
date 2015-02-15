package tv.mediabrowser.mediabrowsertv.startup;

import android.app.Activity;
import android.os.Bundle;

import tv.mediabrowser.mediabrowsertv.R;

public class SelectServerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);
    }
}
