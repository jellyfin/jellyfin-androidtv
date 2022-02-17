package org.jellyfin.androidtv.ui.itemhandling;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.data.querying.SpecialsQuery;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.querying.TrailersQuery;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.data.repository.UserViewsRepository;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.GridFragment;
import org.jellyfin.androidtv.ui.browsing.EnhancedBrowseFragment;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.presentation.TextItemPresenter;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemPerson;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.LiveTvChannelQuery;
import org.jellyfin.apiclient.model.livetv.RecommendedProgramQuery;
import org.jellyfin.apiclient.model.livetv.RecordingGroupQuery;
import org.jellyfin.apiclient.model.livetv.RecordingQuery;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerQuery;
import org.jellyfin.apiclient.model.net.HttpException;
import org.jellyfin.apiclient.model.querying.ArtistsQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.LatestItemsQuery;
import org.jellyfin.apiclient.model.querying.NextUpQuery;
import org.jellyfin.apiclient.model.querying.PersonsQuery;
import org.jellyfin.apiclient.model.querying.SeasonQuery;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;
import org.jellyfin.apiclient.model.querying.UpcomingEpisodesQuery;
import org.jellyfin.apiclient.model.results.ChannelInfoDtoResult;
import org.jellyfin.apiclient.model.results.SeriesTimerInfoDtoResult;
import org.jellyfin.apiclient.model.search.SearchHint;
import org.jellyfin.apiclient.model.search.SearchHintResult;
import org.jellyfin.apiclient.model.search.SearchQuery;
import org.koin.java.KoinJavaComponent;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import kotlin.Lazy;
import timber.log.Timber;

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

    private ChangeTriggerType[] reRetrieveTriggers = new ChangeTriggerType[]{};
    private Calendar lastFullRetrieve;

    private BaseItemPerson[] mPersons;
    private List<ChapterItemInfo> mChapters;
    private List<BaseItemDto> mItems;

    private ArrayObjectAdapter mParent;
    private ListRow mRow;
    private int chunkSize = 0;

    private int itemsLoaded = 0;
    private int totalItems = 0;
    private boolean fullyLoaded = false;

    private final Object currentlyRetrievingSemaphore = new Object();
    private boolean currentlyRetrieving = false;

    private boolean preferParentThumb = false;
    private boolean staticHeight = false;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private Lazy<UserViewsRepository> userViewsRepository = inject(UserViewsRepository.class);

    public boolean isCurrentlyRetrieving() {
        synchronized (currentlyRetrievingSemaphore) {
            return currentlyRetrieving;
        }
    }

    protected void setCurrentlyRetrieving(boolean currentlyRetrieving) {
        synchronized (currentlyRetrievingSemaphore) {
            this.currentlyRetrieving = currentlyRetrieving;
        }
    }

    public boolean getPreferParentThumb() {
        return preferParentThumb;
    }

    public boolean isStaticHeight() {
        return staticHeight;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public ArrayObjectAdapter getParent() {
        return mParent;
    }

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
        if (chunkSize > 0) {
            mQuery.setLimit(chunkSize);
        }
        this.queryType = queryType;
    }

    public ItemRowAdapter(ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, PresenterSelector presenter, ArrayObjectAdapter parent, QueryType queryType) {
        super(presenter);
        mParent = parent;
        mQuery = query;
        mQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        if (chunkSize > 0) {
            mQuery.setLimit(chunkSize);
        }
        this.queryType = queryType;
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
        if (chunkSize > 0) {
            mArtistsQuery.setLimit(chunkSize);
        }
        queryType = QueryType.AlbumArtists;
    }

    public ItemRowAdapter(NextUpQuery query, boolean preferParentThumb, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mNextUpQuery = query;
        mNextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.NextUp;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = true;
    }

    public ItemRowAdapter(SeriesTimerQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSeriesTimerQuery = query;
        queryType = QueryType.SeriesTimer;
    }

    public ItemRowAdapter(LatestItemsQuery query, boolean preferParentThumb, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mLatestQuery = query;
        mLatestQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.LatestItems;
        this.preferParentThumb = preferParentThumb;
        staticHeight = true;
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

    public ItemRowAdapter(SpecialsQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSpecialsQuery = query;
        queryType = QueryType.Specials;
    }

    public ItemRowAdapter(TrailersQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTrailersQuery = query;
        queryType = QueryType.Trailers;
    }

    public ItemRowAdapter(LiveTvChannelQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvChannelQuery = query;
        this.chunkSize = chunkSize;
        if (chunkSize > 0) {
            mTvChannelQuery.setLimit(chunkSize);
        }
        queryType = QueryType.LiveTvChannel;
    }

    public ItemRowAdapter(RecommendedProgramQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvProgramQuery = query;
        queryType = QueryType.LiveTvProgram;
        staticHeight = true;
    }

    public ItemRowAdapter(RecordingQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvRecordingQuery = query;
        this.chunkSize = chunkSize;
        queryType = QueryType.LiveTvRecording;
        staticHeight = true;
    }

    public ItemRowAdapter(RecordingGroupQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvRecordingGroupQuery = query;
        queryType = QueryType.LiveTvRecordingGroup;
    }

    public ItemRowAdapter(SimilarItemsQuery query, QueryType queryType, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSimilarQuery = query;
        mSimilarQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        this.queryType = queryType;
    }

    public ItemRowAdapter(UpcomingEpisodesQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mUpcomingQuery = query;
        mUpcomingQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.Upcoming;
    }

    public ItemRowAdapter(SeasonQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSeasonQuery = query;
        mSeasonQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.Season;
    }

    public ItemRowAdapter(PersonsQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        this.chunkSize = chunkSize;
        mPersonsQuery = query;
        mPersonsQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        if (chunkSize > 0) {
            mPersonsQuery.setLimit(chunkSize);
        }
        queryType = QueryType.Persons;
    }

    public ItemRowAdapter(SearchQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mSearchQuery = query;
        mSearchQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        mSearchQuery.setLimit(50);
        queryType = QueryType.Search;
    }

    public ItemRowAdapter(ViewQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        queryType = QueryType.Views;
        staticHeight = true;
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

    public int getTotalItems() {
        return totalItems;
    }

    public void setSortBy(GridFragment.SortOption option) {
        if (!option.value.equals(mSortBy)) {
            mSortBy = option.value;
            switch (queryType) {
                case AlbumArtists:
                    mArtistsQuery.setSortBy(new String[]{mSortBy});
                    mArtistsQuery.setSortOrder(option.order);
                    break;
                default:
                    mQuery.setSortBy(new String[]{mSortBy});
                    mQuery.setSortOrder(option.order);
                    break;
            }
            if (!"SortName".equals(option.value)) {
                setStartLetter(null);
            }
        }
    }

    public BaseRowItem findByIndex(int ndx) {
        //search for actual index number and return matching item
        for (int i = 0; i < getItemsLoaded(); i++) {
            BaseRowItem item = (BaseRowItem) this.get(i);
            if (item.getIndex() == ndx) {
                return item;
            }
        }
        return null;
    }

    public String getSortBy() {
        return mSortBy;
    }

    public FilterOptions getFilters() {
        return mFilters;
    }

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
        Presenter presenter = getParent().getPresenter(this);
        if (presenter instanceof PositionableListRowPresenter) {
            ((PositionableListRowPresenter) presenter).setPosition(pos);
        }
    }

    public @Nullable String getStartLetter() {
        return mQuery != null ? mQuery.getNameStartsWithOrGreater() : null;
    }

    public void setStartLetter(String value) {
        switch (queryType) {
            case AlbumArtists:
                if (value != null && value.equals("#")) {
                    mArtistsQuery.setNameStartsWithOrGreater(null);
                } else {
                    mArtistsQuery.setNameStartsWithOrGreater(value);
                }
                break;
            default:
                if (value != null && value.equals("#")) {
                    mQuery.setNameStartsWithOrGreater(null);
                } else {
                    mQuery.setNameStartsWithOrGreater(value);
                }
                break;
        }
    }

    public void removeRow() {
        if (mParent == null) {
            // just clear us
            clear();
            return;
        }

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
            Timber.d("Not loading more because currently retrieving");
            return;
        }

        if (pos >= itemsLoaded - 20) {
            Timber.d("Loading more items starting at %d", itemsLoaded);
            retrieveNext();
        }

    }

    private void retrieveNext() {
        if(fullyLoaded || isCurrentlyRetrieving()) {
            return;
        }

        switch (queryType) {
            case Persons:
                if (mPersonsQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                //set the query to go get the next chunk
                mPersonsQuery.setStartIndex(itemsLoaded);
                retrieve(mPersonsQuery);
                break;

            case LiveTvChannel:
                if (mTvChannelQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                //set the query to go get the next chunk
                mTvChannelQuery.setStartIndex(itemsLoaded);
                retrieve(mTvChannelQuery);
                break;

            case AlbumArtists:
                if (mArtistsQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                //set the query to go get the next chunk
                mArtistsQuery.setStartIndex(itemsLoaded);
                retrieve(mArtistsQuery);
                break;

            default:
                if (mQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                //set the query to go get the next chunk
                mQuery.setStartIndex(itemsLoaded);
                retrieve(mQuery);
                break;
        }
    }

    public boolean ReRetrieveIfNeeded() {
        if (reRetrieveTriggers == null) {
            return false;
        }

        boolean retrieve = false;
        DataRefreshService dataRefreshService = KoinJavaComponent.get(DataRefreshService.class);
        for (ChangeTriggerType trigger : reRetrieveTriggers) {
            switch (trigger) {
                case LibraryUpdated:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < dataRefreshService.getLastLibraryChange();
                    break;
                case MoviePlayback:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < dataRefreshService.getLastMoviePlayback();
                    break;
                case TvPlayback:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < dataRefreshService.getLastTvPlayback();
                    break;
                case MusicPlayback:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < dataRefreshService.getLastMusicPlayback();
                    break;
                case FavoriteUpdate:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < dataRefreshService.getLastFavoriteUpdate();
                    break;
                case VideoQueueChange:
                    retrieve |= lastFullRetrieve.getTimeInMillis() < dataRefreshService.getLastVideoQueueChange();
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
            Timber.i("Re-retrieving row of type %s", queryType.toString());
            Retrieve();
        }

        return retrieve;
    }

    public void Retrieve() {
        notifyRetrieveStarted();

        lastFullRetrieve = Calendar.getInstance();
        itemsLoaded = 0;
        switch (queryType) {
            case Items:
                retrieve(mQuery);
                break;
            case NextUp:
                retrieve(mNextUpQuery);
                break;
            case LatestItems:
                retrieve(mLatestQuery);
                break;
            case Upcoming:
                retrieve(mUpcomingQuery);
                break;
            case Season:
                retrieve(mSeasonQuery);
                break;
            case Views:
                retrieveViews();
                break;
            case SimilarSeries:
                retrieveSimilarSeries(mSimilarQuery);
                break;
            case SimilarMovies:
                retrieveSimilarMovies(mSimilarQuery);
                break;
            case Persons:
                retrieve(mPersonsQuery);
                break;
            case LiveTvChannel:
                retrieve(mTvChannelQuery);
                break;
            case LiveTvProgram:
                retrieve(mTvProgramQuery);
                break;
            case LiveTvRecording:
                retrieve(mTvRecordingQuery);
                break;
            case LiveTvRecordingGroup:
                retrieve(mTvRecordingGroupQuery);
                break;
            case StaticPeople:
                loadPeople();
                break;
            case StaticChapters:
                loadChapters();
                break;
            case StaticItems:
                loadStaticItems();
                break;
            case StaticAudioQueueItems:
                loadStaticAudioItems();
                break;
            case Specials:
                retrieve(mSpecialsQuery);
                break;
            case Trailers:
                retrieve(mTrailersQuery);
                break;
            case Search:
                retrieve(mSearchQuery);
                break;
            case AlbumArtists:
                retrieve(mArtistsQuery);
                break;
            case AudioPlaylists:
                retrieveAudioPlaylists(mQuery);
                break;
            case Premieres:
                retrievePremieres(mQuery);
                break;
            case SeriesTimer:
                retrieve(mSeriesTimerQuery);
                break;
        }
    }

    private void loadPeople() {
        if (mPersons != null) {
            for (BaseItemPerson person : mPersons) {
                add(new BaseRowItem(person));
            }

        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void loadChapters() {
        if (mChapters != null) {
            for (ChapterItemInfo chapter : mChapters) {
                add(new BaseRowItem(chapter));
            }

        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void loadStaticItems() {
        if (mItems != null) {
            for (BaseItemDto item : mItems) {
                add(new BaseRowItem(item));
            }
            itemsLoaded = mItems.size();
        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void loadStaticAudioItems() {
        if (mItems != null) {
            int i = 0;
            for (BaseItemDto item : mItems) {
                add(new AudioQueueItem(i++, item));
            }
            itemsLoaded = i;

        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void retrieveViews() {
        final ItemRowAdapter adapter = this;
        final UserDto user = TvApp.getApplication().getCurrentUser();
        apiClient.getValue().GetUserViews(user.getId(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        //re-map the display prefs id to our actual id
                        item.setDisplayPreferencesId(item.getId());
                        if (userViewsRepository.getValue().isSupported(item.getCollectionType())) {
                            adapter.add(new BaseRowItem(i++, item, preferParentThumb, staticHeight));
                        }
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

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving items");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(SearchQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetSearchHintsAsync(query, new Response<SearchHintResult>() {
            @Override
            public void onResponse(SearchHintResult response) {
                if (response.getSearchHints() != null && response.getSearchHints().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (SearchHint item : response.getSearchHints()) {
                        if (userViewsRepository.getValue().isSupported(item.getType())) {
                            i++;
                            adapter.add(new BaseRowItem(item));
                        }
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving search results");
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    public void addToParentIfResultsReceived() {
        if (itemsLoaded > 0 && mParent != null) {
            mParent.add(mRow);
        }
    }

    public void GetResultSizeAsync(final Response<Integer> outerResponse) {
        switch (queryType) {
            case AlbumArtists:
                mArtistsQuery.setLimit(1); // minimum result set because we just need total record count

                apiClient.getValue().GetAlbumArtistsAsync(mArtistsQuery, new Response<ItemsResult>() {
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

                apiClient.getValue().GetItemsAsync(sizeQuery, new Response<ItemsResult>() {
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

    private void retrieve(final ItemQuery query) {
        apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(query.getEnableTotalRecordCount() ? response.getTotalRecordCount() : response.getItems().length);
                    int i = getItemsLoaded();
                    int prevItems = i == 0 && size() > 0 ? size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        add(new BaseRowItem(i++, item, getPreferParentThumb(), isStaticHeight()));

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
                    if (getItemsLoaded() == 0) {
                        removeRow();
                    }
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving items");
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
                notifyRetrieveFinished();
            }

        });
    }

    private void retrieveAudioPlaylists(final ItemQuery query) {
        //Add specialized playlists first
        clear();
        add(new GridButton(EnhancedBrowseFragment.FAVSONGS, TvApp.getApplication().getString(R.string.lbl_favorites), R.drawable.favorites, null));
        itemsLoaded = 1;
        retrieve(query);
    }

    private void retrieve(ArtistsQuery query) {
        apiClient.getValue().GetAlbumArtistsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(response.getTotalRecordCount());
                    int i = getItemsLoaded();
                    int prevItems = i == 0 && size() > 0 ? size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        add(new BaseRowItem(i++, item, getPreferParentThumb(), isStaticHeight()));
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

                notifyRetrieveFinished();
            }
        });
    }

    private void retrieve(LatestItemsQuery query) {
        apiClient.getValue().GetLatestItems(query, new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response != null && response.length > 0) {
                    setTotalItems(response.length);
                    int i = getItemsLoaded();
                    int prevItems = i == 0 && size() > 0 ? size() : 0;
                    for (BaseItemDto item : response) {
                        add(new BaseRowItem(i++, item, getPreferParentThumb(), isStaticHeight()));
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

                notifyRetrieveFinished();
            }
        });
    }

    private void retrievePremieres(final ItemQuery query) {
        final ItemRowAdapter adapter = this;
        //First we need current Next Up to filter our list with
        NextUpQuery nextUp = new NextUpQuery();
        nextUp.setUserId(query.getUserId());
        nextUp.setParentId(query.getParentId());
        nextUp.setLimit(50);
        apiClient.getValue().GetNextUpEpisodesAsync(nextUp, new Response<ItemsResult>() {
            @Override
            public void onResponse(final ItemsResult nextUpResponse) {
                apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        if (adapter.size() > 0) {
                            adapter.clear();
                        }
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
                                            if (((BaseRowItem) adapter.get(n)).getBaseItem().getSeriesId().equals(item.getSeriesId())) {
                                                existing = (BaseRowItem) adapter.get(n);
                                                existingPos = n;
                                                break;
                                            }
                                        }
                                        if (existing == null) {
                                            Timber.d("Adding new episode 1 to premieres %s", item.getSeriesName());
                                            adapter.add(new BaseRowItem(i++, item, preferParentThumb, true));

                                        } else if (existing.getBaseItem().getParentIndexNumber() > item.getParentIndexNumber()) {
                                            //Replace the newer item with the earlier season
                                            Timber.d("Replacing newer episode 1 with an older season for %s", item.getSeriesName());
                                            adapter.replace(existingPos, new BaseRowItem(i++, item, preferParentThumb, false));
                                        } // otherwise, just ignore this newer season premiere since we have the older one already

                                    } else {
                                        Timber.i("Didn't add %s to premieres because different episode is in next up.", item.getSeriesName());
                                    }
                                }
                            }
                            setItemsLoaded(itemsLoaded + i);
                        }


                        if (adapter.size() == 0) {
                            removeRow();
                        }
                        notifyRetrieveFinished();
                    }
                });

            }

        });

    }

    private void retrieve(final NextUpQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetNextUpEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(final ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item, preferParentThumb, staticHeight));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                        notifyRetrieveFinished();
                    } else {
                        //If this was for a single series, get the rest of the episodes in the season
                        if (query.getSeriesId() != null) {
                            BaseItemDto first = adapter.size() == 1 ? ((BaseRowItem) adapter.get(0)).getBaseItem() : null;
                            if (first != null && first.getIndexNumber() != null && first.getSeasonId() != null) {
                                StdItemQuery rest = new StdItemQuery();
                                rest.setUserId(query.getUserId());
                                rest.setParentId(first.getSeasonId());
                                rest.setStartIndex(first.getIndexNumber());
                                apiClient.getValue().GetItemsAsync(rest, new Response<ItemsResult>() {
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
                                        notifyRetrieveFinished();
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        Timber.e(exception, "Unable to retrieve subsequent episodes in next up");
                                        notifyRetrieveFinished();
                                    }
                                });
                            }
                        }

                    }
                } else {
                    // no results - don't show us
                    removeRow();
                    notifyRetrieveFinished();
                }

            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving next up items");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(final LiveTvChannelQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(ChannelInfoDtoResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = itemsLoaded;
                    if (i == 0 && adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (ChannelInfoDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i, item));
                        i++;
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(i);
                    if (i == 0) {
                        removeRow();
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving live tv channels");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(final RecommendedProgramQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetRecommendedLiveTvProgramsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                TvManager.updateProgramsNeedsLoadTime();
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
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

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving live tv programs");
                removeRow();
                //TODO suppress this message for now - put it back when server returns empty set for no live tv
                //Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(final RecordingGroupQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetLiveTvRecordingGroupsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    for (BaseItemDto item : response.getItems()) {
                        item.setBaseItemType(BaseItemType.RecordingGroup); // the API does not fill this in
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

                notifyRetrieveFinished();

            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving live tv recording groups");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });
    }

    private void retrieve(final SeriesTimerQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetLiveTvSeriesTimersAsync(query, new Response<SeriesTimerInfoDtoResult>() {
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

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving live tv series timers");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });
    }

    private void retrieve(final RecordingQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetLiveTvRecordingsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    int prevItems = adapter.size() > 0 ? adapter.size() : 0;
                    if (adapter.chunkSize == 0) {
                        // and recordings as first item if showing all
                        adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_RECORDINGS_OPTION_ID, TvApp.getApplication().getString(R.string.lbl_recorded_tv), R.drawable.tile_port_record, null)));
                        i++;
                        if (TvApp.getApplication().canManageRecordings()) {
                            // and schedule
                            adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_SCHEDULE_OPTION_ID, TvApp.getApplication().getString(R.string.lbl_schedule), R.drawable.tile_port_time, null)));
                            i++;
                            // and series
                            adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_SERIES_OPTION_ID, TvApp.getApplication().getString(R.string.lbl_series), R.drawable.tile_port_series_timer, null)));
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

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving live tv recordings");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(final SpecialsQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetSpecialFeaturesAsync(TvApp.getApplication().getCurrentUser().getId(), query.getItemId(), new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (BaseItemDto item : response) {
                        adapter.add(new BaseRowItem(i++, item, preferParentThumb, false));
                    }
                    totalItems = response.length;
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving special features");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(final TrailersQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetLocalTrailersAsync(TvApp.getApplication().getCurrentUser().getId(), query.getItemId(), new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (BaseItemDto item : response) {
                        item.setName(TvApp.getApplication().getString(R.string.lbl_trailer) + (i + 1));
                        adapter.add(new BaseRowItem(i++, item, preferParentThumb, false, BaseRowItem.SelectAction.Play));
                    }
                    totalItems = response.length;
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving special features");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieveSimilarSeries(final SimilarItemsQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetSimilarItems(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving similar series items");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieveSimilarMovies(final SimilarItemsQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetSimilarItems(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving similar series items");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(final UpcomingEpisodesQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetUpcomingEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (BaseItemDto item : response.getItems()) {
                        if (query.getParentId() == null || item.getSeriesId() == null || item.getSeriesId().equals(query.getParentId())) {
                            adapter.add(new BaseRowItem(i++, item));
                        }
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) {
                        removeRow();
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving upcoming items");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(final PersonsQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetPeopleAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    int i = itemsLoaded;
                    if (i == 0 && adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(i);
                    if (i == 0) {
                        removeRow();
                    }
                } else {
                    // no results - don't show us
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving people");
                removeRow();
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    private void retrieve(SeasonQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetSeasonsAsync(query, new Response<ItemsResult>() {
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

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving season items");
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                notifyRetrieveFinished();
            }
        });

    }

    protected void notifyRetrieveFinished() {
        setCurrentlyRetrieving(false);
        if (mRetrieveFinishedListener != null) {
            mRetrieveFinishedListener.onResponse();
        }
    }

    public void setRetrieveFinishedListener(EmptyResponse response) {
        this.mRetrieveFinishedListener = response;
    }

    protected void notifyRetrieveStarted() {
        setCurrentlyRetrieving(true);
        if (mRetrieveStartedListener != null) {
            mRetrieveStartedListener.onResponse();
        }
    }

    public void setRetrieveStartedListener(EmptyResponse response) {
        this.mRetrieveStartedListener = response;
    }
}
