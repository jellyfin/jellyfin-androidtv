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
import org.jellyfin.androidtv.constant.LiveTvOption;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.data.querying.AdditionalPartsQuery;
import org.jellyfin.androidtv.data.querying.AlbumArtistsQuery;
import org.jellyfin.androidtv.data.querying.SpecialsQuery;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.querying.TrailersQuery;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.data.repository.UserViewsRepository;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.browsing.BrowseGridFragment;
import org.jellyfin.androidtv.ui.browsing.EnhancedBrowseFragment;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.presentation.TextItemPresenter;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyLifecycleAwareResponse;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.LiveTvChannelQuery;
import org.jellyfin.apiclient.model.livetv.RecommendedProgramQuery;
import org.jellyfin.apiclient.model.livetv.RecordingQuery;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerQuery;
import org.jellyfin.apiclient.model.querying.ArtistsQuery;
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
import org.jellyfin.sdk.model.api.BaseItemPerson;
import org.jellyfin.sdk.model.api.ItemSortBy;
import org.jellyfin.sdk.model.api.SortOrder;
import org.jellyfin.sdk.model.api.UserDto;
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest;
import org.koin.java.KoinJavaComponent;

import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import kotlin.Lazy;
import timber.log.Timber;

public class ItemRowAdapter extends MutableObjectAdapter<Object> {
    private ItemQuery mQuery;
    private NextUpQuery mNextUpQuery;
    private SeasonQuery mSeasonQuery;
    private UpcomingEpisodesQuery mUpcomingQuery;
    private SimilarItemsQuery mSimilarQuery;
    private PersonsQuery mPersonsQuery;
    private SpecialsQuery mSpecialsQuery;
    private AdditionalPartsQuery mAdditionalPartsQuery;
    private TrailersQuery mTrailersQuery;
    private LiveTvChannelQuery mTvChannelQuery;
    private RecommendedProgramQuery mTvProgramQuery;
    private RecordingQuery mTvRecordingQuery;
    private ArtistsQuery mArtistsQuery;
    private AlbumArtistsQuery mAlbumArtistsQuery;
    private LatestItemsQuery mLatestQuery;
    private SeriesTimerQuery mSeriesTimerQuery;
    private GetResumeItemsRequest resumeQuery;
    private QueryType queryType;

    private ItemSortBy mSortBy;
    private SortOrder sortOrder;
    private FilterOptions mFilters;

    private EmptyLifecycleAwareResponse mRetrieveFinishedListener;

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

    private final Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private final Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private final Lazy<UserViewsRepository> userViewsRepository = inject(UserViewsRepository.class);
    private Context context;

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

    public MutableObjectAdapter<Row> getParent() {
        return mParent;
    }

    public void setRow(ListRow row) {
        mRow = row;
    }

    public void setReRetrieveTriggers(ChangeTriggerType[] reRetrieveTriggers) {
        this.reRetrieveTriggers = reRetrieveTriggers;
    }

    public ItemRowAdapter(Context context, ItemQuery query, int chunkSize, boolean preferParentThumb, Presenter presenter, MutableObjectAdapter<Row> parent) {
        this(context, query, chunkSize, preferParentThumb, false, presenter, parent);
    }

    public ItemRowAdapter(Context context, ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, MutableObjectAdapter<Row> parent, QueryType queryType) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mQuery = query;
        mQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        if (chunkSize > 0) {
            mQuery.setLimit(chunkSize);
        }
        this.queryType = queryType;
    }

    public ItemRowAdapter(Context context, ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, PresenterSelector presenter, MutableObjectAdapter<Row> parent, QueryType queryType) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mQuery = query;
        mQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        if (chunkSize > 0) {
            mQuery.setLimit(chunkSize);
        }
        this.queryType = queryType;
    }

    public ItemRowAdapter(Context context, ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, MutableObjectAdapter<Row> parent) {
        this(context, query, chunkSize, preferParentThumb, staticHeight, presenter, parent, QueryType.Items);
    }

    public ItemRowAdapter(Context context, ArtistsQuery query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mArtistsQuery = query;
        mArtistsQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        staticHeight = true;
        this.chunkSize = chunkSize;
        if (chunkSize > 0) {
            mArtistsQuery.setLimit(chunkSize);
        }
        queryType = QueryType.Artists;
    }

    public ItemRowAdapter(Context context, AlbumArtistsQuery query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mAlbumArtistsQuery = query;
        mAlbumArtistsQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        staticHeight = true;
        this.chunkSize = chunkSize;
        if (chunkSize > 0) {
            mAlbumArtistsQuery.setLimit(chunkSize);
        }
        queryType = QueryType.AlbumArtists;
    }

    public ItemRowAdapter(Context context, NextUpQuery query, boolean preferParentThumb, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mNextUpQuery = query;
        mNextUpQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        queryType = QueryType.NextUp;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = true;
    }

    public ItemRowAdapter(Context context, SeriesTimerQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mSeriesTimerQuery = query;
        queryType = QueryType.SeriesTimer;
    }

    public ItemRowAdapter(Context context, LatestItemsQuery query, boolean preferParentThumb, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mLatestQuery = query;
        mLatestQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        queryType = QueryType.LatestItems;
        this.preferParentThumb = preferParentThumb;
        staticHeight = true;
    }

    public ItemRowAdapter(Context context, BaseItemPerson[] people, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mPersons = people;
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
        mItems = JavaCompat.mapBaseItemCollection(items);
        queryType = QueryType.StaticItems;
    }

    public ItemRowAdapter(Context context, SpecialsQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mSpecialsQuery = query;
        queryType = QueryType.Specials;
    }

    public ItemRowAdapter(Context context, AdditionalPartsQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mAdditionalPartsQuery = query;
        queryType = QueryType.AdditionalParts;
    }

    public ItemRowAdapter(Context context, TrailersQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTrailersQuery = query;
        queryType = QueryType.Trailers;
    }

    public ItemRowAdapter(Context context, LiveTvChannelQuery query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTvChannelQuery = query;
        this.chunkSize = chunkSize;
        if (chunkSize > 0) {
            mTvChannelQuery.setLimit(chunkSize);
        }
        queryType = QueryType.LiveTvChannel;
    }

    public ItemRowAdapter(Context context, RecommendedProgramQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTvProgramQuery = query;
        queryType = QueryType.LiveTvProgram;
        staticHeight = true;
    }

    public ItemRowAdapter(Context context, RecordingQuery query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mTvRecordingQuery = query;
        this.chunkSize = chunkSize;
        queryType = QueryType.LiveTvRecording;
        staticHeight = true;
    }

    public ItemRowAdapter(Context context, SimilarItemsQuery query, QueryType queryType, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mSimilarQuery = query;
        mSimilarQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        this.queryType = queryType;
    }

    public ItemRowAdapter(Context context, UpcomingEpisodesQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mUpcomingQuery = query;
        mUpcomingQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        queryType = QueryType.Upcoming;
    }

    public ItemRowAdapter(Context context, SeasonQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        mSeasonQuery = query;
        mSeasonQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        queryType = QueryType.Season;
    }

    public ItemRowAdapter(Context context, PersonsQuery query, int chunkSize, Presenter presenter, MutableObjectAdapter<Row> parent) {
        super(presenter);
        this.context = context;
        mParent = parent;
        this.chunkSize = chunkSize;
        mPersonsQuery = query;
        mPersonsQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        if (chunkSize > 0) {
            mPersonsQuery.setLimit(chunkSize);
        }
        queryType = QueryType.Persons;
    }

    public ItemRowAdapter(Context context, ViewQuery query, Presenter presenter, MutableObjectAdapter<Row> parent) {
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
                    mArtistsQuery.setSortBy(new String[]{mSortBy.getSerialName(), ItemSortBy.SORT_NAME.getSerialName()});
                    mArtistsQuery.setSortOrder(ModelCompat.asLegacy(option.order));
                    break;
                case AlbumArtists:
                    mAlbumArtistsQuery.setSortBy(new String[]{mSortBy.getSerialName(), ItemSortBy.SORT_NAME.getSerialName()});
                    mAlbumArtistsQuery.setSortOrder(ModelCompat.asLegacy(option.order));
                    break;
                default:
                    mQuery.setSortBy(new String[]{mSortBy.getSerialName(), ItemSortBy.SORT_NAME.getSerialName()});
                    mQuery.setSortOrder(ModelCompat.asLegacy(option.order));
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
                mArtistsQuery.setFilters(mFilters != null ? mFilters.getFilters() : null);
                break;
            case AlbumArtists:
                mAlbumArtistsQuery.setFilters(mFilters != null ? mFilters.getFilters() : null);
                break;
            default:
                mQuery.setFilters(mFilters != null ? mFilters.getFilters() : null);
        }
        removeRow();
    }

    public void setPosition(int pos) {
        if (getParent() != null) {
            Presenter presenter = getParent().getPresenter(this);
            if (presenter instanceof PositionableListRowPresenter) {
                ((PositionableListRowPresenter) presenter).setPosition(pos);
            }
        }
    }

    public @Nullable String getStartLetter() {
        return mQuery != null ? mQuery.getNameStartsWithOrGreater() : null;
    }

    public void setStartLetter(String value) {
        switch (queryType) {
            case Artists:
                if (value != null && value.equals("#")) {
                    mArtistsQuery.setNameStartsWithOrGreater(null);
                } else {
                    mArtistsQuery.setNameStartsWithOrGreater(value);
                }
                break;
            case AlbumArtists:
                if (value != null && value.equals("#")) {
                    mAlbumArtistsQuery.setNameStartsWithOrGreater(null);
                } else {
                    mAlbumArtistsQuery.setNameStartsWithOrGreater(value);
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
            emptyRow.add(context.getString(R.string.lbl_no_items));
            mParent.add(new ListRow(new HeaderItem(context.getString(R.string.lbl_empty)), emptyRow));
        }

        mParent.remove(mRow);
    }

    public void loadMoreItemsIfNeeded(long pos) {
        if (fullyLoaded) {
            //context.getLogger().Debug("Row is fully loaded");
            return;
        }
        if (isCurrentlyRetrieving()) {
            Timber.d("Not loading more because currently retrieving");
            return;
        }
        // This needs tobe based on the actual estimated cards on screen via type of presenter and WindowAlignmentOffsetPercent
        if (chunkSize > 0) {
            // we can use chunkSize as indicator on when to load
            if (pos >= (itemsLoaded - (chunkSize / 1.7))) {
                Timber.d("Loading more items trigger pos <%s> itemsLoaded <%s> from total <%s> with chunkSize <%s>", pos, itemsLoaded, totalItems, chunkSize);
                retrieveNext();
            }
        } else if (pos >= itemsLoaded - 20) {
            Timber.d("Loading more items trigger pos <%s> itemsLoaded <%s> from total <%s>", pos, itemsLoaded, totalItems);
            retrieveNext();
        }
    }

    private void retrieveNext() {
        if (fullyLoaded || isCurrentlyRetrieving()) {
            return;
        }

        Integer savedIdx = null;
        switch (queryType) {
            case Persons:
                if (mPersonsQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                savedIdx = mPersonsQuery.getStartIndex();
                //set the query to go get the next chunk
                mPersonsQuery.setStartIndex(itemsLoaded);
                retrieve(mPersonsQuery);
                mPersonsQuery.setStartIndex(savedIdx); // is reused so reset
                break;

            case LiveTvChannel:
                if (mTvChannelQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                savedIdx = mTvChannelQuery.getStartIndex();
                //set the query to go get the next chunk
                mTvChannelQuery.setStartIndex(itemsLoaded);
                retrieve(mTvChannelQuery);
                mTvChannelQuery.setStartIndex(savedIdx); // is reused so reset
                break;

            case Artists:
                if (mArtistsQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                savedIdx = mArtistsQuery.getStartIndex();
                //set the query to go get the next chunk
                mArtistsQuery.setStartIndex(itemsLoaded);
                retrieve(mArtistsQuery);
                mArtistsQuery.setStartIndex(savedIdx); // is reused so reset
                break;

            case AlbumArtists:
                if (mAlbumArtistsQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                savedIdx = mAlbumArtistsQuery.getStartIndex();
                //set the query to go get the next chunk
                mAlbumArtistsQuery.setStartIndex(itemsLoaded);
                retrieve(mAlbumArtistsQuery);
                mAlbumArtistsQuery.setStartIndex(savedIdx); // is reused so reset
                break;

            default:
                if (mQuery == null) {
                    return;
                }
                notifyRetrieveStarted();

                savedIdx = mQuery.getStartIndex();
                //set the query to go get the next chunk
                mQuery.setStartIndex(itemsLoaded);
                retrieve(mQuery);
                mQuery.setStartIndex(savedIdx); // is reused so reset
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
        lastFullRetrieve = Instant.now();
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
            case AdditionalParts:
                retrieve(mAdditionalPartsQuery);
                break;
            case Trailers:
                retrieve(mTrailersQuery);
                break;
            case Search:
                loadStaticItems();
                addToParentIfResultsReceived();
                break;
            case Artists:
                retrieve(mArtistsQuery);
                break;
            case AlbumArtists:
                retrieve(mAlbumArtistsQuery);
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
            case Resume:
                ItemRowAdapterHelperKt.retrieveResumeItems(this, api.getValue(), resumeQuery);
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
            for (org.jellyfin.sdk.model.api.BaseItemDto item : mItems) {
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
            for (org.jellyfin.sdk.model.api.BaseItemDto item : mItems) {
                add(new AudioQueueItem(i++, item));
            }
            itemsLoaded = i;

        } else {
            removeRow();
        }

        notifyRetrieveFinished();
    }

    private void retrieveViews() {
        final UserDto user = KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue();
        apiClient.getValue().GetUserViews(user.getId().toString(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(response.getTotalRecordCount());

                    ItemRowAdapterHelperKt.setItems(ItemRowAdapter.this, response.getItems(), (item, i) -> {
                        if (userViewsRepository.getValue().isSupported(ModelCompat.asSdk(item).getCollectionType())) {
                            item.setDisplayPreferencesId(item.getId());
                            return new BaseRowItem(i, item, preferParentThumb, staticHeight);
                        } else {
                            return null;
                        }
                    });
                } else if (getItemsLoaded() == 0) {
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving items");
                removeRow();
                notifyRetrieveFinished(exception);
            }
        });
    }

    public void addToParentIfResultsReceived() {
        if (itemsLoaded > 0 && mParent != null) {
            mParent.add(mRow);
        }
    }

    private void retrieve(final ItemQuery query) {
        apiClient.getValue().GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(query.getEnableTotalRecordCount() ? response.getTotalRecordCount() : response.getItems().length);

                    ItemRowAdapterHelperKt.setItems(ItemRowAdapter.this, response.getItems(), (item, i) -> new BaseRowItem(i, item, getPreferParentThumb(), isStaticHeight()));
                } else if (getItemsLoaded() == 0) {
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving items");
                removeRow();
                notifyRetrieveFinished(exception);
            }
        });
    }

    private void retrieveAudioPlaylists(final ItemQuery query) {
        //Add specialized playlists first
        clear();
        add(new GridButton(EnhancedBrowseFragment.FAVSONGS, context.getString(R.string.lbl_favorites), R.drawable.favorites));
        itemsLoaded = 1;
        retrieve(query);
    }

    private void retrieve(ArtistsQuery query) {
        apiClient.getValue().GetArtistsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(response.getTotalRecordCount());

                    ItemRowAdapterHelperKt.setItems(ItemRowAdapter.this, response.getItems(), (item, i) -> new BaseRowItem(i, item, getPreferParentThumb(), isStaticHeight()));
                } else if (getItemsLoaded() == 0) {
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving items");
                removeRow();
                notifyRetrieveFinished(exception);
            }
        });
    }

    private void retrieve(AlbumArtistsQuery query) {
        apiClient.getValue().GetAlbumArtistsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getItems() != null && response.getItems().length > 0) {
                    setTotalItems(response.getTotalRecordCount());

                    ItemRowAdapterHelperKt.setItems(ItemRowAdapter.this, response.getItems(), (item, i) -> new BaseRowItem(i, item, getPreferParentThumb(), isStaticHeight()));
                } else if (getItemsLoaded() == 0) {
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving items");
                removeRow();
                notifyRetrieveFinished(exception);
            }
        });
    }

    private void retrieve(LatestItemsQuery query) {
        apiClient.getValue().GetLatestItems(query, new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response != null && response.length > 0) {
                    setTotalItems(response.length);

                    ItemRowAdapterHelperKt.setItems(ItemRowAdapter.this, response, (item, i) -> new BaseRowItem(i, item, getPreferParentThumb(), isStaticHeight(), BaseRowItemSelectAction.ShowDetails, getPreferParentThumb()));
                } else if (getItemsLoaded() == 0) {
                    removeRow();
                }

                notifyRetrieveFinished();
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving items");
                removeRow();
                notifyRetrieveFinished(exception);
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
                                    for (BaseItemDto upItem : nextUpItems) {
                                        if (upItem.getSeriesId().equals(item.getSeriesId())) {
                                            nextUpItem = upItem;
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
                                            adapter.set(existingPos, new BaseRowItem(i++, item, preferParentThumb, false));
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
                    setTotalItems(response.getTotalRecordCount());
                    ItemRowAdapterHelperKt.setItems(ItemRowAdapter.this, response.getItems(), (item, i) -> new BaseRowItem(i, item, preferParentThumb, staticHeight));

                    //If this was for a single series, get the rest of the episodes in the season
                    if (query.getSeriesId() != null) {
                        org.jellyfin.sdk.model.api.BaseItemDto first = adapter.size() == 1 ? ((BaseRowItem) adapter.get(0)).getBaseItem() : null;
                        if (first != null && first.getIndexNumber() != null && first.getSeasonId() != null) {
                            StdItemQuery rest = new StdItemQuery();
                            rest.setUserId(query.getUserId());
                            rest.setParentId(first.getSeasonId().toString());
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
                notifyRetrieveFinished(exception);
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
                notifyRetrieveFinished(exception);
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
                    int prevItems = Math.max(adapter.size(), 0);
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
                        removeAt(0, prevItems);
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
                notifyRetrieveFinished(exception);
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
                    int prevItems = Math.max(adapter.size(), 0);
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
                        removeAt(0, prevItems);
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
                notifyRetrieveFinished(exception);
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
                    int prevItems = Math.max(adapter.size(), 0);
                    if (adapter.chunkSize == 0) {
                        // and recordings as first item if showing all
                        adapter.add(new BaseRowItem(new GridButton(LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID, context.getString(R.string.lbl_recorded_tv))));
                        i++;
                        if (Utils.canManageRecordings(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue())) {
                            // and schedule
                            adapter.add(new BaseRowItem(new GridButton(LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID, context.getString(R.string.lbl_schedule))));
                            i++;
                            // and series
                            adapter.add(new BaseRowItem(new GridButton(LiveTvOption.LIVE_TV_SERIES_OPTION_ID, context.getString(R.string.lbl_series))));
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
                        removeAt(0, prevItems);
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
                notifyRetrieveFinished(exception);
            }
        });

    }

    private void retrieve(final SpecialsQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetSpecialFeaturesAsync(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), query.getItemId(), new Response<BaseItemDto[]>() {
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
                notifyRetrieveFinished(exception);
            }
        });

    }

    private void retrieve(final AdditionalPartsQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetAdditionalParts(query.getItemId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<ItemsResult>() {
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
                Timber.e(exception, "Error retrieving additional parts for item");
                removeRow();
                notifyRetrieveFinished(exception);
            }
        });
    }

    private void retrieve(final TrailersQuery query) {
        final ItemRowAdapter adapter = this;
        apiClient.getValue().GetLocalTrailersAsync(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), query.getItemId(), new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    int i = 0;
                    if (adapter.size() > 0) {
                        adapter.clear();
                    }
                    for (BaseItemDto item : response) {
                        item.setName(context.getString(R.string.lbl_trailer) + (i + 1));
                        adapter.add(new BaseRowItem(i++, item, preferParentThumb, false, BaseRowItemSelectAction.Play));
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
                notifyRetrieveFinished(exception);
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
                notifyRetrieveFinished(exception);
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
                notifyRetrieveFinished(exception);
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
                notifyRetrieveFinished(exception);
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
                notifyRetrieveFinished(exception);
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
                    int prevItems = Math.max(adapter.size(), 0);
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (prevItems > 0) {
                        // remove previous items as we re-retrieved
                        // this is done this way instead of clearing the adapter to avoid bugs in the framework elements
                        removeAt(0, prevItems);
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
                notifyRetrieveFinished(exception);
            }
        });

    }

    protected void notifyRetrieveFinished() {
        notifyRetrieveFinished(null);
    }

    protected void notifyRetrieveFinished(@Nullable Exception exception) {
        setCurrentlyRetrieving(false);
        if (mRetrieveFinishedListener != null && mRetrieveFinishedListener.getActive()) {
            if (exception == null) mRetrieveFinishedListener.onResponse();
            else mRetrieveFinishedListener.onError(exception);
        }
    }

    public void setRetrieveFinishedListener(EmptyLifecycleAwareResponse response) {
        this.mRetrieveFinishedListener = response;
    }

    protected void notifyRetrieveStarted() {
        setCurrentlyRetrieving(true);
    }
}
