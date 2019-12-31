package org.jellyfin.androidtv.search;

import android.os.Bundle;
import android.speech.SpeechRecognizer;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ListRow;

public class SearchFragment extends SearchSupportFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create provider
        SearchProvider searchProvider = new SearchProvider(getContext(), getActivity().getIntent().getBooleanExtra("MusicOnly", false));
        setSearchResultProvider(searchProvider);

        // Create click listener
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof BaseRowItem)) return;

            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
        });

        // Disable speech functionality
        boolean isSpeechEnabled = SpeechRecognizer.isRecognitionAvailable(getContext());
        if (!isSpeechEnabled) {
            // This function is deprecated but it still disabled the automatic speech functionality
            // when a callback is set. We don't actually implement it.
            setSpeechRecognitionCallback(() -> {
                TvApp.getApplication().getLogger().Debug("setSpeechRecognitionCallback call ignored");
            });
        }
    }
}

