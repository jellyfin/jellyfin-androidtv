package org.jellyfin.androidtv.ui.browsing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.sdk.model.api.BaseItemDto;

import kotlinx.serialization.json.Json;

public class BrowseFolderFragment extends StdBrowseFragment {

    protected static final int BY_LETTER = 0;
    protected static final int GENRES = 1;
    protected static final int YEARS = 2;
    protected static final int PERSONS = 3;
    protected static final int SUGGESTED = 4;
    protected BaseItemDto mFolder;
    protected String itemTypeString;
    protected boolean showViews = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mFolder = Json.Default.decodeFromString(BaseItemDto.Companion.serializer(), getActivity().getIntent().getStringExtra(Extras.Folder));
        if (MainTitle == null) MainTitle = mFolder.getName();
        ShowBadge = false;
        if (mFolder.getCollectionType() != null) {
            switch (mFolder.getCollectionType()) {
                case "movies":
                    itemTypeString = "Movie";
                    break;
                case "tvshows":
                    itemTypeString = "Series";
                    break;
                case "folders":
                    showViews = false;
                    break;
                default:
                    showViews = false;
            }
        } else {
            showViews = false;
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);
        if (showViews) {
            HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), TvApp.getApplication().getString(R.string.lbl_views));

            GridButtonPresenter mGridPresenter = new GridButtonPresenter();
            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
            gridRowAdapter.add(new GridButton(BY_LETTER, TvApp.getApplication().getString(R.string.lbl_by_letter), R.drawable.tile_letters, null));
            if (itemTypeString != null && itemTypeString.equals("Movie"))
                gridRowAdapter.add(new GridButton(SUGGESTED, TvApp.getApplication().getString(R.string.lbl_suggested), R.drawable.tile_suggestions, null));
            gridRowAdapter.add(new GridButton(GENRES, TvApp.getApplication().getString(R.string.lbl_genres), R.drawable.tile_genres, null));
            gridRowAdapter.add(new GridButton(PERSONS, TvApp.getApplication().getString(R.string.lbl_performers), R.drawable.tile_actors, null));
            rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));
        }
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();
        if (showViews) mClickedListener.registerListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case BY_LETTER:
                        Intent intent = new Intent(getActivity(), ByLetterActivity.class);
                        intent.putExtra(Extras.Folder, Json.Default.encodeToString(BaseItemDto.Companion.serializer(), mFolder));
                        intent.putExtra(Extras.IncludeType, itemTypeString);

                        getActivity().startActivity(intent);
                        break;
                    case GENRES:
                        Intent genreIntent = new Intent(getActivity(), ByGenreActivity.class);
                        genreIntent.putExtra(Extras.Folder, Json.Default.encodeToString(BaseItemDto.Companion.serializer(), mFolder));
                        genreIntent.putExtra(Extras.IncludeType, itemTypeString);

                        getActivity().startActivity(genreIntent);
                        break;
                    case SUGGESTED:
                        Intent suggIntent = new Intent(getActivity(), SuggestedMoviesActivity.class);
                        suggIntent.putExtra(Extras.Folder, Json.Default.encodeToString(BaseItemDto.Companion.serializer(), mFolder));
                        suggIntent.putExtra(Extras.IncludeType, itemTypeString);

                        getActivity().startActivity(suggIntent);
                        break;
                    case PERSONS:
                        Intent personIntent = new Intent(getActivity(), BrowsePersonsActivity.class);
                        personIntent.putExtra(Extras.Folder, Json.Default.encodeToString(BaseItemDto.Companion.serializer(), mFolder));
                        personIntent.putExtra(Extras.IncludeType, itemTypeString);

                        getActivity().startActivity(personIntent);
                        break;
                    default:
                        Toast.makeText(getActivity(), item.toString() + TvApp.getApplication().getString(R.string.msg_not_implemented), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }
}
