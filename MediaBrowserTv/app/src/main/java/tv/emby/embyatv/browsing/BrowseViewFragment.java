package tv.emby.embyatv.browsing;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.RecommendedProgramQuery;
import mediabrowser.model.livetv.RecordingGroupQuery;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.model.ChangeTriggerType;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseViewFragment extends EnhancedBrowseFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
                latestMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latestMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
                latestMovies.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestMovies, 0, new ChangeTriggerType[] {ChangeTriggerType.MoviePlayback, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery favorites = new StdItemQuery();
                favorites.setIncludeItemTypes(new String[]{"Movie"});
                favorites.setRecursive(true);
                favorites.setParentId(mFolder.getId());
                favorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                favorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), favorites, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                //Collections
                StdItemQuery collections = new StdItemQuery();
                collections.setIncludeItemTypes(new String[]{"BoxSet"});
                collections.setRecursive(true);
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
                nextUpQuery.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
                mRows.add(new BrowseRowDef(mApplication.getResources().getString(R.string.lbl_next_up), nextUpQuery, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}));

                //Latest content added
                StdItemQuery latestSeries = new StdItemQuery();
                latestSeries.setIncludeItemTypes(new String[]{"Series"});
                latestSeries.setRecursive(true);
                latestSeries.setParentId(mFolder.getId());
                latestSeries.setLimit(50);
                latestSeries.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
                latestSeries.setSortBy(new String[]{ItemSortBy.DateLastContentAdded});
                latestSeries.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestSeries, 0, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery tvFavorites = new StdItemQuery();
                tvFavorites.setIncludeItemTypes(new String[]{"Series"});
                tvFavorites.setRecursive(true);
                tvFavorites.setParentId(mFolder.getId());
                tvFavorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                tvFavorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), tvFavorites, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                rowLoader.loadRows(mRows);
                break;
            case "music":

                //Latest
                StdItemQuery latestAlbums = new StdItemQuery();
                latestAlbums.setIncludeItemTypes(new String[]{"MusicAlbum"});
                latestAlbums.setRecursive(true);
                latestAlbums.setParentId(mFolder.getId());
                latestAlbums.setLimit(50);
                latestAlbums.setSortBy(new String[]{ItemSortBy.DateLastContentAdded});
                latestAlbums.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestAlbums, 0, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                //Last Played
                StdItemQuery lastPlayed = new StdItemQuery();
                lastPlayed.setIncludeItemTypes(new String[]{"Audio"});
                lastPlayed.setRecursive(true);
                lastPlayed.setParentId(mFolder.getId());
                lastPlayed.setFilters(new ItemFilter[]{ItemFilter.IsPlayed});
                lastPlayed.setSortBy(new String[]{ItemSortBy.DatePlayed});
                lastPlayed.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_last_played), lastPlayed, 60, new ChangeTriggerType[] {ChangeTriggerType.MusicPlayed, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery favAlbums = new StdItemQuery();
                favAlbums.setIncludeItemTypes(new String[]{"MusicAlbum", "MusicArtist", "Audio"});
                favAlbums.setRecursive(true);
                favAlbums.setParentId(mFolder.getId());
                favAlbums.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                favAlbums.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), favAlbums, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                //Playlists
                StdItemQuery playlists = new StdItemQuery();
                playlists.setIncludeItemTypes(new String[]{"Playlist"});
                playlists.setRecursive(true);
                playlists.setSortBy(new String[]{ItemSortBy.DateCreated});
                playlists.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_playlists), playlists, 60, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated}));

                rowLoader.loadRows(mRows);
                break;
            case "livetv":
                //On now
                RecommendedProgramQuery onNow = new RecommendedProgramQuery();
                onNow.setIsAiring(true);
                onNow.setFields(new ItemFields[] {ItemFields.Overview, ItemFields.PrimaryImageAspectRatio, ItemFields.ChannelInfo});
                onNow.setUserId(TvApp.getApplication().getCurrentUser().getId());
                onNow.setLimit(200);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_on_now), onNow));

                //Upcoming
                RecommendedProgramQuery upcomingTv = new RecommendedProgramQuery();
                upcomingTv.setUserId(TvApp.getApplication().getCurrentUser().getId());
                upcomingTv.setFields(new ItemFields[] {ItemFields.Overview, ItemFields.PrimaryImageAspectRatio, ItemFields.ChannelInfo});
                upcomingTv.setIsAiring(false);
                upcomingTv.setHasAired(false);
                upcomingTv.setLimit(200);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_coming_up), upcomingTv));

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
