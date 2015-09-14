package tv.emby.embyatv.search;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.speech.SpeechRecognizer;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

public class SearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SpeechRecognizer.isRecognitionAvailable(TvApp.getApplication())) {
            setContentView(R.layout.activity_search);
        } else {
            setContentView(R.layout.activity_search_no_speech);
        }
    }

    @Override
    public boolean onSearchRequested() {
        //re-start us
        this.recreate();
        return true;
    }
}
