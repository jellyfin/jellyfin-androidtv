package org.jellyfin.androidtv.search;

import android.app.Activity;
import android.os.Bundle;
import android.speech.SpeechRecognizer;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;

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
