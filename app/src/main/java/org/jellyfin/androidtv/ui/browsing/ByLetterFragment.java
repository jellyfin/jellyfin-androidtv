package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.querying.ItemSortBy;

public class ByLetterFragment extends BrowseFolderFragment {

    @Override
    protected void setupQueries(RowLoader rowLoader) {
        String letters = getString(R.string.byletter_letters);
        if (Utils.getSafeValue(mFolder.getChildCount(), 0) > 0) {
            //First add a '#' item
            StdItemQuery numbers = new StdItemQuery();
            numbers.setParentId(mFolder.getId().toString());
            numbers.setSortBy(new String[]{ItemSortBy.SortName});
            if (includeType != null) numbers.setIncludeItemTypes(new String[]{includeType});
            numbers.setNameLessThan(letters.substring(0, 1));
            numbers.setRecursive(true);
            mRows.add(new BrowseRowDef("#", numbers, 40));

            //Then all the defined letters
            for (Character letter : letters.toCharArray()) {
                StdItemQuery letterQuery = new StdItemQuery();
                letterQuery.setParentId(mFolder.getId().toString());
                letterQuery.setSortBy(new String[]{ItemSortBy.SortName});
                if (includeType != null) letterQuery.setIncludeItemTypes(new String[]{includeType});
                letterQuery.setNameStartsWith(letter.toString());
                letterQuery.setRecursive(true);
                mRows.add(new BrowseRowDef(letter.toString(), letterQuery, 40));
            }

            if (mRows.size() < 2) setHeadersState(HEADERS_DISABLED);

            rowLoader.loadRows(mRows);
        } else {
            setHeadersState(HEADERS_DISABLED);
        }
    }
}
