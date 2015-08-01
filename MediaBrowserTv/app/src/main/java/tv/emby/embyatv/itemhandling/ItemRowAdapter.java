package tv.emby.embyatv.itemhandling;

import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.RecommendedProgramQuery;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.PersonsQuery;
import mediabrowser.model.querying.SeasonQuery;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.querying.UpcomingEpisodesQuery;
import mediabrowser.model.results.ChannelInfoDtoResult;
import mediabrowser.model.search.SearchHint;
import mediabrowser.model.search.SearchHintResult;
import mediabrowser.model.search.SearchQuery;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.model.ChangeTriggerType;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.presentation.TextItemPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.SpecialsQuery;
import tv.emby.embyatv.querying.TrailersQuery;
import tv.emby.embyatv.querying.ViewQuery;
import tv.emby.embyatv.ui.GridButton;
import tv.emby.embyatv.util.Utils;

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
    private QueryType queryType;

    private ChangeTriggerType[] reRetrieveTriggers = new ChangeTriggerType[] {};
    private Calendar lastFullRetrieve;

    private BaseItemPerson[] mPersons;
    private ServerInfo[] mServers;
    private List<ChapterItemInfo> mChapters;
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

    public ItemRowAdapter(ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mQuery = query;
        mQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        if (chunkSize > 0) mQuery.setLimit(chunkSize);
        queryType = QueryType.Items;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(NextUpQuery query, boolean preferParentThumb, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mNextUpQuery = query;
        mNextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.NextUp;
        this.preferParentThumb = preferParentThumb;
        add(new BaseRowItem(new GridButton(0,TvApp.getApplication().getString(R.string.lbl_loading_elipses), R.drawable.loading)));
    }

    public ItemRowAdapter(BaseItemPerson[] people, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mPersons = people;
        queryType = QueryType.StaticPeople;
    }

    public ItemRowAdapter(List<ChapterItemInfo> chapters, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mChapters = chapters;
        queryType = QueryType.StaticChapters;
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

    public ItemRowAdapter(RecordingQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mTvRecordingQuery = query;
        queryType = QueryType.LiveTvRecording;
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

    public void removeRow() {
        if (mParent.size() == 1) {
            // we will be removing the last row - show something and prevent the framework from crashing
            // because there is nowhere for focus to land
            ArrayObjectAdapter emptyRow = new ArrayObjectAdapter(new TextItemPresenter());
            emptyRow.add(TvApp.getApplication().getString(R.string.lbl_no_items));
            mParent.add(new ListRow(new HeaderItem(TvApp.getApplication().getString(R.string.lbl_empty),null),emptyRow));
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
            TvApp.getApplication().getLogger().Debug("Loading more items starting at "+itemsLoaded);
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

            default:
                if (fullyLoaded || mQuery == null || isCurrentlyRetrieving()) return;
                setCurrentlyRetrieving(true);

                //set the query to go get the next chunk
                mQuery.setStartIndex(itemsLoaded);
                Retrieve(mQuery);
                break;
        }
    }

    public void ReRetrieveIfNeeded() {
        if (reRetrieveTriggers == null) return;

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
                case Always:
                    retrieve = true;
                    break;
            }
        }

        if (retrieve) {
            TvApp.getApplication().getLogger().Info("Re-retrieving row of type "+ queryType);
            Retrieve();
        }
    }

    public void Retrieve() {
        setCurrentlyRetrieving(true);
        lastFullRetrieve = Calendar.getInstance();
        itemsLoaded = 0;
        switch (queryType) {
            case Items:
                Retrieve(mQuery);
                break;
            case NextUp:
                Retrieve(mNextUpQuery);
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
            case StaticPeople:
                LoadPeople();
                break;
            case StaticServers:
                LoadServers();
                break;
            case StaticChapters:
                LoadChapters();
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

    private void LoadServers() {
        if (mServers != null) {
            for (ServerInfo server : mServers) {
                add(new BaseRowItem(server));
            }

        } else {
            removeRow();
        }

        currentlyRetrieving = false;
    }

    private static String[] ignoreTypes = new String[] {"music","books","games","playlists"};
    private static List<String> ignoreTypeList = Arrays.asList(ignoreTypes);

    private void RetrieveViews() {
        final ItemRowAdapter adapter = this;
        UserDto user = TvApp.getApplication().getCurrentUser();
        TvApp.getApplication().getConnectionManager().GetApiClient(user).GetUserViews(user.getId(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        if (!ignoreTypeList.contains(item.getCollectionType()) && !ignoreTypeList.contains(item.getType())) adapter.add(new BaseRowItem(i++,item, preferParentThumb, staticHeight));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
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
                if (response.getTotalRecordCount() > 0) {
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
                    if (itemsLoaded > 0) mParent.add(mRow);
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

    public void Retrieve(ItemQuery query) {
        TvApp.getApplication().getApiClient().GetItemsAsync(query, new ItemQueryResponse(this));
    }

    public void Retrieve(final NextUpQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetNextUpEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item, preferParentThumb, false));
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
                if (response.getTotalRecordCount() > 0) {
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
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    if (query.getIsAiring()) {
                        // show guide option as first item
                        adapter.add(new BaseRowItem(new GridButton(TvApp.LIVE_TV_GUIDE_OPTION_ID, "Live TV Guide", R.drawable.guide)));
                        i++;
                    }
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(item, staticHeight));
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
                TvApp.getApplication().getLogger().ErrorException("Error retrieving live tv programs", exception);
                removeRow();
                //TODO suppress this message for now - put it back when server returns empty set for no live tv
                //Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                currentlyRetrieving = false;
            }
        });

    }

    public void Retrieve(final RecordingQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetLiveTvRecordingsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(item));
                        i++;
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
        TvApp.getApplication().getApiClient().GetSimilarSeriesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
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
        TvApp.getApplication().getApiClient().GetSimilarMoviesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
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
                if (response.getTotalRecordCount() > 0) {
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
                if (response.getTotalRecordCount() > 0) {
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
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    if (adapter.size() > 0) adapter.clear();
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
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

}
