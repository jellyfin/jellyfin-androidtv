package org.jellyfin.androidtv.search;

import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.SpeechOrbView;

public class SearchFragment extends SearchSupportFragment {

    private boolean isSpeechEnabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isSpeechEnabled = SpeechRecognizer.isRecognitionAvailable(getContext());

        SearchProvider searchProvider = new SearchProvider(getContext(), getActivity().getIntent().getBooleanExtra("MusicOnly", false));
        setSearchResultProvider(searchProvider);
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof BaseRowItem)) return;

            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
        });

        // Disable speech functionality
        if (!isSpeechEnabled) {
            // This function is deprecated but it still disabled the automatic speech functionality
            // when a callback is set. We don't actually implement it.
            setSpeechRecognitionCallback(() -> {
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        // Hide speech orb when speech is disabled
        if (!isSpeechEnabled) {
            // Set to invisible instead of gone to keep the alignment of the input
            SpeechOrbView speechOrbView = view.findViewById(R.id.lb_search_bar_speech_orb);
            speechOrbView.setVisibility(View.INVISIBLE);
        }

        return view;
    }
}

