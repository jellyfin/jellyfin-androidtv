package org.jellyfin.androidtv.ui.browsing;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.livetv.LiveTvChannelQuery;
import org.jellyfin.apiclient.model.livetv.RecommendedProgramQuery;
import org.jellyfin.apiclient.model.livetv.RecordingGroupQuery;
import org.jellyfin.apiclient.model.livetv.RecordingQuery;
import org.jellyfin.apiclient.model.livetv.SeriesTimerQuery;
import org.jellyfin.apiclient.model.livetv.TimerInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.LatestItemsQuery;
import org.jellyfin.apiclient.model.querying.NextUpQuery;
import org.jellyfin.apiclient.model.results.TimerInfoDtoResult;

import java.util.ArrayList;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;
import static org.koin.java.KoinJavaComponent.inject;

public class BrowseViewFragment extends EnhancedBrowseFragment {
    private boolean isLiveTvLibrary;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        String type = mFolder.getCollectionType() != null ? mFolder.getCollectionType().toLowerCase() : "";
        switch (type) {
            case "movies":
                itemTypeString = "Movie";

                //Resume
                StdItemQuery resumeMovies = new StdItemQuery();
                resumeMovies.setIncludeItemTypes(new String[]{"Movie"});
                resumeMovies.setRecursive(true);
                resumeMovies.setParentId(mFolder.getId());
                resumeMovies.setImageTypeLimit(1);
                resumeMovies.setLimit(50);
                resumeMovies.setCollapseBoxSetItems(false);
                resumeMovies.setEnableTotalRecordCount(false);
                resumeMovies.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
                resumeMovies.setSortBy(new String[]{ItemSortBy.DatePlayed});
                resumeMovies.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_continue_watching), resumeMovies, 0, new ChangeTriggerType[]{ChangeTriggerType.MoviePlayback}));

                //Latest
                LatestItemsQuery latestMovies = new LatestItemsQuery();
                latestMovies.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Overview,
                        ItemFields.ChildCount
                });
                latestMovies.setParentId(mFolder.getId());
                latestMovies.setLimit(50);
                latestMovies.setImageTypeLimit(1);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestMovies, new ChangeTriggerType[]{ChangeTriggerType.MoviePlayback, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery favorites = new StdItemQuery();
                favorites.setIncludeItemTypes(new String[]{"Movie"});
                favorites.setRecursive(true);
                favorites.setParentId(mFolder.getId());
                favorites.setImageTypeLimit(1);
                favorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                favorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), favorites, 60, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                //Collections
                StdItemQuery collections = new StdItemQuery();
                collections.setFields(new ItemFields[]{
                        ItemFields.ChildCount
                });
                collections.setIncludeItemTypes(new String[]{"BoxSet"});
                collections.setRecursive(true);
                collections.setImageTypeLimit(1);
                //collections.setParentId(mFolder.getId());
                collections.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_collections), collections, 60, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));

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
                nextUpQuery.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Overview,
                        ItemFields.ChildCount
                });
                mRows.add(new BrowseRowDef(mApplication.getResources().getString(R.string.lbl_next_up), nextUpQuery, new ChangeTriggerType[]{ChangeTriggerType.TvPlayback}));

                //Premieres
                if (get(UserPreferences.class).get(UserPreferences.Companion.getPremieresEnabled())) {
                    StdItemQuery newQuery = new StdItemQuery(new ItemFields[]{
                            ItemFields.DateCreated,
                            ItemFields.PrimaryImageAspectRatio,
                            ItemFields.Overview,
                            ItemFields.ChildCount
                    });
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
                    newQuery.setEnableTotalRecordCount(false);
                    newQuery.setLimit(300);
                    mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_new_premieres), newQuery, 0, true, true, new ChangeTriggerType[]{ChangeTriggerType.TvPlayback}, QueryType.Premieres));
                }

                //Latest content added
                LatestItemsQuery latestSeries = new LatestItemsQuery();
                latestSeries.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Overview,
                        ItemFields.ChildCount
                });
                latestSeries.setIncludeItemTypes(new String[]{"Episode"});
                latestSeries.setGroupItems(true);
                latestSeries.setParentId(mFolder.getId());
                latestSeries.setLimit(50);
                latestSeries.setImageTypeLimit(1);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestSeries, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery tvFavorites = new StdItemQuery();
                tvFavorites.setIncludeItemTypes(new String[]{"Series"});
                tvFavorites.setRecursive(true);
                tvFavorites.setParentId(mFolder.getId());
                tvFavorites.setImageTypeLimit(1);
                tvFavorites.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                tvFavorites.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), tvFavorites, 60, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                rowLoader.loadRows(mRows);
                break;
            case "music":

                //Latest
                LatestItemsQuery latestAlbums = new LatestItemsQuery();
                latestAlbums.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.Overview,
                        ItemFields.ChildCount
                });
                latestAlbums.setIncludeItemTypes(new String[]{"Audio"});
                latestAlbums.setGroupItems(true);
                latestAlbums.setImageTypeLimit(1);
                latestAlbums.setParentId(mFolder.getId());
                latestAlbums.setLimit(50);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest), latestAlbums, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));

                //Last Played
                StdItemQuery lastPlayed = new StdItemQuery();
                lastPlayed.setIncludeItemTypes(new String[]{"Audio"});
                lastPlayed.setRecursive(true);
                lastPlayed.setParentId(mFolder.getId());
                lastPlayed.setImageTypeLimit(1);
                lastPlayed.setFilters(new ItemFilter[]{ItemFilter.IsPlayed});
                lastPlayed.setSortBy(new String[]{ItemSortBy.DatePlayed});
                lastPlayed.setSortOrder(SortOrder.Descending);
                lastPlayed.setEnableTotalRecordCount(false);
                lastPlayed.setLimit(50);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_last_played), lastPlayed, 0, false, true, new ChangeTriggerType[]{ChangeTriggerType.MusicPlayback, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                StdItemQuery favAlbums = new StdItemQuery();
                favAlbums.setIncludeItemTypes(new String[]{"MusicAlbum", "MusicArtist"});
                favAlbums.setRecursive(true);
                favAlbums.setParentId(mFolder.getId());
                favAlbums.setImageTypeLimit(1);
                favAlbums.setFilters(new ItemFilter[]{ItemFilter.IsFavorite});
                favAlbums.setSortBy(new String[]{ItemSortBy.SortName});
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_favorites), favAlbums, 60, false, true, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                //AudioPlaylists
                StdItemQuery playlists = new StdItemQuery(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.CumulativeRunTimeTicks,
                        ItemFields.ChildCount
                });
                playlists.setIncludeItemTypes(new String[]{"Playlist"});
                playlists.setImageTypeLimit(1);
                playlists.setRecursive(true);
                playlists.setSortBy(new String[]{ItemSortBy.DateCreated});
                playlists.setSortOrder(SortOrder.Descending);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_playlists), playlists, 60, false, true, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}, QueryType.AudioPlaylists));

                rowLoader.loadRows(mRows);
                break;
            case "livetv":
                isLiveTvLibrary = true;
                showViews = true;

                //On now
                RecommendedProgramQuery onNow = new RecommendedProgramQuery();
                onNow.setIsAiring(true);
                onNow.setFields(new ItemFields[]{
                        ItemFields.Overview,
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChannelInfo,
                        ItemFields.ChildCount
                });
                onNow.setUserId(TvApp.getApplication().getCurrentUser().getId());
                onNow.setImageTypeLimit(1);
                onNow.setEnableTotalRecordCount(false);
                onNow.setLimit(150);
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_on_now), onNow));

                //Upcoming
                RecommendedProgramQuery upcomingTv = new RecommendedProgramQuery();
                upcomingTv.setUserId(TvApp.getApplication().getCurrentUser().getId());
                upcomingTv.setFields(new ItemFields[]{
                        ItemFields.Overview,
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChannelInfo,
                        ItemFields.ChildCount
                });
                upcomingTv.setIsAiring(false);
                upcomingTv.setHasAired(false);
                upcomingTv.setImageTypeLimit(1);
                upcomingTv.setEnableTotalRecordCount(false);
                upcomingTv.setLimit(150);
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

                //Latest Recordings
                RecordingQuery recordings = new RecordingQuery();
                recordings.setFields(new ItemFields[]{
                        ItemFields.Overview,
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
                recordings.setEnableImages(true);
                recordings.setLimit(40);

                //Do a straight query and then split the returned items into logical groups
                apiClient.getValue().GetLiveTvRecordingsAsync(recordings, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        final ItemsResult recordingsResponse = response;
                        final long ticks24 = 1000 * 60 * 60 * 24;

                        // Also get scheduled recordings for next 24 hours
                        final TimerQuery scheduled = new TimerQuery();
                        apiClient.getValue().GetLiveTvTimersAsync(scheduled, new Response<TimerInfoDtoResult>() {
                            @Override
                            public void onResponse(TimerInfoDtoResult response) {
                                List<BaseItemDto> nearTimers = new ArrayList<>();
                                long next24 = System.currentTimeMillis() + ticks24;
                                //Get scheduled items for next 24 hours
                                for (TimerInfoDto timer : response.getItems()) {
                                    if (TimeUtils.convertToLocalDate(timer.getStartDate()).getTime() <= next24) {
                                        BaseItemDto programInfo = timer.getProgramInfo();
                                        if (programInfo == null) {
                                            programInfo = new BaseItemDto();
                                            programInfo.setId(timer.getId());
                                            programInfo.setChannelName(timer.getChannelName());
                                            programInfo.setName(Utils.getSafeValue(timer.getName(), "Unknown"));
                                            Timber.w("No program info for timer %s.  Creating one...", programInfo.getName());
                                            programInfo.setBaseItemType(BaseItemType.Program);
                                            programInfo.setTimerId(timer.getId());
                                            programInfo.setSeriesTimerId(timer.getSeriesTimerId());
                                            programInfo.setStartDate(timer.getStartDate());
                                            programInfo.setEndDate(timer.getEndDate());
                                        }
                                        programInfo.setLocationType(LocationType.Virtual);
                                        nearTimers.add(programInfo);
                                    }
                                }

                                if (recordingsResponse.getTotalRecordCount() > 0) {
                                    List<BaseItemDto> dayItems = new ArrayList<>();
                                    List<BaseItemDto> weekItems = new ArrayList<>();

                                    long past24 = System.currentTimeMillis() - ticks24;
                                    long pastWeek = System.currentTimeMillis() - (ticks24 * 7);
                                    for (BaseItemDto item : recordingsResponse.getItems()) {
                                        if (item.getDateCreated() != null) {
                                            if (TimeUtils.convertToLocalDate(item.getDateCreated()).getTime() >= past24) {
                                                dayItems.add(item);
                                            } else if (TimeUtils.convertToLocalDate(item.getDateCreated()).getTime() >= pastWeek) {
                                                weekItems.add(item);
                                            }
                                        }
                                    }

                                    //First put all recordings in and retrieve
                                    //All Recordings
                                    RecordingQuery recordings = new RecordingQuery();
                                    recordings.setFields(new ItemFields[]{
                                            ItemFields.Overview,
                                            ItemFields.PrimaryImageAspectRatio,
                                            ItemFields.ChildCount
                                    });
                                    recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
                                    recordings.setEnableImages(true);
                                    mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_recent_recordings), recordings, 50));
                                    //All Recordings by group - will only be there for non-internal TV
                                    RecordingGroupQuery recordingGroups = new RecordingGroupQuery();
                                    recordingGroups.setUserId(TvApp.getApplication().getCurrentUser().getId());
                                    mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_all_recordings), recordingGroups));
                                    rowLoader.loadRows(mRows);

                                    //Now insert our smart rows
                                    if (weekItems.size() > 0) {
                                        ItemRowAdapter weekAdapter = new ItemRowAdapter(weekItems, mCardPresenter, mRowsAdapter, true);
                                        weekAdapter.Retrieve();
                                        ListRow weekRow = new ListRow(new HeaderItem("Past Week"), weekAdapter);
                                        mRowsAdapter.add(0, weekRow);
                                    }
                                    if (nearTimers.size() > 0) {
                                        ItemRowAdapter scheduledAdapter = new ItemRowAdapter(nearTimers, mCardPresenter, mRowsAdapter, true);
                                        scheduledAdapter.Retrieve();
                                        ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours"), scheduledAdapter);
                                        mRowsAdapter.add(0, scheduleRow);
                                    }
                                    if (dayItems.size() > 0) {
                                        ItemRowAdapter dayAdapter = new ItemRowAdapter(dayItems, mCardPresenter, mRowsAdapter, true);
                                        dayAdapter.Retrieve();
                                        ListRow dayRow = new ListRow(new HeaderItem("Past 24 Hours"), dayAdapter);
                                        mRowsAdapter.add(0, dayRow);
                                    }

                                } else {
                                    // no recordings
                                    rowLoader.loadRows(mRows);
                                    if (nearTimers.size() > 0) {
                                        ItemRowAdapter scheduledAdapter = new ItemRowAdapter(nearTimers, mCardPresenter, mRowsAdapter, true);
                                        scheduledAdapter.Retrieve();
                                        ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours"), scheduledAdapter);
                                        mRowsAdapter.add(0, scheduleRow);
                                    } else {
                                        mTitle.setText(R.string.lbl_no_recordings);

                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        Utils.showToast(mApplication, exception.getLocalizedMessage());
                    }
                });

                break;

            case "seriestimers":
                mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_series_recordings), new SeriesTimerQuery()));
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

                apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        if (response.getTotalRecordCount() > 0) {
                            for (BaseItemDto item : response.getItems()) {
                                ItemQuery rowQuery = new StdItemQuery();
                                rowQuery.setParentId(item.getId());
                                rowQuery.setUserId(userId);
                                rows.add(new BrowseRowDef(item.getName(), rowQuery, 60, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));
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

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        if (isLiveTvLibrary) {
            //Views row
            HeaderItem gridHeader = new HeaderItem(mRowsAdapter.size(), mApplication.getString(R.string.lbl_views));

            GridButtonPresenter mGridPresenter = new GridButtonPresenter();
            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
            gridRowAdapter.add(new GridButton(TvApp.LIVE_TV_RECORDINGS_OPTION_ID, TvApp.getApplication().getResources().getString(R.string.lbl_recorded_tv), R.drawable.tile_port_record));
            if (mApplication.canManageRecordings()) {
                gridRowAdapter.add(new GridButton(TvApp.LIVE_TV_SCHEDULE_OPTION_ID, TvApp.getApplication().getResources().getString(R.string.lbl_schedule), R.drawable.tile_port_time));
                gridRowAdapter.add(new GridButton(TvApp.LIVE_TV_SERIES_OPTION_ID, TvApp.getApplication().getResources().getString(R.string.lbl_series), R.drawable.tile_port_series_timer));
            }

            mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        } else {
            super.addAdditionalRows(rowAdapter);
        }
    }
}
