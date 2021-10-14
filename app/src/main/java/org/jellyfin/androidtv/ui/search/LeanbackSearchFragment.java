package org.jellyfin.androidtv.ui.search;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ListRow;

import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.ui.browsing.CompositeSelectedListener;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;

import kotlin.Lazy;

public class LeanbackSearchFragment extends SearchSupportFragment {
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();

    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundService.getValue().attach(requireActivity());

        // Create provider
        SearchProvider searchProvider = new SearchProvider(getContext(), getActivity().getIntent().getBooleanExtra("MusicOnly", false));
        setSearchResultProvider(searchProvider);

        // Create click listener
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof BaseRowItem)) return;

            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), ((BaseRowItem) item).getIndex(), getActivity());
        });

        setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof BaseRowItem)) {
                backgroundService.getValue().clearBackgrounds();
            } else {
                BaseRowItem rowItem = (BaseRowItem) item;
                backgroundService.getValue().setBackground(rowItem.getSearchHint());
            }
        });
    }
}

