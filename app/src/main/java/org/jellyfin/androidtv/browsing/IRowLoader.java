package org.jellyfin.androidtv.browsing;

import java.util.List;

public interface IRowLoader {
    void loadRows(List<BrowseRowDef> rows);
}
