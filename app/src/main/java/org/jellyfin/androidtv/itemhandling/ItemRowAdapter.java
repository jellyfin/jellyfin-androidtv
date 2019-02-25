package org.jellyfin.androidtv.itemhandling;

import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.browsing.EnhancedBrowseFragment;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.model.ChangeTriggerType;
import org.jellyfin.androidtv.model.ChapterItemInfo;
import org.jellyfin.androidtv.model.FilterOptions;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.presentation.IPositionablePresenter;
import org.jellyfin.androidtv.presentation.TextItemPresenter;
import org.jellyfin.androidtv.querying.QueryType;
import org.jellyfin.androidtv.querying.SpecialsQuery;
import org.jellyfin.androidtv.querying.StdItemQuery;
import org.jellyfin.androidtv.querying.TrailersQuery;
import org.jellyfin.androidtv.querying.ViewQuery;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.HorizontalGridFragment;
import org.jellyfin.androidtv.util.Utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.RecommendedProgramQuery;
import mediabrowser.model.livetv.RecordingGroupQuery;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.livetv.SeriesTimerQuery;
import mediabrowser.model.net.HttpException;
import mediabrowser.model.querying.ArtistsQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.LatestItemsQuery;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.PersonsQuery;
import mediabrowser.model.querying.SeasonQuery;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.querying.UpcomingEpisodesQuery;
import mediabrowser.model.results.ChannelInfoDtoResult;
import mediabrowser.model.results.SeriesTimerInfoDtoResult;
import mediabrowser.model.search.SearchHint;
import mediabrowser.model.search.SearchHintResult;
import mediabrowser.model.search.SearchQuery;

/**
 * Created by Eric on 12/5/2014.
 */
public class ItemRowAdapter extends ArrayObjectAdapter {
    private ItemQuery mQuery;
    private NextUpQuery mNextUpQuery;
    private SeasonQuery mSeasonQuery;
    private UpcomingEpisodesQuery mUpcomingQuery;
    private SimilarItemsQuery mSimilarQuery;
    private PersonsQuery mPersonsQuery;
    private SearchQuery mSearchQuery;
    private SpecialsQuery mSpecialsQuery;
    private TrailersQuery mTrailersQuery;
    private LiveTvChannelQuery mTvChannelQuery;
    private RecommendedProgramQuery mTvProgramQuery;
    private RecordingQuery mTvRecordingQuery;
    private RecordingGroupQuery mTvRecordingGroupQuery;
    private ArtistsQuery mArtistsQuery;
    private LatestItemsQuery mLatestQuery;
    private SeriesTimerQuery mSeriesTimerQuery;
    private QueryType queryType;

    private String mSortBy;
    private FilterOptions mFilters;

    private EmptyResponse mRetrieveStartedListener;
    private EmptyResponse mRetrieveFinishedListener;

    private ChangeTriggerType[] reRetrieveTriggers = new ChangeTriggerType[] {};
    private Calendar lastFullRetrieve;

    private BaseItemPerson[] mPersons;
    private ServerInfo[] mServers;
    private List<ChapterItemInfo> mChapters;
    private List<BaseItemDto> mItems;
    private ServerInfo mServer;

    private ArrayObjectAdapter mParent;
    private ListRow mRow;
    private int chunkSize = 0;

    private int itemsLoaded = 0;
    private int totalItems = 0;
    private boolean fullyLoaded = false;
    private boolean currentlyRetrieving = false;

    private boolean preferParentThumb = false;
    private boolean staticHeight = false;

    public boolean isCurrentlyRetrieving() {
        synchronized (this) {
            return currentlyRetrieving;
        }
    }

    public void setCurrentlyRetrieving(boolean currentlyRetrieving) {
        synchronized (this) {
            this.currentlyRetrieving = currentlyRetrieving;
        }
    }

    public boolean getPreferParentThumb() { return preferParentThumb; }
    public boolean isStaticHeight() { return staticHeight; }
    public QueryType getQueryType() { return queryType; }

    public ArrayObjectAdapter getParent() { return mParent; }

    public void setRow(ListRow row) {
        mRow = row;
    }

    public void setReRetrieveTriggers(ChangeTriggerType[] reRetrieveTriggers) {
        this.reRetrieveTriggers = reRetrieveTriggers;
    }
    public ItemRowAdapter(ItemQuery query, int chunkSize, boolean preferParentThumb, Presenter presenter, ArrayObjectAdapter parent) {
        this(query, chunkSize, preferParentThumb, false, presenter, parent);
    }

    public ItemRowAdapter(ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, ArrayObjectAdapter parent, QueryType queryType) {
        super(presenter);
        mParent = parent;
        mQuery = query;
        mQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        if (chunkSize > 0) mQuery.setLimit(chunkSize);
        this.queryType = queryType;
        add(new BaseRowItem(new GridButton(0, TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, PresenterSelector presenter, ArrayObjectAdapter parent, QueryType queryType) {
        super(presenter);
        mParent = parent;
        mQuery = query;
        mQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        if (chunkSize > 0) mQuery.setLimit(chunkSize);
        this.queryType = queryType;
        add(new BaseRowItem(new GridButton(0, TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, ArrayObjectAdapter parent) {
        this(query, chunkSize, preferParentThumb, staticHeight, presenter, parent, QueryType.Items);
    }

    public ItemRowAdapter(ArtistsQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mArtistsQuery = query;
        mArtistsQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        staticHeight = true;
        this.chunkSize = chunkSize;
        if (chunkSize > 0) mArtistsQuery.setLimit(chunkSize);
        queryType = QueryType.AlbumArtists;
        add(new BaseRowItem(new GridButton(0, TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(NextUpQuery query, boolean preferParentThumb, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mNextUpQuery = query;
        mNextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.NextUp;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = true;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(SeriesTimerQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSeriesTimerQuery = query;
        queryType = QueryType.SeriesTimer;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(LatestItemsQuery query, boolean preferParentThumb, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mLatestQuery = query;
        mLatestQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.LatestItems;
        this.preferParentThumb = preferParentThumb;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(BaseItemPerson[] people, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mPersons = people;
        staticHeight = true;
        queryType = QueryType.StaticPeople;
    }

    public ItemRowAdapter(List<ChapterItemInfo> chapters, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mChapters = chapters;
        staticHeight = true;
        queryType = QueryType.StaticChapters;
    }

    public ItemRowAdapter(List<BaseItemDto> items, Presenter presenter, ArrayObjectAdapter parent, QueryType queryType) {
        super(presenter);
        mParent = parent;
        mItems = items;
        this.queryType = queryType;
    }

    public ItemRowAdapter(List<BaseItemDto> items, Presenter presenter, ArrayObjectAdapter parent, boolean staticItems) { // last param is just for sig
        super(presenter);
        mParent = parent;
        mItems = items;
        queryType = QueryType.StaticItems;
    }

    public ItemRowAdapter(ServerInfo[] servers, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mServers = servers;
        queryType = QueryType.StaticServers;
    }

    public ItemRowAdapter(SpecialsQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSpecialsQuery = query;
        queryType = QueryType.Specials;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(TrailersQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTrailersQuery = query;
        queryType = QueryType.Trailers;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(LiveTvChannelQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvChannelQuery = query;
        this.chunkSize = chunkSize;
        if (chunkSize > 0) mTvChannelQuery.setLimit(chunkSize);
        queryType = QueryType.LiveTvChannel;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(RecommendedProgramQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvProgramQuery = query;
        queryType = QueryType.LiveTvProgram;
        staticHeight = true;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(RecordingQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvRecordingQuery = query;
        this.chunkSize = chunkSize;
        queryType = QueryType.LiveTvRecording;
        staticHeight = true;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(RecordingGroupQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvRecordingGroupQuery = query;
        queryType = QueryType.LiveTvRecordingGroup;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(SimilarItemsQuery query, QueryType queryType, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSimilarQuery = query;
        mSimilarQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        this.queryType = queryType;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(UpcomingEpisodesQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mUpcomingQuery = query;
        mUpcomingQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.Upcoming;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(SeasonQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSeasonQuery = query;
        mSeasonQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.Season;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(PersonsQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        this.chunkSize = chunkSize;
        mPersonsQuery = query;
        mPersonsQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        if (chunkSize > 0) mPersonsQuery.setLimit(chunkSize);
        queryType = QueryType.Persons;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(SearchQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSearchQuery = query;
        mSearchQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        mSearchQuery.setLimit(50);
        queryType = QueryType.Search;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(ViewQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        queryType = QueryType.Views;
        staticHeight = true;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(ServerInfo serverInfo, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mServer = serverInfo;
        queryType = QueryType.Users;
    }

    public void setItemsLoaded(int itemsLoaded) {
        this.itemsLoaded = itemsLoaded;
        this.fullyLoaded = chunkSize == 0 || itemsLoaded >= totalItems;
    }

    public void setSearchString(String value) {
        if (mSearchQuery != null) {
            mSearchQuery.setSearchTerm(value);
        }
    }

    public int getItemsLoaded() {
        return itemsLoaded;
    }

    public void setTotalItems(int amt) {
        totalItems = amt;
    }

    public int getTotalItems() { return totalItems; }

    public void setSortBy(HorizontalGridFragment.SortOption option) {
        if (!option.value.equals(mSortBy)) {
            mSortBy = option.value;
            switch (queryType) {
                case AlbumArtists:
                    mArtistsQuery.setSortBy(new String[]{mSortBy});
                    mArtistsQuery.setSortOrder(option.order);
                    break;
                default:
                    mQuery.setSortBy(new String[] {mSortBy});
                    mQuery.setSortOrder(option.order);
                    break;
            }
            if (!"SortName".equals(option.value)) setStartLetter(null);
        }
    }

    public BaseRowItem findByIndex(int ndx) {
        //search for actual index number and return matching item
        for (int i = 0; i < getItemsLoaded(); i++) {
            BaseRowItem item = (BaseRowItem)this.get(i);
            if (item.getIndex() == ndx) return item;
        }
        return null;
    }

    public String getSortBy() { return mSortBy; }

    public FilterOptions getFilters() { return mFilters; }

    public void setFilters(FilterOptions filters) {
        mFilters = filters;
            switch (queryType) {
                case AlbumArtists:
                    mArtistsQuery.setFilters(mFilters != null ? mFilters.getFilters() : null);
                    break;
                default:
                    mQuery.setFilters(mFilters != null ? mFilters.getFilters() : null);
            }
    }

    public void setPosition(int pos) {
        ((IPositionablePresenter)(getParent().getPresenter(this))).setPosition(pos);
    }

    public String getStartLetter() { return mQuery != null ? mQuery.getNameStartsWithOrGreater() : null; }

    public void setStartLetter(String value) {
        switch (queryType) {
            case AlbumArtists:
                if (value != null && value.equals("#")) mArtistsQuery.setNameStartsWithOrGreater(null);
                else mArtistsQuery.setNameStartsWithOrGreater(value);
                break;
            default:
                if (value != null && value.equals("#")) mQuery.setNameStartsWithOrGreater(null);
                else mQuery.setNameStartsWithOrGreater(value);
                break;
        }
    }

    public void removeRow() {
        if (mParent == null) {
            // just clear us
            clear();
            return;
        };

        if (mParent.size() == 1) {
            // we will be removing the last row - show something and prevent the framework from crashing
            // because there is nowhere for focus to land
            ArrayObjectAdapter emptyRow = new ArrayObjectAdapter(new TextItemPresenter());
            emptyRow.add(TvApp.getApplication().getString(R.string.lbl_no_items));
            mParent.add(new ListRow(new HeaderItem(TvApp.getApplication().getString(R.string.lbl_empty)), emptyRow));
        }

        mParent.remove(mRow);
    }

    public void loadMoreItemsIfNeeded(long pos) {
        if (fullyLoaded) {
            //TvApp.getApplication().getLogger().Debug("Row is fully loaded");
            return;
        }
        if (isCurrentlyRetrieving()) {
            TvApp.getApplication().getLogger().Debug("Not loading more because currently retrieving");
            return;
        }

        if (pos >= itemsLoaded - 20) {
            TvApp.getApplication().getLogger().Debug("Loading more items starting at " + itemsLoaded);
            RetrieveNext();
        }

    }

    public void RetrieveNext() {
        switch (queryType) {
            case Persons:
                if (fullyLoaded || mPersonsQuery == null || isCurrentlyRetrieving()) return;
                setCurrentlyRetrieving(true);

                //set the query to go get the next chunk
                mPersonsQuery.setStartIndex(itemsLoaded);
                Retrieve(mPersonsQuery);
                break;

            case LiveTvChannel:
                if (fullyLoaded || mTvChannelQuery == null || isCurrentlyRetrieving()) return;
                setCurrentlyRetrieving(true);

                //set the query to go get the next chunk
                mTvChannelQuery.setStartIndex(itemsLoaded);
                Retrieve(mTvChannelQuery);
                break;

            case AlbumArtists:
                if (fullyLoaded || mArtistsQuery == null || isCurrentlyRetrieving()) return;
                setCurrentlyRetrieving(true);

                //set the query to go get the next chunk
                mArtistsQuery.setStartIndex(itemsLoaded);
                Retrieve(mArtistsQuery);
                break;

            default:
                if (fullyLoaded || mQuery == null || isCurrentlyRetrieving()) return;
                setCurrentlyRetrieving(true);

                //set the query to go get the next chunk
                mQuery.setStartIndex(itemsLoaded);
                Retrieve(mQuery);
                break;
        }
    }

    public boolean ReRetrieveIfNeeded() {
        if (reRetrieveTriggers == null) return false;

        boolean retrieve = false;
        TvApp app = TvApp.getApplication();
        for (ChangeTriggerType trigger : reRetrieveTriggers) {
            switch (trigger) {
                case LibraryUpdated:
                    retrieve |= lastFullRetrieve.before(app.getLastLibraryChange());
                    break;
                case MoviePlayback:
                    retrieve |= lastFullRetrieve.before(app.getLastMoviePlayback());
                    break;
                case TvPlayback:
                    retrieve |= lastFullRetrieve.before(app.getLastTvPlayback());
                    break;
                case MusicPlayback:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < app.getLastMusicPlayback();
                    break;
                case FavoriteUpdate:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < app.getLastFavoriteUpdate();
                    break;
                case VideoQueueChange:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < app.getLastVideoQueueChange();
                    break;
                case GuideNeedsLoad:
                    Calendar start = new GregorianCalendar(TimeZone.getTimeZone("Z"));
                    start.set(Calendar.MINUTE, start.get(Calendar.MINUTE) >= 30 ? 30 : 0);
                    start.set(Calendar.SECOND, 0);
                    retrieve |= TvManager.programsNeedLoad(start);
                    break;
                case Always:
                    retrieve = true;
                    break;
            }
        }

        if (retrieve) {
            TvApp.getApplication().getLogger().Info("Re-retrieving row of type "+ queryType);
            Retrieve();
        }

        return retrieve;
    }

    public void Retrieve() {
        setCurrentlyRetrieving(true);
        lastFullRetrieve = Calendar.getInstance();
        itemsLoaded = 0;
        notifyRetrieveStarted();
        switch (queryType) {
            case Items:
                Retrieve(mQuery);
                break;
            case NextUp:
                Retrieve(mNextUpQuery);
                break;
            case LatestItems:
                Retrieve(mLatestQuery);
                break;
            case Upcoming:
                Retrieve(mUpcomingQuery);
                break;
            case Season:
                Retrieve(mSeasonQuery);
                break;
            case Views:
                RetrieveViews();
                break;
            case SimilarSeries:
                RetrieveSimilarSeries(mSimilarQuery);
                break;
            case SimilarMovies:
                RetrieveSimilarMovies(mSimilarQuery);
                break;
            case Persons:
                Retrieve(mPersonsQuery);
                break;
            case LiveTvChannel:
                Retrieve(mTvChannelQuery);
                break;
            case LiveTvProgram:
                Retrieve(mTvProgramQuery);
                break;
            case LiveTvRecording:
                Retrieve(mTvRecordingQuery);
                break;
            case LiveTvRecordingGroup:
                Retrieve(mTvRecordingGroupQuery);
                break;
            case StaticPeople:
                LoadPeople();
                break;
            case StaticServers:
                LoadServers();
                break;
            case StaticChapters:
                LoadChapters();
                break;
            case StaticItems:
                LoadStaticItems();
                break;
            case StaticAudioQueueItems:
                LoadStaticAudioItems();
                break;
            case Specials:
                Retrieve(mSpecialsQuery);
                break;
            case Trailers:
                Retrieve(mTrailersQuery);
                break;
            case Users:
                RetrieveUsers(mServer);
                break;
            case Search:
                Retrieve(mSearchQuery);
                break;
            case AlbumArtists:
                Retrieve(mArtistsQuery);
                break;
            case AudioPlaylists:
                RetrieveAudioPlaylists(mQuery);
                break;
            case Premieres:
                RetrievePremieres(mQuery);
                break;
            case ContinueWatching:
                RetrieveContinueWatching(mQuery);
                break;
            case SeriesTimer:
                Retrieve(mSeriesTimerQuery);
                break;
        }
    }

    private void RetrieveUsers(ServerInfo mServer) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getLoginApiClient().GetPublicUsersAsync(new Response<UserDto[]>() {
            @Override
            public void onResponse(UserDto[] response) {
                for (UserDto user : response) {
                    adapter.add(new BaseRowItem(user));
                }
                totalItems = response.length;
                setItemsLoaded(totalItems);
                if (totalItems == 0) removeRow();

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving users", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                removeRow();
                currentlyRetrieving = false;
            }
        });

    }

    private void LoadPeople() {
        if (mPersons != null) {
            for (BaseItemPerson person : mPersons) {
                add(new BaseRowItem(person));
            }

        } else {
            removeRow();
        }

        currentlyRetrieving = false;
    }

    private void LoadChapters() {
        if (mChapters != null) {
            for (ChapterItemInfo chapter : mChapters) {
                add(new BaseRowItem(chapter));
            }

        } else {
            removeRow();
        }

        currentlyRetrieving = false;
    }

    private void LoadStaticItems() {
        if (mItems != null) {
            for (BaseItemDto item : mItems) {
                add(new BaseRowItem(item));
            }
            itemsLoaded = mItems.size();
        } else {
            removeRow();
        }

        currentlyRetrieving = false;
    }

    private void LoadStaticAudioItems() {
        if (mItems != null) {
            int i = 0;
            for (BaseItemDto item : mItems) {
                add(new AudioQueueItem(i++, item));
            }
            itemsLoaded = i;

        } else {
            removeRow();
        }

        currentlyRetrieving = false;
    }

    private void LoadServers() {
        if (mServers != null) {
            for (ServerInfo server : mServers) {
                add(new BaseRowItem(server));
            }
            itemsLoaded = mServers.length;
        } else {
            removeRow();
        }

        currentlyRetrieving = false;
    }

    private static String[] ignoreTypes = new String[] {"books","games"};
    private static List<String> ignoreTypeList = Arrays.asList(ignoreTypes);

    private void RetrieveViews() {
        final ItemRowAdapter adapter = this;
        UserDto user = TvApp.getApplication().getCurrentUser();
        TvApp.getApplication().getConnectionManager().GetApiClient(user).GetUserViews(user.getId(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        //re-map the display prefs id to our actual id
                        item.setDisplayPreferencesId(item.getId());
                        if (!ignoreTypeList.contains(item.getCollectionType()) && !ignoreTypeList.contains(item.getType()))
                            adapter.add(new BaseRowItem(i++, item, preferParentThumb, staticHeight));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    private void Retrieve(SearchQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetSearchHintsAsync(query, new Response<SearchHintResult>() {
            @Override
            public void onResponse(SearchHintResult response) {
                if (response.getSearchHints() != null && response.getSearchHints().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (SearchHint item : response.getSearchHints()) {
                        if (!ignoreTypeList.contains(item.getType())) {
                            i++;
                            adapter.add(new BaseRowItem(item));
                        }
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (itemsLoaded > 0 && mParent != null) mParent.add(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving search results", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void GetResultSizeAsync(final Response<Integer> outerResponse) {
        switch (queryType) {
            case AlbumArtists:
                mArtistsQuery.setLimit(1); // minimum result set because we just need total record count

                TvApp.getApplication().getApiClient().GetAlbumArtistsAsync(mArtistsQuery, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        mArtistsQuery.setLimit(chunkSize > 0 ? chunkSize : null);
                        outerResponse.onResponse(response.getTotalRecordCount());
                    }

                    @Override
                    public void onError(Exception exception) {
                        mArtistsQuery.setLimit(chunkSize > 0 ? chunkSize : null);
                        outerResponse.onError(exception);
                    }
                });
                break;
            case Items:
                StdItemQuery sizeQuery = new StdItemQuery(new ItemFields[]{});
                sizeQuery.setIncludeItemTypes(mQuery.getIncludeItemTypes());
                sizeQuery.setNameStartsWithOrGreater(mQuery.getNameStartsWithOrGreater());
                sizeQuery.setNameLessThan(mQuery.getNameLessThan());
                sizeQuery.setFilters(getFilters().getFilters());
                sizeQuery.setRecursive(mQuery.getRecursive());
                sizeQuery.setParentId(mQuery.getParentId());
                sizeQuery.setLimit(1); // minimum result set because we just need total record count

                TvApp.getApplication().getApiClient().GetItemsAsync(sizeQuery, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        outerResponse.onResponse(response.getTotalRecordCount());
                    }

                    @Override
                    public void onError(Exception exception) {
                        outerResponse.onError(exception);
                    }
                });
                break;
            default:
                outerResponse.onError(new Exception("Can only be used with standard query"));
                break;
        }

    }

    public void Retrieve(final ItemQuery query) {
        TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(query.getEnableTotalRecordCount() ? response.getTotalRecordCount() : response.getItems().length);
                    int i = getItemsLoaded();
                    int prevItems = i == 0 && size() > 0 ? size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        add(new BaseRowItem(i++, item, getPreferParentThumb(), isStaticHeight()));
                        //TvApp.getApplication().getLogger().Debug("Item Type: "+item.getType());

                    }
                    setItemsLoaded(i);
                    if (i == 0) {
                        removeRow();
                    } else if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    if (getItemsLoaded() == 0) removeRow();
                }

                setCurrentlyRetrieving(false);
                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
                if (exception instanceof HttpException) {
                    HttpException httpException = (HttpException) exception;
                    if (httpException.getStatusCode() != null && httpException.getStatusCode() == 401 && "ParentalControl".equals(httpException.getHeaders().get("X-Application-Error-Code"))) {
                        Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_access_restricted));
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                System.exit(1);
                            }
                        }, 3000);
                    } else {
                        removeRow();
                        Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                    }
                } else {
                    removeRow();
                    Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());

                }
                setCurrentlyRetrieving(false);
                notifyRetrieveFinished();
            }

        });
    }

    public void RetrieveAudioPlaylists(final ItemQuery query) {
        //Add specialized playlists first
        clear();
        add(new GridButton(EnhancedBrowseFragment.FAVSONGS, TvApp.getApplication().getString(R.string.lbl_favorites), R.drawable.genericmusic));
        itemsLoaded = 1;
        Retrieve(query);
    }

    public void RetrieveContinueWatching(final ItemQuery query) {
        //Add current video queue first if there
        clear();
        if (MediaManager.hasVideoQueueItems()) {
            TvApp.getApplication().getLogger().Debug("Adding video queue...");
            add(new BaseRowItem(new GridButton(TvApp.VIDEO_QUEUE_OPTION_ID, TvApp.getApplication().getString(R.string.lbl_current_queue), R.drawable.videoqueue)));
            itemsLoaded = 1;
        }
        Retrieve(query);
    }

    public void Retrieve(ArtistsQuery query) {
        TvApp.getApplication().getApiClient().GetAlbumArtistsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(response.getTotalRecordCount());
                    int i = getItemsLoaded();
                    int prevItems = i == 0 && size() > 0 ? size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        add(new BaseRowItem(i++, item, getPreferParentThumb(), isStaticHeight()));
                        //TvApp.getApplication().getLogger().Debug("Item Type: "+item.getType());
                    }
                    setItemsLoaded(i);
                    if (i == 0) {
                        removeRow();
                    } else if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    setTotalItems(0);
                    removeRow();
                }

                setCurrentlyRetrieving(false);
                notifyRetrieveFinished();

            }
        });
    }

    public void Retrieve(LatestItemsQuery query) {
        TvApp.getApplication().getApiClient().GetLatestItems(query, new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response != null && response.length > 0) {
                    setTotalItems(response.length);
                    int i = getItemsLoaded();
                    int prevItems = i == 0 && size() > 0 ? size() : 0;
                    for (BaseItemDto item : response) {
                        add(new BaseRowItem(i++, item, getPreferParentThumb(), isStaticHeight()));
                        //TvApp.getApplication().getLogger().Debug("Item Type: "+item.getType());
                    }
                    setItemsLoaded(i);
                    if (i == 0) {
                        removeRow();
                    } else if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    setTotalItems(0);
                    removeRow();
                }

                setCurrentlyRetrieving(false);
                notifyRetrieveFinished();

            }
        });
    }

    private void RetrievePremieres(final ItemQuery query) {
        final ItemRowAdapter adapter = this;
        //First we need current Next Up to filter our list with
        NextUpQuery nextUp = new NextUpQuery();
        nextUp.setUserId(query.getUserId());
        nextUp.setParentId(query.getParentId());
        nextUp.setLimit(50);
        TvApp.getApplication().getApiClient().GetNextUpEpisodesAsync(nextUp, new Response<ItemsResult>() {
            @Override
            public void onResponse(final ItemsResult nextUpResponse) {
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        if (adapter.size() > 0) adapter.clear();
                        if (response.getItems() != null && response.getItems().length > 0) {
                            int i = 0;
                            Calendar compare = Calendar.getInstance();
                            compare.add(Calendar.MONTH, -2);
                            BaseItemDto[] nextUpItems = nextUpResponse.getItems();
                            for (BaseItemDto item : response.getItems()) {
                                if (item.getIndexNumber() != null && item.getIndexNumber() == 1 && (item.getDateCreated() == null || item.getDateCreated().after(compare.getTime()))
                                        && (item.getUserData() == null || item.getUserData().getLikes() == null || item.getUserData().getLikes())
                                        ) {
                                    // new unwatched episode 1 not disliked - check to be sure prev episode not already in next up
                                    BaseItemDto nextUpItem = null;
                                    for (int n = 0; n < nextUpItems.length; n++) {
                                        if (nextUpItems[n].getSeriesId().equals(item.getSeriesId())) {
                                            nextUpItem = nextUpItems[n];
                                            break;
                                        }
                                    }

                                    if (nextUpItem == null || nextUpItem.getId().equals(item.getId())) {
                                        //Now - let's be sure there isn't already a premiere for this series
                                        BaseRowItem existing = null;
                                        int existingPos = -1;
                                        for (int n = 0; n < adapter.size(); n++) {
                                            if (((BaseRowItem)adapter.get(n)).getBaseItem().getSeriesId().equals(item.getSeriesId())) {
                                                existing = (BaseRowItem)adapter.get(n);
                                                existingPos = n;
                                                break;
                                            }
                                        }
                                        if (existing == null) {
                                            TvApp.getApplication().getLogger().Debug("Adding new episode 1 to premieres " + item.getSeriesName());
                                            adapter.add(new BaseRowItem(i++, item, preferParentThumb, true));

                                        } else if (existing.getBaseItem().getParentIndexNumber() > item.getParentIndexNumber()) {
                                            //Replace the newer item with the earlier season
                                            TvApp.getApplication().getLogger().Debug("Replacing newer episode 1 with an older season for " + item.getSeriesName());
                                            adapter.replace(existingPos, new BaseRowItem(i++, item, preferParentThumb, false));
                                        } // otherwise, just ignore this newer season premiere since we have the older one already

                                    } else {
                                        TvApp.getApplication().getLogger().Info("Didn't add %s to premieres because different episode is in next up.", item.getSeriesName());
                                    }
                                }
                            }
                            setItemsLoaded(itemsLoaded + i);
                        }


                        if (adapter.size() == 0) removeRow();
                        currentlyRetrieving = false;
                    }
                });

            }

        });

    }

    public void Retrieve(final NextUpQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetNextUpEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(final ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    if (adapter.size() > 0) adapter.clear();
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item, preferParentThumb, staticHeight));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                        currentlyRetrieving = false;
                    } else {
                        //If this was for a single series, get the rest of the episodes in the season
                        if (query.getSeriesId() != null) {
                            BaseItemDto first = adapter.size() == 1 ? ((BaseRowItem)adapter.get(0)).getBaseItem() : null;
                            if (first != null && first.getIndexNumber() != null && first.getSeasonId() != null) {
                                StdItemQuery rest = new StdItemQuery();
                                rest.setUserId(query.getUserId());
                                rest.setParentId(first.getSeasonId());
                                rest.setStartIndex(first.getIndexNumber());
                                TvApp.getApplication().getApiClient().GetItemsAsync(rest, new Response<ItemsResult>() {
                                    @Override
                                    public void onResponse(ItemsResult innerResponse) {
                                        if (response.getItems() != null) {
                                            int n = response.getItems().length;
                                            for (BaseItemDto item : innerResponse.getItems()) {
                                                adapter.add(new BaseRowItem(n++, item, preferParentThumb, false));
                                            }
                                            totalItems += innerResponse.getTotalRecordCount();
                                            setItemsLoaded(itemsLoaded + n);

                                        }
                                        currentlyRetrieving = false;
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        TvApp.getApplication().getLogger().ErrorException("Unable to retrieve subsequent episodes in next up", exception);
                                        currentlyRetrieving = false;
                                    }
                                });
                            }
                        }

                    }
                } else {
                    // no results - don't show us
                    removeRow();
                    currentlyRetrieving = false;
                }

            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving next up items", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final LiveTvChannelQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(ChannelInfoDtoResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = itemsLoaded;
                    if (i == 0 && adapter.size() > 0) adapter.clear();
                    for (ChannelInfoDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i, item));
                        i++;
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(i);
                    if (i == 0) removeRow();
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving live tv channels", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final RecommendedProgramQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetRecommendedLiveTvProgramsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                TvManager.updateProgramsNeedsLoadTime();
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    if (query.getIsAiring()) {
                        // show guide option as first item
                        adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_GUIDE_OPTION_ID, TvApp.getApplication().getResources().getString(R.string.lbl_live_tv_guide), R.drawable.guide)));
                        i++;
                    }
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(item, staticHeight));
                        i++;
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(i);
                    if (i == 0) {
                        removeRow();
                    } else if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving live tv programs", exception);
                removeRow();
                //TODO suppress this message for now - put it back when server returns empty set for no live tv
                //Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final RecordingGroupQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetLiveTvRecordingGroupsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        item.setType("RecordingGroup"); // the API does not fill this in
                        item.setIsFolder(true); // nor this
                        adapter.add(new BaseRowItem(item));
                        i++;
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    } else if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;

            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving live tv recording groups", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });
    }

    public void Retrieve(final SeriesTimerQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetLiveTvSeriesTimersAsync(query, new Response<SeriesTimerInfoDtoResult>() {
            @Override
            public void onResponse(SeriesTimerInfoDtoResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    for (SeriesTimerInfoDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(item));
                        i++;
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    } else if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;

            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving live tv series timers", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });
    }

    public void Retrieve(final RecordingQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetLiveTvRecordingsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    if (adapter.chunkSize == 0) {
                        // and recordings as first item if showing all
                        adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_RECORDINGS_OPTION_ID, TvApp.getApplication().getResources().getString(R.string.lbl_recorded_tv), R.drawable.recgroup)));
                        i++;
                        if (TvApp.getApplication().canManageRecordings()) {
                            // and schedule
                            adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_SCHEDULE_OPTION_ID, TvApp.getApplication().getResources().getString(R.string.lbl_schedule), R.drawable.clock)));
                            i++;
                            // and series
                            adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_SERIES_OPTION_ID, TvApp.getApplication().getResources().getString(R.string.lbl_series), R.drawable.seriestimerp)));
                            i++;
                        }
                    }

                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(item, staticHeight));
                        i++;
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    } else if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving live tv recordings", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final SpecialsQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetSpecialFeaturesAsync(TvApp.getApplication().getCurrentUser().getId(), query.getItemId(), new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response) {
                        adapter.add(new BaseRowItem(i++, item, preferParentThumb, false));
                    }
                    totalItems = response.length;
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) removeRow();
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving special features", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final TrailersQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetLocalTrailersAsync(TvApp.getApplication().getCurrentUser().getId(), query.getItemId(), new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response) {
                        item.setName(TvApp.getApplication().getString(R.string.lbl_trailer) + (i + 1));
                        adapter.add(new BaseRowItem(i++, item, preferParentThumb, false, BaseRowItem.SelectAction.Play));
                    }
                    totalItems = response.length;
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) removeRow();
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving special features", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void RetrieveSimilarSeries(final SimilarItemsQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetSimilarItems(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) removeRow();
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving similar series items", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void RetrieveSimilarMovies(final SimilarItemsQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetSimilarItems(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) removeRow();
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving similar series items", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final UpcomingEpisodesQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetUpcomingEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        if (query.getParentId() == null || item.getSeriesId() == null || item.getSeriesId().equals(query.getParentId()))
                            adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) removeRow();
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving upcoming items", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final PersonsQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetPeopleAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = itemsLoaded;
                    if (i == 0 && adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(i);
                    if (i == 0) removeRow();
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving people", exception);
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(SeasonQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetSeasonsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeItems(0, prevItems);
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving season items", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void notifyRetrieveFinished() {
        if (mRetrieveFinishedListener != null) {
            mRetrieveFinishedListener.onResponse();
        }
    }

    public void setRetrieveFinishedListener(EmptyResponse response) {
        this.mRetrieveFinishedListener = response;
    }

    public void notifyRetrieveStarted() {
        if (mRetrieveStartedListener != null) {
            mRetrieveStartedListener.onResponse();
        }
    }

    public void setRetrieveStartedListener(EmptyResponse response) {
        this.mRetrieveStartedListener = response;
    }
}
