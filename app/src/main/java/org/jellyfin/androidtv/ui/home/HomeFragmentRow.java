package org.jellyfin.androidtv.ui.home;

import org.jellyfin.androidtv.ui.presentation.CardPresenter;

import androidx.leanback.widget.ArrayObjectAdapter;

public abstract class HomeFragmentRow {
    abstract public void addToRowsAdapter(CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter);
}

