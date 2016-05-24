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
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.apiclient.ServerInfo;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.base.IMessageListener;
import tv.emby.embyatv.browsing.CustomBrowseFragment;
import tv.emby.embyatv.ui.GridButton;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.R;
import tv.emby.embyatv.browsing.StdBrowseFragment;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.GridButtonPresenter;

/**
 * Created by Eric on 12/4/2014.
 */
public class SelectServerFragment extends CustomBrowseFragment {
    private static final int ENTER_MANUALLY = 0;
    private static final int LOGIN_CONNECT = 1;
    private static final int LOGOUT_CONNECT = 2;
    private List<ServerInfo> mServers = new ArrayList<>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
        String[] passedItems = getActivity().getIntent().getStringArrayExtra("Servers");
        if (passedItems != null) {
            for (String json : passedItems) {
                mServers.add((ServerInfo) serializer.DeserializeFromString(json, ServerInfo.class));
            }
        }

        mActivity = (BaseActivity) getActivity();
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);

        HeaderItem serverHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_select_server));
        ItemRowAdapter serverAdapter = new ItemRowAdapter(mServers.toArray(new ServerInfo[mServers.size()]), new CardPresenter(), rowAdapter);
        serverAdapter.Retrieve();
        rowAdapter.add(new ListRow(serverHeader, serverAdapter));

        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_other_options));

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new GridButton(ENTER_MANUALLY, mApplication.getString(R.string.lbl_enter_manually), R.drawable.edit));
        if (TvApp.getApplication().isConnectLogin()) {
            gridRowAdapter.add(new GridButton(LOGOUT_CONNECT, mApplication.getString(R.string.lbl_logout_connect), R.drawable.unlink));
        } else {
            gridRowAdapter.add(new GridButton(LOGIN_CONNECT, mApplication.getString(R.string.lbl_login_with_connect), R.drawable.chain));

        }
        rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();
        mClickedListener.registerListener(new ItemViewClickedListener());
        if (mActivity != null) {
            mActivity.registerKeyListener(new IKeyListener() {
                @Override
                public boolean onKeyUp(int key, KeyEvent event) {
                    return KeyProcessor.HandleKey(key, mCurrentItem, mActivity);
                }
            });

            mActivity.registerMessageListener(new IMessageListener() {
                @Override
                public void onMessageReceived(CustomMessage message) {
                    switch (message) {

                        case RemoveCurrentItem:
                            ((ItemRowAdapter)mCurrentRow.getAdapter()).remove(mCurrentItem);
                            break;
                    }
                }
            });
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case ENTER_MANUALLY:
                        Utils.EnterManualServerAddress(getActivity());
                        break;
                    case LOGIN_CONNECT:
                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                        break;

                    case LOGOUT_CONNECT:
                        TvApp.getApplication().getConnectionManager().Logout(new EmptyResponse() {
                            @Override
                            public void onResponse() {
                                mApplication.setConnectLogin(false);
                                TvApp.getApplication().getPrefs().edit().putString("pref_login_behavior", "0").apply();
                                getActivity().finish();
                            }
                        });
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
