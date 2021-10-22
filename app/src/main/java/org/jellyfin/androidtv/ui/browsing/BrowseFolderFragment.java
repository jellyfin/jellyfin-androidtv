package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.sdk.model.api.BaseItemDto;

import kotlinx.serialization.json.Json;

public class BrowseFolderFragment extends StdBrowseFragment {
    protected BaseItemDto mFolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mFolder = Json.Default.decodeFromString(BaseItemDto.Companion.serializer(), getActivity().getIntent().getStringExtra(Extras.Folder));
        if (MainTitle == null) MainTitle = mFolder.getName();
        ShowBadge = false;

        super.onCreate(savedInstanceState);
    }
}
