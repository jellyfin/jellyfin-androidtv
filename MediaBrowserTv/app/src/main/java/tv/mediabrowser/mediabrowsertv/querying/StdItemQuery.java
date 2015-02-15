package tv.mediabrowser.mediabrowsertv.querying;

import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import tv.mediabrowser.mediabrowsertv.TvApp;

/**
 * Created by Eric on 12/5/2014.
 */
public class StdItemQuery extends ItemQuery {

    public StdItemQuery() {
        setUserId(TvApp.getApplication().getCurrentUser().getId());
        setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
    }
}
