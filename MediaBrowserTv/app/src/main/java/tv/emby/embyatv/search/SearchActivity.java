package tv.emby.embyatv.search;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

import tv.emby.embyatv.R;
import tv.emby.embyatv.util.Utils;

public class SearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Utils.isFireTv()) {
            setContentView(R.layout.activity_search);
        } else {
            setContentView(R.layout.activity_search_no_speech);
        }
    }
}
