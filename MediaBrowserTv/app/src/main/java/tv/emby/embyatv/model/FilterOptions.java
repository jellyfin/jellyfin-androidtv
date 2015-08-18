package tv.emby.embyatv.model;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.model.querying.ItemFilter;

/**
 * Created by Eric on 8/18/2015.
 */
public class FilterOptions {
    private boolean favoriteOnly;
    private boolean unwatchedOnly;

    public void setFavoriteOnly(boolean value) {favoriteOnly = value;}
    public void setUnwatchedOnly(boolean value) {unwatchedOnly = value;}
    public boolean isUnwatchedOnly() {return unwatchedOnly;}
    public boolean isFavoriteOnly() {return favoriteOnly;}

    public ItemFilter[] getFilters() {
        if (!unwatchedOnly && !favoriteOnly) return null;

        List<ItemFilter> filters = new ArrayList();
        if (favoriteOnly) filters.add(ItemFilter.IsFavorite);
        if (unwatchedOnly) filters.add(ItemFilter.IsUnplayed);

        return filters.toArray(new ItemFilter[filters.size()]);
    }
}
