package tv.emby.embyatv.querying;

import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 12/5/2014.
 */
public class StdItemQuery extends ItemQuery {

    public StdItemQuery(boolean includeOverview) {
        setUserId(TvApp.getApplication().getCurrentUser().getId());
        setFields(includeOverview ? new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Overview, ItemFields.ItemCounts} :
                                    new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.ItemCounts});
    }

    public StdItemQuery() {
        this(true);
    }
}
