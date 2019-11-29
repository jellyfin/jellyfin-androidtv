package org.jellyfin.androidtv.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.ChangeTriggerType;
import org.jellyfin.androidtv.querying.StdItemQuery;

import org.jellyfin.apiclient.model.dto.EBaseItemType;
import org.jellyfin.apiclient.model.querying.ArtistsQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;

/**
 * Created by Eric on 8/16/2015.
 */
public class BrowseGridFragment extends StdGridFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(IGridLoader gridLoader) {
        StdItemQuery query = new StdItemQuery(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
        query.setParentId(mParentId);
        if (mFolder.getEBaseItemType() == EBaseItemType.UserView || mFolder.getEBaseItemType() == EBaseItemType.CollectionFolder) {
            String type = mFolder.getCollectionType() != null ? mFolder.getCollectionType().toLowerCase() : "";
            switch (type) {
                case "movies":
                    query.setIncludeItemTypes(new String[]{"Movie"});
                    query.setRecursive(true);
                    break;
                case "tvshows":
                    query.setIncludeItemTypes(new String[]{"Series"});
                    query.setRecursive(true);
                    break;
                case "boxsets":
                    query.setIncludeItemTypes(new String[]{"BoxSet"});
                    query.setParentId(null);
                    query.setRecursive(true);
                    break;
                case "music":
                    mAllowViewSelection = false;
                    //Special queries needed for album artists
                    String includeType = getActivity().getIntent().getStringExtra("IncludeType");
                    if ("AlbumArtist".equals(includeType)) {
                        ArtistsQuery albumArtists = new ArtistsQuery();
                        albumArtists.setUserId(TvApp.getApplication().getCurrentUser().getId());
                        albumArtists.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.ItemCounts});
                        albumArtists.setParentId(mParentId);
                        mRowDef = new BrowseRowDef("", albumArtists, 150, new ChangeTriggerType[] {});
                        gridLoader.loadGrid(mRowDef);
                        return;
                    }
                    query.setIncludeItemTypes(new String[]{includeType != null ? includeType : "MusicAlbum"});
                    query.setRecursive(true);
                    break;
            }
        }

        mRowDef = new BrowseRowDef("", query, 150, false, true);

        gridLoader.loadGrid(mRowDef);
    }

}
