package org.jellyfin.androidtv.ui.browsing;

import java.util.List;

public interface IRowLoader {
    void loadRows(List<BrowseRowDef> rows);
}
