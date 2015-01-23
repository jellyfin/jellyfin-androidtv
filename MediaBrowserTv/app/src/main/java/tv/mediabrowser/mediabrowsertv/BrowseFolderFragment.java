package tv.mediabrowser.mediabrowsertv;

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

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 12/4/2014.
 */
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
    public void onActivityCreated(Bundle savedInstanceState) {
        mFolder = TvApp.getApplication().getSerializer().DeserializeFromString(getActivity().getIntent().getStringExtra("Folder"),BaseItemDto.class);
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
            }
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);
        if (showViews) {
            HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), "Views", null);

            GridButtonPresenter mGridPresenter = new GridButtonPresenter();
            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
            gridRowAdapter.add(new GridButton(BY_LETTER, "By Letter", R.drawable.byletter));
            if (itemTypeString != null && itemTypeString.equals("Movie"))
                gridRowAdapter.add(new GridButton(SUGGESTED, "Suggested", R.drawable.suggestions));
            gridRowAdapter.add(new GridButton(GENRES, "Genres", R.drawable.genres));
            if (itemTypeString != null && itemTypeString.equals("Movie"))
                gridRowAdapter.add(new GridButton(YEARS, "Years", R.drawable.years));
            gridRowAdapter.add(new GridButton(PERSONS, "Performers", R.drawable.actors));
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
                        intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        intent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(intent);
                        break;

                    case GENRES:
                        Intent genreIntent = new Intent(getActivity(), ByGenreActivity.class);
                        genreIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        genreIntent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(genreIntent);
                        break;

                    case SUGGESTED:
                        Intent suggIntent = new Intent(getActivity(), SuggestedMoviesActivity.class);
                        suggIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        suggIntent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(suggIntent);
                        break;

                    case PERSONS:
                        Intent personIntent = new Intent(getActivity(), BrowsePersonsActivity.class);
                        personIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(mFolder));
                        personIntent.putExtra("IncludeType", itemTypeString);

                        getActivity().startActivity(personIntent);
                        break;

                    default:
                        Toast.makeText(getActivity(), item.toString() + " not implemented", Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        }
    }
}
