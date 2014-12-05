package tv.mediabrowser.mediabrowsertv;

import java.util.List;

import mediabrowser.model.querying.ItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseRowDef {
    private String headerText;
    private ItemQuery query;

    public BrowseRowDef(String header, ItemQuery query) {
        headerText = header;
        this.query = query;
    }

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public ItemQuery getQuery() {
        return query;
    }

    public void setQuery(ItemQuery itemQuery) {
        this.query = itemQuery;
    }
}
