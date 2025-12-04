package org.jellyfin.androidtv.ui.itemhandling;

import static org.koin.java.KoinJavaComponent.inject;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.data.querying.GetAdditionalPartsRequest;
import org.jellyfin.androidtv.data.querying.GetSeriesTimersRequest;
import org.jellyfin.androidtv.data.querying.GetSpecialsRequest;
import org.jellyfin.androidtv.data.querying.GetTrailersRequest;
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest;
import org.jellyfin.androidtv.data.repository.UserViewsRepository;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.browsing.BrowseGridFragment;
import org.jellyfin.androidtv.ui.browsing.EnhancedBrowseFragment;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.ui.presentation.TextItemPresenter;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyResponse;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemPerson;
import org.jellyfin.sdk.model.api.ItemSortBy;
import org.jellyfin.sdk.model.api.SortOrder;
import org.jellyfin.sdk.model.api.request.GetAlbumArtistsRequest;
import org.jellyfin.sdk.model.api.request.GetArtistsRequest;
import org.jellyfin.sdk.model.api.request.GetItemsRequest;
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest;
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest;
import org.jellyfin.sdk.model.api.request.GetNextUpRequest;
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest;
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest;
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest;
import org.jellyfin.sdk.model.api.request.GetSeasonsRequest;
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest;
import org.jellyfin.sdk.model.api.request.GetUpcomingEpisodesRequest;
import org.koin.java.KoinJavaComponent;

import java.time.Instant;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class ItemRowAdapter extends MutableObjectAdapter<Object> {
    private GetItemsRequest mQuery;
    private GetNextUpRequest mNextUpQuery;
    private GetSeasonsRequest mSeasonQuery;
    private GetUpcomingEpisodesRequest mUpcomingQuery;
    private GetSimilarItemsRequest mSimilarQuery;
    private GetSpecialsRequest mSpecialsQuery;
    private GetAdditionalPartsRequest mAdditionalPartsQuery;
    private GetTrailersRequest mTrailersQuery;
    private GetLiveTvChannelsRequest mTvChannelQuery;
    private GetRecommendedProgramsRequest mTvProgramQuery;
    private GetRecordingsRequest mTvRecordingQuery;
    private GetArtistsRequest mArtistsQuery;
    private GetAlbumArtistsRequest mAlbumArtistsQuery;
    private GetLatestMediaRequest mLatestQuery;
    private GetResumeItemsRequest resumeQuery;
    private QueryType queryType;

    private ItemSortBy mSortBy;
    private SortOrder sortOrder;
    private FilterOptions mFilters;

    private EmptyResponse mRetrieveFinishedListener;

    private ChangeTriggerType[] reRetrieveTriggers = new ChangeTriggerType[]{};
    private Instant lastFullRetrieve;

    private BaseItemPerson[] mPersons;
    private List<ChapterItemInfo> mChapters;
    private List<org.jellyfin.sdk.model.api.BaseItemDto> mItems;
    private MutableObjectAdapter<Row> mParent;
    private ListRow mRow;
    private int chunkSize = 0;

    private int itemsLoaded = 0;
    private int totalItems = 0;
    private boolean fullyLoaded = false;

    private final Object currentlyRetrievingSemaphore = new Object();
    private boolean currentlyRetrieving = false;

    private boolean preferParentThumb = false;
    private boolean staticHeight = false;

    private final Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private final Lazy<UserViewsRepository> userViewsRepository = inject(UserViewsRepository.class);
    private Context context;

    private boolean isCurrentlyRetrieving() {
        synchronized (currentlyRetrievingSemaphore) {
            return currentlyRetrieving;
        }
    }

    private void setCurrentlyRetrieving(boolean currentlyRetrieving) {
        synchronized (currentlyRetrievingSemaphore) {
            this.currentlyRetrieving = currentlyRetrieving;
        }
    }

    protected boolean getPreferParentThumb() {
        return preferParentThumb;
    }

    protected boolean isStaticHeight() {
        return staticHeight;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setRow(ListRow row) {
        mRow = row;
    }

    public void setReRetrieveTriggers(ChangeTriggerType[] reRetrieveTriggers) {
        this.reRetrieveTriggers = reRetrieveTriggers;
    }

    public ItemRowAdapter(Context context, GetItemsRequest query, int chunkSize, boolean preferParentThumb, Presenter presenter, MutableObjectAdapter<Row> parent) {
        this(context, query, chunkSize, preferParentThumb, false, presenter, parent);
    }

    public ItemRowAdapter(Context context, GetItemsRequest query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, MutableObjectAdapter<Row> parent, QueryType queryType) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mQuery = query;
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        this.queryType = queryType;
    }

    public ItemRowAdapter(Context context, GetItemsRequest query, int chunkSize, boolean preferParentThumb, boolean staticHeight, PresenterSelector presenter, MutableObjectAdapter<Row> parent, QueryType queryType) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mQuery = query;
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        this.queryType = queryType;
    }

    public ItemRowAdapter(Context context, GetItemsRequest query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, MutableObjectAdapter<Row> parent) {
        this(context, query, chunkSize, preferParentThumb, staticHeight, presenter, parent, QueryType.Items);
    }

    public ItemRowAdapter(Context context, GetArtistsRequest query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mArtistsQuery = query;
        staticHeight = true;
        this.chunkSize = chunkSize;
        queryType = QueryType.Artists;
    }

    public ItemRowAdapter(Context context, GetAlbumArtistsRequest query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mAlbumArtistsQuery = query;
        staticHeight = true;
        this.chunkSize = chunkSize;
        queryType = QueryType.AlbumArtists;
    }

    public ItemRowAdapter(Context context, GetNextUpRequest query, boolean preferParentThumb, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mNextUpQuery = query;
        queryType = QueryType.NextUp;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = true;
    }

    public ItemRowAdapter(Context context, GetSeriesTimersRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        queryType = QueryType.SeriesTimer;
    }

    public ItemRowAdapter(Context context, GetLatestMediaRequest query, boolean preferParentThumb, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mLatestQuery = query;
        queryType = QueryType.LatestItems;
        this.preferParentThumb = preferParentThumb;
        staticHeight = true;
    }

    public ItemRowAdapter(List<BaseItemPerson> people, Context context, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mPersons = people.toArray(new BaseItemPerson[people.size()]);
        staticHeight = true;
        queryType = QueryType.StaticPeople;
    }

    public ItemRowAdapter(Context context, List<ChapterItemInfo> chapters, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mChapters = chapters;
        staticHeight = true;
        queryType = QueryType.StaticChapters;
    }

    public ItemRowAdapter(Context context, List<org.jellyfin.sdk.model.api.BaseItemDto> items, Presenter presenter, MutableObjectAdapter<Row> parent, QueryType queryType) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mItems = items;
        this.queryType = queryType;
    }

    public ItemRowAdapter(Context context, List<BaseItemDto> items, Presenter presenter, MutableObjectAdapter<Row> parent, boolean staticItems) { // last param is just for sig
        super(presenter);
        this.context = context;
        mParent = parent;
        mItems = items;
        queryType = QueryType.StaticItems;
    }

    public ItemRowAdapter(Context context, GetSpecialsRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mSpecialsQuery = query;
        queryType = QueryType.Specials;
    }

    public ItemRowAdapter(Context context, GetAdditionalPartsRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mAdditionalPartsQuery = query;
        queryType = QueryType.AdditionalParts;
    }

    public ItemRowAdapter(Context context, GetTrailersRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTrailersQuery = query;
        queryType = QueryType.Trailers;
    }

    public ItemRowAdapter(Context context, GetLiveTvChannelsRequest query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTvChannelQuery = query;
        this.chunkSize = chunkSize;
        queryType = QueryType.LiveTvChannel;
    }

    public ItemRowAdapter(Context context, GetRecommendedProgramsRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTvProgramQuery = query;
        queryType = QueryType.LiveTvProgram;
        staticHeight = true;
    }

    public ItemRowAdapter(Context context, GetRecordingsRequest query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTvRecordingQuery = query;
        this.chunkSize = chunkSize;
        queryType = QueryType.LiveTvRecording;
        staticHeight = true;
    }

    public ItemRowAdapter(Context context, GetSimilarItemsRequest query, QueryType queryType, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mSimilarQuery = query;
        this.queryType = queryType;
    }

    public ItemRowAdapter(Context context, GetUpcomingEpisodesRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mUpcomingQuery = query;
        queryType = QueryType.Upcoming;
    }

    public ItemRowAdapter(Context context, GetSeasonsRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mSeasonQuery = query;
        queryType = QueryType.Season;
    }

    public ItemRowAdapter(Context context, GetUserViewsRequest query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        queryType = QueryType.Views;
        staticHeight = true;
    }

    public ItemRowAdapter(Context context, GetResumeItemsRequest query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        resumeQuery = query;
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        this.queryType = QueryType.Resume;
    }

    public void setItemsLoaded(int itemsLoaded) {
        this.itemsLoaded = itemsLoaded;
        this.fullyLoaded = chunkSize == 0 || itemsLoaded >= totalItems;
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

    public void setSortBy(BrowseGridFragment.SortOption option) {
        if (!option.value.equals(mSortBy) || !option.order.equals(sortOrder)) {
            mSortBy = option.value;
            sortOrder = option.order;
            switch (queryType) {
                case Artists:
                    mArtistsQuery = ItemRowAdapterHelperKt.setArtistsSorting(mArtistsQuery, option);
                    break;
                case AlbumArtists:
                    mAlbumArtistsQuery = ItemRowAdapterHelperKt.setAlbumArtistsSorting(mAlbumArtistsQuery, option);
                    break;
                default:
                    mQuery = ItemRowAdapterHelperKt.setItemsSorting(mQuery, option);
                    break;
            }
            if (!ItemSortBy.SORT_NAME.equals(option.value)) {
                setStartLetter(null);
            }
        }
    }

    public ItemSortBy getSortBy() {
        return mSortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public FilterOptions getFilters() {
        return mFilters;
    }

    public void setFilters(FilterOptions filters) {
        mFilters = filters;
        switch (queryType) {
            case Artists:
                mArtistsQuery = ItemRowAdapterHelperKt.setArtistsFilter(mArtistsQuery, filters.getFilters());
                break;
            case AlbumArtists:
                mAlbumArtistsQuery = ItemRowAdapterHelperKt.setAlbumArtistsFilter(mAlbumArtistsQuery, filters.getFilters());
                break;
            default:
                mQuery = ItemRowAdapterHelperKt.setItemsFilter(mQuery, filters.getFilters());
        }
        removeRow();
    }

    public @Nullable String getStartLetter() {
        switch (queryType) {
            case Artists:
                return mArtistsQuery != null ? mArtistsQuery.getNameStartsWith() : null;
            case AlbumArtists:
                return mAlbumArtistsQuery != null ? mAlbumArtistsQuery.getNameStartsWith() : null;
            default:
                return mQuery != null ? mQuery.getNameStartsWith() : null;
        }
    }

    public void setStartLetter(String value) {
        switch (queryType) {
            case Artists:
                if (value != null && value.equals("#")) {
                    mArtistsQuery = ItemRowAdapterHelperKt.setArtistsStartLetter(mArtistsQuery, null);
                } else {
                    mArtistsQuery = ItemRowAdapterHelperKt.setArtistsStartLetter(mArtistsQuery, value);
                }
                break;
            case AlbumArtists:
                if (value != null && value.equals("#")) {
                    mAlbumArtistsQuery = ItemRowAdapterHelperKt.setAlbumArtistsStartLetter(mAlbumArtistsQuery, null);
                } else {
                    mAlbumArtistsQuery = ItemRowAdapterHelperKt.setAlbumArtistsStartLetter(mAlbumArtistsQuery, value);
                }
                break;
            default:
                if (value != null && value.equals("#")) {
                    mQuery = ItemRowAdapterHelperKt.setItemsStartLetter(mQuery, null);
                } else {
                    mQuery = ItemRowAdapterHelperKt.setItemsStartLetter(mQuery, value);
                }
                break;
        }
    }

    protected void removeRow() {
        if (mParent == null) {
            // just clear us
            clear();
            return;
        }

        if (mParent.size() == 1) {
            // we will be removing the last row - show something and prevent the framework from crashing
            // because there is nowhere for focus to land
            ArrayObjectAdapter emptyRow = new ArrayObjectAdapter(new TextItemPresenter());
            emptyRow.add(context.getString(R.string.lbl_no_items));
            mParent.add(new ListRow(new HeaderItem(context.getString(R.string.lbl_empty)), emptyRow));
        }

        mParent.remove(mRow);
    }

    public void loadMoreItemsIfNeeded(int pos) {
        if (fullyLoaded) {
            //context.getLogger().Debug("Row is fully loaded");
            return;
        }
        if (isCurrentlyRetrieving()) {
            Timber.i("Not loading more because currently retrieving");
            return;
        }
        // This needs tobe based on the actual estimated cards on screen via type of presenter and WindowAlignmentOffsetPercent
        if (chunkSize > 0) {
            // we can use chunkSize as indicator on when to load
            if (pos >= (itemsLoaded - (chunkSize / 1.7))) {
                Timber.i("Loading more items trigger pos <%s> itemsLoaded <%s> from total <%s> with chunkSize <%s>", pos, itemsLoaded, totalItems, chunkSize);
                retrieveNext();
            }
        } else if (pos >= itemsLoaded - 20) {
            Timber.i("Loading more items trigger pos <%s> itemsLoaded <%s> from total <%s>", pos, itemsLoaded, totalItems);
            retrieveNext();
        }
    }

    private void retrieveNext() {
        if (fullyLoaded || isCurrentlyRetrieving() || chunkSize == 0) {
            return;
        }

        switch (queryType) {
            case LiveTvChannel:
                if (mTvChannelQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                ItemRowAdapterHelperKt.retrieveLiveTvChannels(this, api.getValue(), mTvChannelQuery, itemsLoaded, chunkSize);
                break;

            case Artists:
                if (mArtistsQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                ItemRowAdapterHelperKt.retrieveArtists(this, api.getValue(), mArtistsQuery, itemsLoaded, chunkSize);
                break;

            case AlbumArtists:
                if (mAlbumArtistsQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                ItemRowAdapterHelperKt.retrieveAlbumArtists(this, api.getValue(), mAlbumArtistsQuery, itemsLoaded, chunkSize);
                break;

            default:
                if (mQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                ItemRowAdapterHelperKt.retrieveItems(this, api.getValue(), mQuery, itemsLoaded, chunkSize);
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
                    retrieve |= dataRefreshService.getLastLibraryChange() != null && lastFullRetrieve.isBefore(dataRefreshService.getLastLibraryChange());
                    break;
                case MoviePlayback:
                    retrieve |= dataRefreshService.getLastMoviePlayback() != null && lastFullRetrieve.isBefore(dataRefreshService.getLastMoviePlayback());
                    break;
                case TvPlayback:
                    retrieve |= dataRefreshService.getLastTvPlayback() != null && lastFullRetrieve.isBefore(dataRefreshService.getLastTvPlayback());
                    break;
                case FavoriteUpdate:
                    retrieve |= dataRefreshService.getLastFavoriteUpdate() != null && lastFullRetrieve.isBefore(dataRefreshService.getLastFavoriteUpdate());
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
        lastFullRetrieve = Instant.now();
        itemsLoaded = 0;
        switch (queryType) {
            case Items:
                if (mQuery.getStartIndex() != null && mQuery.getLimit() != null) {
                    ItemRowAdapterHelperKt.retrieveItems(this, api.getValue(), mQuery, mQuery.getStartIndex(), mQuery.getLimit());
                } else {
                    ItemRowAdapterHelperKt.retrieveItems(this, api.getValue(), mQuery, 0, chunkSize);
                }
                break;
            case NextUp:
                ItemRowAdapterHelperKt.retrieveNextUpItems(this, api.getValue(), mNextUpQuery);
                break;
            case LatestItems:
                ItemRowAdapterHelperKt.retrieveLatestMedia(this, api.getValue(), mLatestQuery);
                break;
            case Upcoming:
                ItemRowAdapterHelperKt.retrieveUpcomingEpisodes(this, api.getValue(), mUpcomingQuery);
                break;
            case Season:
                ItemRowAdapterHelperKt.retrieveSeasons(this, api.getValue(), mSeasonQuery);
                break;
            case Views:
                ItemRowAdapterHelperKt.retrieveUserViews(this, api.getValue(), userViewsRepository.getValue());
                break;
            case SimilarSeries:
            case SimilarMovies:
                ItemRowAdapterHelperKt.retrieveSimilarItems(this, api.getValue(), mSimilarQuery);
                break;
            case LiveTvChannel:
                ItemRowAdapterHelperKt.retrieveLiveTvChannels(this, api.getValue(), mTvChannelQuery, 0, chunkSize);
                break;
            case LiveTvProgram:
                ItemRowAdapterHelperKt.retrieveLiveTvRecommendedPrograms(this, api.getValue(), mTvProgramQuery);
                break;
            case LiveTvRecording:
                ItemRowAdapterHelperKt.retrieveLiveTvRecordings(this, api.getValue(), mTvRecordingQuery);
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
            case Specials:
                ItemRowAdapterHelperKt.retrieveSpecialFeatures(this, api.getValue(), mSpecialsQuery);
                break;
            case AdditionalParts:
                ItemRowAdapterHelperKt.retrieveAdditionalParts(this, api.getValue(), mAdditionalPartsQuery);
                break;
            case Trailers:
                ItemRowAdapterHelperKt.retrieveTrailers(this, api.getValue(), mTrailersQuery);
                break;
            case Search:
                loadStaticItems();
                addToParentIfResultsReceived();
                break;
            case Artists:
                ItemRowAdapterHelperKt.retrieveArtists(this, api.getValue(), mArtistsQuery, 0, chunkSize);
                break;
            case AlbumArtists:
                ItemRowAdapterHelperKt.retrieveAlbumArtists(this, api.getValue(), mAlbumArtistsQuery, 0, chunkSize);
                break;
            case AudioPlaylists:
                retrieveAudioPlaylists(mQuery);
                break;
            case Premieres:
                ItemRowAdapterHelperKt.retrievePremieres(this, api.getValue(), mQuery);
                break;
            case SeriesTimer:
                boolean canManageRecordings = Utils.canManageRecordings(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue());
                ItemRowAdapterHelperKt.retrieveLiveTvSeriesTimers(this, api.getValue(), context, canManageRecordings);
                break;
            case Resume:
                ItemRowAdapterHelperKt.retrieveResumeItems(this, api.getValue(), resumeQuery);
                break;
        }
    }

    private void loadPeople() {
        if (mPersons != null) {
            for (BaseItemPerson person : mPersons) {
                add(new BaseItemPersonBaseRowItem(person));
            }

        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void loadChapters() {
        if (mChapters != null) {
            for (ChapterItemInfo chapter : mChapters) {
                add(new ChapterItemInfoBaseRowItem(chapter));
            }

        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void loadStaticItems() {
        if (mItems != null) {
            for (org.jellyfin.sdk.model.api.BaseItemDto item : mItems) {
                add(new BaseItemDtoBaseRowItem(item));
            }
            itemsLoaded = mItems.size();
        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void addToParentIfResultsReceived() {
        if (itemsLoaded > 0 && mParent != null) {
            mParent.add(mRow);
        }
    }

    private void retrieveAudioPlaylists(final GetItemsRequest query) {
        //Add specialized playlists first
        clear();
        add(new GridButtonBaseRowItem(new GridButton(EnhancedBrowseFragment.FAVSONGS, context.getString(R.string.lbl_favorites), R.drawable.favorites)));
        itemsLoaded = 1;
        ItemRowAdapterHelperKt.retrieveItems(this, api.getValue(), mQuery, 0, chunkSize);
    }

    protected void notifyRetrieveFinished() {
        notifyRetrieveFinished(null);
    }

    protected void notifyRetrieveFinished(@Nullable Exception exception) {
        if (exception != null) Timber.w(exception, "Failed to retrieve items");

        setCurrentlyRetrieving(false);
        if (mRetrieveFinishedListener != null) {
            if (exception == null) mRetrieveFinishedListener.onResponse();
            else mRetrieveFinishedListener.onError(exception);
        }
    }

    public void setRetrieveFinishedListener(EmptyResponse response) {
        this.mRetrieveFinishedListener = response;
    }

    private void notifyRetrieveStarted() {
        setCurrentlyRetrieving(true);
    }
}
