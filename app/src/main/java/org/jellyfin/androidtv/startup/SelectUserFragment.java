package org.jellyfin.androidtv.startup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.widget.Toast;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.browsing.CustomBrowseFragment;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.presentation.CardPresenter;
import org.jellyfin.androidtv.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;

import java.util.ArrayList;
import java.util.List;

import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.interaction.GsonJsonSerializer;
import org.jellyfin.apiclient.model.apiclient.ServerInfo;

public class SelectUserFragment extends CustomBrowseFragment {
    private static final int ENTER_MANUALLY = 0;
    private static final int SWITCH_SERVER = 3;
    private ServerInfo mServer;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
        mServer = serializer.DeserializeFromString(getActivity().getIntent().getStringExtra("Server"), ServerInfo.class);

        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);

        HeaderItem usersHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_select_user));
        ItemRowAdapter usersAdapter = new ItemRowAdapter(mServer, new CardPresenter(), rowAdapter);
        usersAdapter.Retrieve();
        rowAdapter.add(new ListRow(usersHeader, usersAdapter));

        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_other_options));

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new GridButton(ENTER_MANUALLY, mApplication.getString(R.string.lbl_enter_manually), R.drawable.edit));
        gridRowAdapter.add(new GridButton(SWITCH_SERVER, mApplication.getString(R.string.lbl_switch_server), R.drawable.server));
        rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();
        mClickedListener.registerListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case SWITCH_SERVER:
                        // Present server selection
                        mApplication.getConnectionManager().GetAvailableServers(new Response<ArrayList<ServerInfo>>() {
                            @Override
                            public void onResponse(ArrayList<ServerInfo> serverResponse) {
                                    Intent serverIntent = new Intent(getActivity(), SelectServerActivity.class);
                                    GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
                                    List<String> payload = new ArrayList<>();
                                    for (ServerInfo server : serverResponse) {
                                        payload.add(serializer.SerializeToString(server));
                                    }
                                    serverIntent.putExtra("Servers", payload.toArray(new String[] {}));
                                    serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    getActivity().startActivity(serverIntent);
                            }
                        });
                        break;
                    case ENTER_MANUALLY:
                        // Manual login
                        AuthenticationHelper.enterManualUser(getActivity());
                        break;
                    default:
                        Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        }
    }
}
