package org.jellyfin.androidtv.ui.home;

import android.app.Activity;
import android.content.Intent;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.preference.PreferencesActivity;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.startup.StartupActivity;

public class HomeFragmentFooterRow extends HomeFragmentRow implements OnItemViewClickedListener {
    private static final int LOGOUT = 0;
    private static final int SETTINGS = 1;

    private Activity activity;

    public HomeFragmentFooterRow(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void addToRowsAdapter(CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter) {
        HeaderItem header = new HeaderItem(rowsAdapter.size(), activity.getString(R.string.lbl_settings));
        GridButtonPresenter presenter = new GridButtonPresenter();

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        adapter.add(new GridButton(SETTINGS, activity.getString(R.string.lbl_settings), R.drawable.tile_settings));
        adapter.add(new GridButton(LOGOUT, activity.getString(R.string.lbl_logout), R.drawable.tile_logout));

        rowsAdapter.add(new ListRow(header, adapter));
    }

    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (!(item instanceof GridButton)) return;

        switch (((GridButton) item).getId()) {
            case LOGOUT:
                // Unset the current user
                TvApp.getApplication().setCurrentUser(null);

                // Open login activity
                Intent selectUserIntent = new Intent(activity, StartupActivity.class);
                selectUserIntent.putExtra(StartupActivity.HIDE_SPLASH, true);
                selectUserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); // Disallow back button
                activity.startActivity(selectUserIntent);

                activity.finish();

                break;
            case SETTINGS:
                Intent settingsIntent = new Intent(activity, PreferencesActivity.class);
                activity.startActivity(settingsIntent);
                break;
        }
    }
}
