package tv.mediabrowser.mediabrowsertv;

import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import java.util.Arrays;
import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.net.HttpException;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.PersonsQuery;
import mediabrowser.model.querying.SeasonQuery;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.querying.UpcomingEpisodesQuery;
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
    private QueryType queryType;

    private BaseItemPerson[] mPersons;
    private ServerInfo[] mServers;
    private ServerInfo mServer;

    private ArrayObjectAdapter mParent;
    private ListRow mRow;
    private int chunkSize = 0;

    private int itemsLoaded = 0;
    private int totalItems = 0;
    private boolean fullyLoaded = false;
    private boolean currentlyRetrieving = false;

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

    public ItemRowAdapter(ItemQuery query, int chunkSize, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mQuery = query;
        mQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        this.chunkSize = chunkSize;
        if (chunkSize > 0) mQuery.setLimit(chunkSize);
        queryType = QueryType.Items;
    }

    public ItemRowAdapter(NextUpQuery query, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mNextUpQuery = query;
        mNextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        queryType = QueryType.NextUp;
    }

    public ItemRowAdapter(BaseItemPerson[] people, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mPersons = people;
        queryType = QueryType.StaticPeople;
    }

    public ItemRowAdapter(ServerInfo[] servers, Presenter presenter, ArrayObjectAdapter parent) {
        super(presenter);
        mParent = parent;
        mServers = servers;
        queryType = QueryType.StaticServers;
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
        if (chunkSize > 0) mPersonsQuery.setLimit(chunkSize);
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

    public long getItemsLoaded() {
        return itemsLoaded;
    }

    public void loadMoreItemsIfNeeded(long pos) {
        if (fullyLoaded) {
            TvApp.getApplication().getLogger().Debug("Row is fully loaded");
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

            default:
                if (fullyLoaded || mQuery == null || isCurrentlyRetrieving()) return;
                setCurrentlyRetrieving(true);

                //set the query to go get the next chunk
                mQuery.setStartIndex(itemsLoaded);
                Retrieve(mQuery);
                break;
        }
    }

    public void Retrieve() {
        setCurrentlyRetrieving(true);
        this.clear();
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
            case StaticPeople:
                LoadPeople();
                break;
            case StaticServers:
                LoadServers();
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
                if (totalItems == 0) mParent.remove(mRow);

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving users", exception);
                Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                mParent.remove(mRow);
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
            mParent.remove(mRow);
        }

        currentlyRetrieving = false;
    }

    private void LoadServers() {
        if (mServers != null) {
            for (ServerInfo server : mServers) {
                add(new BaseRowItem(server));
            }

        } else {
            mParent.remove(mRow);
        }

        currentlyRetrieving = false;
    }

    private static String[] ignoreTypes = new String[] {"music","books","games"};
    private static List<String> ignoreTypeList = Arrays.asList(ignoreTypes);

    private void RetrieveViews() {
        final ItemRowAdapter adapter = this;
        UserDto user = TvApp.getApplication().getCurrentUser();
        TvApp.getApplication().getConnectionManager().GetApiClient(user).GetUserViews(user.getId(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        if (!ignoreTypeList.contains(item.getCollectionType())) adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
                mParent.remove(mRow);
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
        final ItemRowAdapter adapter = this;
            TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = itemsLoaded;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(i);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving items", exception);
                if (exception instanceof HttpException) {
                    HttpException httpException = (HttpException) exception;
                    if (httpException.getStatusCode() == 401 && "ParentalControl".equals(httpException.getHeaders().get("X-Application-Error-Code"))) {
                        Utils.showToast(TvApp.getApplication(), "Access Restricted at this time");
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                System.exit(1);
                            }
                        }, 3000);
                    } else {
                        mParent.remove(mRow);
                        Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());
                    }
                } else {
                    mParent.remove(mRow);
                    Utils.showToast(TvApp.getApplication(), exception.getLocalizedMessage());

                }
                currentlyRetrieving = false;
            }
        });

    }
    public void Retrieve(final NextUpQuery query) {
        final ItemRowAdapter adapter = this;
        TvApp.getApplication().getApiClient().GetNextUpEpisodesAsync(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = 0;
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) mParent.remove(mRow);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving next up items", exception);
                mParent.remove(mRow);
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
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) mParent.remove(mRow);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving similar series items", exception);
                mParent.remove(mRow);
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
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) mParent.remove(mRow);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving similar series items", exception);
                mParent.remove(mRow);
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
                    for (BaseItemDto item : response.getItems()) {
                        if (query.getParentId() == null || item.getSeriesId() == null || item.getSeriesId().equals(query.getParentId()))
                            adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                    if (i == 0) mParent.remove(mRow);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving upcoming items", exception);
                mParent.remove(mRow);
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
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++, item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(i);
                    if (i == 0) mParent.remove(mRow);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
                }

                currentlyRetrieving = false;
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving people", exception);
                mParent.remove(mRow);
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
                    for (BaseItemDto item : response.getItems()) {
                        adapter.add(new BaseRowItem(i++,item));
                    }
                    totalItems = response.getTotalRecordCount();
                    setItemsLoaded(itemsLoaded + i);
                } else {
                    // no results - don't show us
                    mParent.remove(mRow);
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

    public void setRow(ListRow row) {
        mRow = row;
    }
}
