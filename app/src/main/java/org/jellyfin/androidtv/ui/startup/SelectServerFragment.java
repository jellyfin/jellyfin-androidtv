package org.jellyfin.androidtv.ui.startup;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.base.BaseActivity;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.ui.base.IKeyListener;
import org.jellyfin.androidtv.ui.base.IMessageListener;
import org.jellyfin.androidtv.ui.browsing.CustomBrowseFragment;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;
import org.jellyfin.apiclient.model.apiclient.ServerInfo;

import java.util.ArrayList;
import java.util.List;

public class SelectServerFragment extends CustomBrowseFragment {
    private static final int ENTER_MANUALLY = 0;
    private List<ServerInfo> mServers = new ArrayList<>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Don't read from extras but retrieve server list ourselves
        String[] passedItems = getActivity().getIntent().getStringArrayExtra("Servers");
        if (passedItems != null) {
            for (String json : passedItems) {
                //mServers.add(SerializerRepository.INSTANCE.getSerializer().DeserializeFromString(json, ServerInfo.class));
            }
        }

        mActivity = (BaseActivity) getActivity();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);

        HeaderItem serverHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_select_server));
        ItemRowAdapter serverAdapter = new ItemRowAdapter(mServers.toArray(new ServerInfo[0]), new CardPresenter(), rowAdapter);
        serverAdapter.Retrieve();
        rowAdapter.add(new ListRow(serverHeader, serverAdapter));

        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_other_options));

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new GridButton(ENTER_MANUALLY, mApplication.getString(R.string.lbl_enter_manually), R.drawable.tile_edit));
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
                            ((ItemRowAdapter) mCurrentRow.getAdapter()).remove(mCurrentItem);
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
                        AuthenticationHelper.enterManualServerAddress(getActivity());
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
