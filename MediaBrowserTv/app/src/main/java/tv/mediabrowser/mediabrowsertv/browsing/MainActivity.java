package tv.mediabrowser.mediabrowsertv.browsing;

import android.app.Activity;
import android.os.Bundle;

import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.base.BaseActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }
}
