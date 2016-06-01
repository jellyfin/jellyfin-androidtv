package tv.emby.embyatv.startup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.apiclient.ServerInfo;
import tv.emby.embyatv.browsing.CustomBrowseFragment;
import tv.emby.embyatv.ui.GridButton;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.R;
import tv.emby.embyatv.browsing.StdBrowseFragment;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.GridButtonPresenter;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/4/2014.
 */
public class SelectUserFragment extends CustomBrowseFragment {
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int ENTER_MANUALLY = 0;
    private static final int LOGIN_CONNECT = 1;
    private static final int REPORT = 2;
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
        gridRowAdapter.add(new GridButton(LOGIN_CONNECT, mApplication.getString(R.string.lbl_login_with_connect), R.drawable.chain));
        gridRowAdapter.add(new GridButton(SWITCH_SERVER, mApplication.getString(R.string.lbl_switch_server), R.drawable.server));
        gridRowAdapter.add(new GridButton(REPORT, mApplication.getString(R.string.lbl_send_logs), R.drawable.upload));
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
                                    serverIntent.putExtra("Servers", payload.toArray(new String[payload.size()]));
                                    serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    getActivity().startActivity(serverIntent);
                            }
                        });
                        break;
                    case ENTER_MANUALLY:
                        // Manual login
                        Utils.EnterManualUser(getActivity());
                        break;
                    case LOGIN_CONNECT:
                        //Logout since we've already connected to a server
                        if (TvApp.getApplication().getApiClient() != null) TvApp.getApplication().getApiClient().Logout(new EmptyResponse());
                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                        break;

                    case REPORT:
                        Utils.reportError(getActivity(), "Send Log to Dev");
                        break;
                    default:
                        Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText(item.toString());
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}
