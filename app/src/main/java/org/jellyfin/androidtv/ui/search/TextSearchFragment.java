package org.jellyfin.androidtv.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;

import androidx.fragment.app.Fragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ListRow;

public class TextSearchFragment extends Fragment implements TextWatcher, TextView.OnEditorActionListener {
    private SearchProvider searchProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchProvider = new SearchProvider(getContext(), getActivity().getIntent().getBooleanExtra("MusicOnly", false));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_text, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Add event listeners
        EditText searchBar = getActivity().findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(this);
        searchBar.setOnEditorActionListener(this);

        // Set up result fragment
        RowsSupportFragment rowsSupportFragment = (RowsSupportFragment) getChildFragmentManager().findFragmentById(R.id.results_frame);
        rowsSupportFragment.setAdapter(searchProvider.getResultsAdapter());

        // Create click listener
        rowsSupportFragment.setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof BaseRowItem)) return;

            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // No implementation
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // No implementation
    }

    @Override
    public void afterTextChanged(Editable s) {
        searchProvider.onQueryTextChange(s.toString());
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // Detect keyboard "submit" actions
        if (actionId == EditorInfo.IME_ACTION_SEARCH)
            searchProvider.onQueryTextSubmit(v.getText().toString());

        // Return "false" to automatically close keyboard
        return false;
    }
}

