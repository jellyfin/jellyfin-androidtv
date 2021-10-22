package org.jellyfin.androidtv.ui.browsing;

import android.content.Intent;
import android.os.Bundle;

import org.jellyfin.sdk.model.api.BaseItemDto;

import kotlinx.serialization.json.Json;
import timber.log.Timber;

public class BrowseFolderFragment extends StdBrowseFragment {
    protected String includeType;
    protected BaseItemDto mFolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = requireActivity().getIntent();

        mFolder = Json.Default.decodeFromString(BaseItemDto.Companion.serializer(), intent.getStringExtra(GroupedItemsActivity.EXTRA_FOLDER));
        includeType = intent.getStringExtra(GroupedItemsActivity.EXTRA_INCLUDE_TYPE);
        MainTitle = mFolder.getName();
        ShowBadge = false;

        Timber.d("Item type: %s", includeType);

        super.onCreate(savedInstanceState);
    }
}
