package org.jellyfin.androidtv.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.ChangeTriggerType;
import org.jellyfin.androidtv.querying.QueryType;
import org.jellyfin.androidtv.querying.StdItemQuery;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.RecommendedProgramQuery;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseViewFragment extends EnhancedBrowseFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        String type = mFolder.getCollectionType() != null ? mFolder.getCollectionType().toLowerCase() : "";
        switch (type)
        {
            case "movies":
                itemTypeString = "Movie";

                //Resume
                StdItemQuery resumeMovies = new StdItemQuery();
                resumeMovies.setIncludeItemTypes(new String[]{"Movie"});
                resumeMovies.setRecursive(true);
                resumeMovies.setParentId(mFolder.getId());
                resumeMovies.setImageTypeLimit(1);
                resumeMovies.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
                resumeMovies.setSortBy(new String[]{ItemSortBy.DatePlayed});
                resumeMovies.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_continue_watching), resumeMovies, 50, new ChangeTriggerType[] {ChangeTriggerType.MoviePlayback}));

                //Latest
                StdItemQuery latestMovies = new StdItemQuery();
                latestMovies.setIncludeItemTypes(new String[]{"Movie"});
                latestMovies.setRecursive(true);
                latestMovies.setParentId(mFolder.getId());
                latestMovies.setLimit(50);
                latestMovies.setImageTypeLimit(1);
                latestMovies.setCollapseBoxSetItems(false);
                if (TvApp.getApplication().getCurrentUser().getConfiguration().getHidePlayedInLatest()) latestMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latestMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
                latestMovies.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestMovies, 0, new ChangeTriggerType[] {ChangeTriggerType.MoviePlayback, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery favorites = new StdItemQuery();
                favorites.setIncludeItemTypes(new String[]{"Movie"});
                favorites.setRecursive(true);
                favorites.setParentId(mFolder.getId());
                favorites.setImageTypeLimit(1);
                favorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                favorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), favorites, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                //Collections
                StdItemQuery collections = new StdItemQuery();
                collections.setIncludeItemTypes(new String[]{"BoxSet"});
                collections.setRecursive(true);
                collections.setImageTypeLimit(1);
                collections.setParentId(mFolder.getId());
                collections.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_collections), collections, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                rowLoader.loadRows(mRows);
                break;
            case "tvshows":
                itemTypeString = "Series";

                //Next up
                NextUpQuery nextUpQuery = new NextUpQuery();
                nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
                nextUpQuery.setLimit(50);
                nextUpQuery.setParentId(mFolder.getId());
                nextUpQuery.setImageTypeLimit(1);
                nextUpQuery.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
                mRows.add(new BrowseRowDef(mApplication.getResources().getString(R.string.lbl_next_up), nextUpQuery, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}));

                //Premieres
                StdItemQuery newQuery = new StdItemQuery(new ItemFields[]{ItemFields.DateCreated, ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
                newQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
                newQuery.setIncludeItemTypes(new String[]{"Episode"});
                newQuery.setParentId(mFolder.getId());
                newQuery.setRecursive(true);
                newQuery.setIsVirtualUnaired(false);
                newQuery.setIsMissing(false);
                newQuery.setImageTypeLimit(1);
                newQuery.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                newQuery.setSortBy(new String[]{ItemSortBy.DateCreated});
                newQuery.setSortOrder(SortOrder.Descending);
                newQuery.setLimit(300);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_new_premieres), newQuery, 0, true, true, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}, QueryType.Premieres));

                //Latest content added
                StdItemQuery latestSeries = new StdItemQuery();
                latestSeries.setIncludeItemTypes(new String[]{"Series"});
                latestSeries.setRecursive(true);
                latestSeries.setParentId(mFolder.getId());
                latestSeries.setLimit(50);
                latestSeries.setImageTypeLimit(1);
                if (TvApp.getApplication().getCurrentUser().getConfiguration().getHidePlayedInLatest()) latestSeries.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latestSeries.setSortBy(new String[]{ItemSortBy.DateLastContentAdded});
                latestSeries.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestSeries, 0, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery tvFavorites = new StdItemQuery();
                tvFavorites.setIncludeItemTypes(new String[]{"Series"});
                tvFavorites.setRecursive(true);
                tvFavorites.setParentId(mFolder.getId());
                tvFavorites.setImageTypeLimit(1);
                tvFavorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                tvFavorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), tvFavorites, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                rowLoader.loadRows(mRows);
                break;
            case "music":

                //Latest
                StdItemQuery latestAlbums = new StdItemQuery();
                latestAlbums.setIncludeItemTypes(new String[]{"MusicAlbum"});
                latestAlbums.setRecursive(true);
                latestAlbums.setImageTypeLimit(1);
                latestAlbums.setParentId(mFolder.getId());
                latestAlbums.setLimit(50);
                latestAlbums.setSortBy(new String[]{ItemSortBy.DateLastContentAdded});
                latestAlbums.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestAlbums, 0, false, true, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                //Last Played
                StdItemQuery lastPlayed = new StdItemQuery();
                lastPlayed.setIncludeItemTypes(new String[]{"Audio"});
                lastPlayed.setRecursive(true);
                lastPlayed.setParentId(mFolder.getId());
                lastPlayed.setImageTypeLimit(1);
                lastPlayed.setFilters(new ItemFilter[]{ItemFilter.IsPlayed});
                lastPlayed.setSortBy(new String[]{ItemSortBy.DatePlayed});
                lastPlayed.setSortOrder(SortOrder.Descending);
                lastPlayed.setLimit(50);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_last_played), lastPlayed, 0, false, true, new ChangeTriggerType[] {ChangeTriggerType.MusicPlayback, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery favAlbums = new StdItemQuery();
                favAlbums.setIncludeItemTypes(new String[]{"MusicAlbum", "MusicArtist"});
                favAlbums.setRecursive(true);
                favAlbums.setParentId(mFolder.getId());
                favAlbums.setImageTypeLimit(1);
                favAlbums.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                favAlbums.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), favAlbums, 60, false, true, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                //AudioPlaylists
                StdItemQuery playlists = new StdItemQuery(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.CumulativeRunTimeTicks});
                playlists.setIncludeItemTypes(new String[]{"Playlist"});
                playlists.setMediaTypes(new String[] {"Audio"});
                playlists.setImageTypeLimit(1);
                playlists.setRecursive(true);
                playlists.setSortBy(new String[]{ItemSortBy.DateCreated});
                playlists.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_playlists), playlists, 60, false, true, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}, QueryType.AudioPlaylists));

                rowLoader.loadRows(mRows);
                break;
            case "livetv":
                //On now
                RecommendedProgramQuery onNow = new RecommendedProgramQuery();
                onNow.setIsAiring(true);
                onNow.setFields(new ItemFields[] {ItemFields.Overview, ItemFields.PrimaryImageAspectRatio, ItemFields.ChannelInfo});
                onNow.setUserId(TvApp.getApplication().getCurrentUser().getId());
                onNow.setImageTypeLimit(1);
                onNow.setLimit(200);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_on_now), onNow));

                //Upcoming
                RecommendedProgramQuery upcomingTv = new RecommendedProgramQuery();
                upcomingTv.setUserId(TvApp.getApplication().getCurrentUser().getId());
                upcomingTv.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio, ItemFields.ChannelInfo});
                upcomingTv.setIsAiring(false);
                upcomingTv.setHasAired(false);
                upcomingTv.setImageTypeLimit(1);
                upcomingTv.setLimit(200);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_coming_up), upcomingTv));

                //Latest Recordings
                RecordingQuery recordings = new RecordingQuery();
                recordings.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
                recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
                recordings.setEnableImages(true);
                recordings.setImageTypeLimit(1);
                recordings.setLimit(40);
                mRows.add(new BrowseRowDef("Latest Recordings", recordings));

                //Fav Channels
                LiveTvChannelQuery favTv = new LiveTvChannelQuery();
                favTv.setUserId(TvApp.getApplication().getCurrentUser().getId());
                favTv.setEnableFavoriteSorting(true);
                favTv.setIsFavorite(true);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorite_channels), favTv));

                //Other Channels
                LiveTvChannelQuery otherTv = new LiveTvChannelQuery();
                otherTv.setUserId(TvApp.getApplication().getCurrentUser().getId());
                otherTv.setIsFavorite(false);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_other_channels), otherTv));

                rowLoader.loadRows(mRows);

                break;
            default:
                // Fall back to rows defined by the view children
                final List<BrowseRowDef> rows = new ArrayList<>();
                final String userId = TvApp.getApplication().getCurrentUser().getId();

                ItemQuery query = new ItemQuery();
                query.setParentId(mFolder.getId());
                query.setUserId(userId);
                query.setImageTypeLimit(1);
                query.setSortBy(new String[]{ItemSortBy.SortName});

                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        if (response.getTotalRecordCount() > 0) {
                            for (BaseItemDto item : response.getItems()) {
                                ItemQuery rowQuery = new StdItemQuery();
                                rowQuery.setParentId(item.getId());
                                rowQuery.setUserId(userId);
                                rows.add(new BrowseRowDef(item.getName(), rowQuery, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));
                            }
                        }

                        rowLoader.loadRows(rows);
                    }

                    @Override
                    public void onError(Exception exception) {
                        exception.printStackTrace();
                    }
                });
                break;
        }


    }

}
