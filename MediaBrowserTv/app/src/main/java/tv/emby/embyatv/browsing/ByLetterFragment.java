package tv.emby.embyatv.browsing;

import mediabrowser.model.querying.ItemSortBy;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class ByLetterFragment extends CustomViewFragment {

    private static String letters = TvApp.getApplication().getResources().getString(R.string.byletter_letters);

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        if (mFolder.getChildCount() > 0) {
            //First add a '#' item
            StdItemQuery numbers = new StdItemQuery();
            numbers.setParentId(mFolder.getId());
            numbers.setSortBy(new String[]{ItemSortBy.SortName});
            if (includeType != null) numbers.setIncludeItemTypes(new String[]{includeType});
            numbers.setNameLessThan(letters.substring(0,1));
            numbers.setRecursive(true);
            mRows.add(new BrowseRowDef("#", numbers, 40));

            //Then all the defined letters
            for (Character letter : letters.toCharArray()) {
                StdItemQuery letterQuery = new StdItemQuery();
                letterQuery.setParentId(mFolder.getId());
                letterQuery.setSortBy(new String[]{ItemSortBy.SortName});
                if (includeType != null) letterQuery.setIncludeItemTypes(new String[]{includeType});
                letterQuery.setNameStartsWith(letter.toString());
                letterQuery.setRecursive(true);
                mRows.add(new BrowseRowDef(letter.toString(), letterQuery, 40));
            }

            if (mRows.size() < 2) setHeadersState(HEADERS_DISABLED);

            rowLoader.loadRows(mRows);

        }
        else {
            setHeadersState(HEADERS_DISABLED);
        }
    }


}
