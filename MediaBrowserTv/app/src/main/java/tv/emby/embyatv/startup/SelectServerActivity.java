package tv.emby.embyatv.startup;

import android.app.Activity;
import android.os.Bundle;

import tv.emby.embyatv.R;

public class SelectServerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);
    }
}
