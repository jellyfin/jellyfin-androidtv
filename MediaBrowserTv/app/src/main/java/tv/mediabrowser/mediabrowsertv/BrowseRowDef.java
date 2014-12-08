package tv.mediabrowser.mediabrowsertv;

import java.util.List;

import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.NextUpQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseRowDef {
    private String headerText;
    private ItemQuery query;
    private NextUpQuery nextUpQuery;
    private QueryType queryType;

    public BrowseRowDef(String header, ItemQuery query) {
        headerText = header;
        this.query = query;
        this.queryType = QueryType.Items;
    }

    public BrowseRowDef(String header, NextUpQuery query) {
        headerText = header;
        this.nextUpQuery = query;
        this.queryType = QueryType.NextUp;
    }

    public BrowseRowDef(String header, ViewQuery query) {
        headerText = header;
        this.queryType = QueryType.Views;
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

    public NextUpQuery getNextUpQuery() {
        return nextUpQuery;
    }

    public QueryType getQueryType() {
        return queryType;
    }
}

