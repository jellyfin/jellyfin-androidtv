package tv.mediabrowser.mediabrowsertv;

import java.util.List;

import mediabrowser.model.querying.ItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseRowDef {
    private String headerText;
    private List<ItemQuery> itemQueries;

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public List<ItemQuery> getItemQueries() {
        return itemQueries;
    }

    public void setItemQueries(List<ItemQuery> itemQueries) {
        this.itemQueries = itemQueries;
    }
}
