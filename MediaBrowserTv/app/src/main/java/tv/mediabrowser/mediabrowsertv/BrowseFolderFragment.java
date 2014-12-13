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

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.NextUpQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseFolderFragment extends StdBrowseFragment {

    protected BaseItemDto mFolder;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mFolder = TvApp.getApplication().getSerializer().DeserializeFromString(getActivity().getIntent().getStringExtra("Folder"),BaseItemDto.class);
        MainTitle = mFolder.getName();

        super.onActivityCreated(savedInstanceState);
    }

}
