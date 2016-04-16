package tv.emby.embyatv.startup;

import android.app.Activity;
import android.os.Bundle;

import tv.emby.embyatv.R;
import tv.emby.embyatv.base.BaseActivity;

public class SelectServerActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);
    }
}
