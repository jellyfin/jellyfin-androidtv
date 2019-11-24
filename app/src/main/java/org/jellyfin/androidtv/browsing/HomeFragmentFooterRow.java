package org.jellyfin.androidtv.browsing;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.presentation.CardPresenter;
import org.jellyfin.androidtv.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.settings.SettingsActivity;
import org.jellyfin.androidtv.startup.SelectUserActivity;
import org.jellyfin.androidtv.ui.GridButton;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

public class HomeFragmentFooterRow extends HomeFragmentRow implements OnItemViewClickedListener {
    private static final int LOGOUT = 0;
    private static final int SETTINGS = 1;

    private Context context;

    public HomeFragmentFooterRow(Context context) {
        this.context = context;
    }

    @Override
    public void addToRowsAdapter(CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter) {
        HeaderItem header = new HeaderItem(rowsAdapter.size(), context.getString(R.string.lbl_settings));
        GridButtonPresenter presenter = new GridButtonPresenter();

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        adapter.add(new GridButton(SETTINGS, context.getString(R.string.lbl_settings), R.drawable.tile_settings));
        adapter.add(new GridButton(LOGOUT, context.getString(R.string.lbl_logout), R.drawable.tile_logout));

        rowsAdapter.add(new ListRow(header, adapter));
    }

    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (!(item instanceof GridButton)) return;

        switch (((GridButton) item).getId()) {
            case LOGOUT:
                TvApp app = TvApp.getApplication();

                if (app.getIsAutoLoginConfigured()) {
                    // Present user selection
                    app.setLoginApiClient(app.getApiClient());

                    // Open login activity
                    Intent intent = new Intent(context, SelectUserActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    context.startActivity(intent);
                }

                break;
            case SETTINGS:
                Intent intent = new Intent(context, SettingsActivity.class);
                context.startActivity(intent);
                break;
            default:
                Toast.makeText(context, item.toString(), Toast.LENGTH_SHORT)
                        .show();
                break;
        }

    }
}
