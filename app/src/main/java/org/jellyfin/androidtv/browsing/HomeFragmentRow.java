package org.jellyfin.androidtv.browsing;

import org.jellyfin.androidtv.presentation.CardPresenter;

import androidx.leanback.widget.ArrayObjectAdapter;

public abstract class HomeFragmentRow {
    abstract public void addToRowsAdapter(CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter);
}

