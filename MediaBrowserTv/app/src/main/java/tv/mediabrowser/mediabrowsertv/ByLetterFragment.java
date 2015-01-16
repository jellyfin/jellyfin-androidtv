package tv.mediabrowser.mediabrowsertv;

import android.os.Bundle;

import java.util.Arrays;

import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;

/**
 * Created by Eric on 12/4/2014.
 */
public class ByLetterFragment extends BrowseFolderFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private static String letters = TvApp.getApplication().getResources().getString(R.string.byletter_letters);

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        if (mFolder.getChildCount() > 0) {
            //First add a '#' item
            StdItemQuery byName = new StdItemQuery();
            byName.setParentId(mFolder.getId());
            byName.setSortBy(new String[]{ItemSortBy.SortName});
            byName.setNameLessThan("A");
            byName.setRecursive(true);
            mRows.add(new BrowseRowDef("#", byName, 100));

            //Then all the defined letters
            for (Character letter : letters.toCharArray()) {
                byName = new StdItemQuery();
                byName.setParentId(mFolder.getId());
                byName.setSortBy(new String[]{ItemSortBy.SortName});
                byName.setNameStartsWith(letter.toString());
                byName.setRecursive(true);
                mRows.add(new BrowseRowDef(letter.toString(), byName, 100));
            }

            if (mRows.size() < 2) setHeadersState(HEADERS_DISABLED);

            rowLoader.loadRows(mRows);

        }
        else {
            setHeadersState(HEADERS_DISABLED);
        }
    }


}
