package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;

import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.PersonsQuery;

public class BrowsePersonsFragment extends CustomViewFragment {

    private static String letters = TvApp.getApplication().getResources().getString(R.string.byletter_letters);

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        if (Utils.getSafeValue(mFolder.getChildCount(), 0) > 0) {
            //First add a '#' item
            PersonsQuery numbers = new PersonsQuery();
            numbers.setParentId(mFolder.getId());
            numbers.setSortBy(new String[]{ItemSortBy.SortName});
            if (includeType != null) numbers.setIncludeItemTypes(new String[]{includeType});
            numbers.setNameLessThan("A");
            numbers.setRecursive(true);
            mRows.add(new BrowseRowDef("#", numbers, 25));

            //Then all the defined letters
            for (Character letter : letters.toCharArray()) {
                PersonsQuery letterQuery = new PersonsQuery();
                letterQuery.setParentId(mFolder.getId());
                letterQuery.setSortBy(new String[]{ItemSortBy.SortName});
                if (includeType != null) letterQuery.setIncludeItemTypes(new String[]{includeType});
                letterQuery.setNameStartsWith(letter.toString());
                letterQuery.setRecursive(true);
                mRows.add(new BrowseRowDef(letter.toString(), letterQuery, 25));
            }

            if (mRows.size() < 2) setHeadersState(HEADERS_DISABLED);

            rowLoader.loadRows(mRows);

        }
        else {
            setHeadersState(HEADERS_DISABLED);
        }
    }


}
