package tv.emby.embyatv.search;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

import tv.emby.embyatv.R;

public class SearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            setContentView(R.layout.activity_search);
        } else {
            setContentView(R.layout.activity_search_no_speech);
        }
    }
}
