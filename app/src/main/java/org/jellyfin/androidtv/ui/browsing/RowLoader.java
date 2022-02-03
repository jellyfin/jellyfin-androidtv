package org.jellyfin.androidtv.ui.browsing;

import java.util.List;

public interface RowLoader {
    void loadRows(List<BrowseRowDef> rows);
}
