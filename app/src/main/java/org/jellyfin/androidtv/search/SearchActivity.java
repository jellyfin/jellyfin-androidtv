package org.jellyfin.androidtv.search;

import android.os.Bundle;
import android.speech.SpeechRecognizer;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;

public class SearchActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TvApp.getApplication().isVoiceSearchAllowed() &&
                SpeechRecognizer.isRecognitionAvailable(TvApp.getApplication())) {
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
