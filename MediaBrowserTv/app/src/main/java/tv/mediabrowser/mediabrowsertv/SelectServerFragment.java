package tv.mediabrowser.mediabrowsertv;

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
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 12/4/2014.
 */
public class SelectServerFragment extends StdBrowseFragment {
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
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

        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);

        setHeadersState(HEADERS_DISABLED);

        HeaderItem serverHeader = new HeaderItem(rowAdapter.size(), "Select a server", null);
        ItemRowAdapter serverAdapter = new ItemRowAdapter(mServers.toArray(new ServerInfo[mServers.size()]), new CardPresenter(), rowAdapter);
        serverAdapter.Retrieve();
        rowAdapter.add(new ListRow(serverHeader, serverAdapter));

        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), "Other options", null);

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add("Enter Manually");
        gridRowAdapter.add("Login with Connect");
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

            if (item instanceof String) {
                if (((String) item).indexOf(getString(R.string.error_fragment)) >= 0) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    intent.putExtra("Message", "Test Error");
                    startActivity(intent);
                } else if (((String) item).indexOf("Logout ") >= 0) {
                    mApiClient = TvApp.getApplication().getConnectionManager().GetApiClient(TvApp.getApplication().getCurrentUser());
                    mApiClient.Logout(new EmptyResponse() {
                        @Override
                        public void onResponse() {
                            super.onResponse();
                            Intent intent = new Intent(getActivity(), StartupActivity.class);
                            startActivity(intent);
                        }
                    });
                } else
                {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
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
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}
