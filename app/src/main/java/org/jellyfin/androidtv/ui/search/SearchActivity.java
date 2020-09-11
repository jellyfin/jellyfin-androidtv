package org.jellyfin.androidtv.ui.search;

import android.os.Bundle;
import android.speech.SpeechRecognizer;

import org.jellyfin.androidtv.ui.base.BaseActivity;

import androidx.fragment.app.Fragment;

public class SearchActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isSpeechEnabled = SpeechRecognizer.isRecognitionAvailable(this);

        // Determine fragment to use
        Fragment searchFragment = isSpeechEnabled
                ? new LeanbackSearchFragment()
                : new TextSearchFragment();

        // Add fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, searchFragment)
                .commit();
    }

    @Override
    public boolean onSearchRequested() {
        // Reset layout
        recreate();

        return true;
    }
}
