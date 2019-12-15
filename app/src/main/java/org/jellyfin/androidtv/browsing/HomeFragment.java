package org.jellyfin.androidtv.browsing;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import android.widget.Toast;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.CustomMessage;
import org.jellyfin.androidtv.base.IMessageListener;
import org.jellyfin.androidtv.channels.ChannelManager;
import org.jellyfin.androidtv.integration.RecommendationManager;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.model.ChangeTriggerType;
import org.jellyfin.androidtv.model.DisplayPriorityType;
import org.jellyfin.androidtv.playback.AudioEventListener;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.presentation.ThemeManager;
import org.jellyfin.androidtv.querying.QueryType;
import org.jellyfin.androidtv.querying.StdItemQuery;
import org.jellyfin.androidtv.querying.ViewQuery;
import org.jellyfin.androidtv.settings.SettingsActivity;
import org.jellyfin.androidtv.model.LogonCredentials;
import org.jellyfin.androidtv.startup.SelectUserActivity;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;

import java.io.IOException;

import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.livetv.RecommendedProgramQuery;
import org.jellyfin.apiclient.model.livetv.RecordingQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.LatestItemsQuery;
import org.jellyfin.apiclient.model.querying.NextUpQuery;

public class HomeFragment extends StdBrowseFragment {
    private static final int LOGOUT = 0;
    private static final int SETTINGS = 1;

    private ChannelManager channelManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        MainTitle = this.getString(R.string.home_title);

        super.onActivityCreated(savedInstanceState);

        //Save last login so we can get back proper context on entry
        try {
            AuthenticationHelper.saveLoginCredentials(new LogonCredentials(TvApp.getApplication().getApiClient().getServerInfo(), TvApp.getApplication().getCurrentUser()), TvApp.CREDENTIALS_PATH);
        } catch (IOException e) {
            TvApp.getApplication().getLogger().ErrorException("Unable to save login creds", e);
        }

        //Init recommendations
        RecommendationManager.init();

        // Init leanback home channels;
        channelManager = new ChannelManager();
        channelManager.update();

        //Get auto bitrate
        TvApp.getApplication().determineAutoBitrate();

        //First time audio message
        if (!mApplication.getSystemPrefs().getBoolean("syspref_audio_warned", false)) {
            mApplication.getSystemPrefs().edit().putBoolean("syspref_audio_warned",true).apply();
            new AlertDialog.Builder(mActivity)
                    .setTitle(mApplication.getString(R.string.lbl_audio_capabilitites))
                    .setMessage(mApplication.getString(R.string.msg_audio_warning))
                    .setPositiveButton(mApplication.getString(R.string.btn_got_it), null)
                    .setNegativeButton(mApplication.getString(R.string.btn_set_compatible_audio), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mApplication.getPrefs().edit().putString("pref_audio_option", "1").apply();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

        ThemeManager.showWelcomeMessage();

        //Subscribe to Audio messages
        MediaManager.addAudioEventListener(audioEventListener);

        //Setup activity messages
        mActivity.registerMessageListener(new IMessageListener() {
            @Override
            public void onMessageReceived(CustomMessage message) {
                switch (message) {
                    case RefreshRows:
                        if (hasResumeRow()) {
                            refreshRows();
                        } else {
                            addContinueWatching();
                        }

                        break;
                }
            }
        });

        if (mApplication.getPrefs().getBoolean("pref_live_tv_mode",false)) {
            //open guide activity and tell it to start last channel
            Intent guide = new Intent(getActivity(), LiveTvGuideActivity.class);
            guide.putExtra("loadLast", true);

            startActivity(guide);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        // Update leanback channels
        channelManager.update();

        //make sure rows have had a chance to be created
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                addNowPlaying();
                //check for resume row and add if not there
                if (!hasResumeRow()) addContinueWatching();
            }
        }, 750);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MediaManager.removeAudioEventListener(audioEventListener);
    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        //Peek at the first library item to determine our order
        TvApp.getApplication().getApiClient().GetUserViews(TvApp.getApplication().getCurrentUser().getId(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                //First library and in-progress
                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_library), new ViewQuery()));

                //Special suggestions
                String[] specialGenres = ThemeManager.getSpecialGenres();
                if (specialGenres != null) {
                    StdItemQuery suggestions = new StdItemQuery();
                    suggestions.setIncludeItemTypes(new String[]{"Movie", "Series"});
                    suggestions.setGenres(specialGenres);
                    suggestions.setRecursive(true);
                    suggestions.setLimit(40);
                    suggestions.setEnableTotalRecordCount(false);
                    suggestions.setSortBy(new String[]{ItemSortBy.DatePlayed});
                    suggestions.setSortOrder(SortOrder.Ascending);
                    mRows.add(new BrowseRowDef(ThemeManager.getSuggestionTitle(), suggestions, 0, true, true, new ChangeTriggerType[]{}));

                }

                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_continue_watching), getResumeQuery(), 0, true, true, new ChangeTriggerType[]{ChangeTriggerType.MoviePlayback, ChangeTriggerType.TvPlayback, ChangeTriggerType.VideoQueueChange}, QueryType.ContinueWatching));

                //Now others based on first library type
                if (response.getTotalRecordCount() > 0) {
                    mApplication.setDisplayPriority("tvshows".equals(response.getItems()[0].getCollectionType()) ? DisplayPriorityType.TvShows : ("livetv".equals(response.getItems()[0].getCollectionType()) ? DisplayPriorityType.LiveTv : DisplayPriorityType.Movies));
                    switch (mApplication.getDisplayPriority()) {
                        case TvShows:
                            addNextUp();
                            addPremieres();
                            addLatestMovies();
                            addLatestTVShows();
                            addOnNow();
                            break;

                        case LiveTv:
                            addOnNow();
                            addNextUp();
                            addPremieres();
                            addLatestMovies();
                            addLatestTVShows();
                            break;

                        default:
                            addLatestMovies();
                            addLatestTVShows();
                            addNextUp();
                            addPremieres();
                            addOnNow();
                    }
                }
                //        StdItemQuery latestMusic = new StdItemQuery();
                //        latestMusic.setIncludeItemTypes(new String[]{"MusicAlbum"});
                //        latestMusic.setRecursive(true);
                //        latestMusic.setLimit(50);
                //        latestMusic.setSortBy(new String[]{ItemSortBy.DateCreated});
                //        latestMusic.setSortOrder(SortOrder.Descending);
                //        mRowDef.add(new BrowseRowDef("Latest Albums", latestMusic, 0));

                rowLoader.loadRows(mRows);
            }
        });



    }

    private StdItemQuery getResumeQuery() {
        StdItemQuery resumeItems = new StdItemQuery();
        resumeItems.setMediaTypes(new String[] {"Video"});
        resumeItems.setRecursive(true);
        resumeItems.setImageTypeLimit(1);
        resumeItems.setEnableTotalRecordCount(false);
        resumeItems.setCollapseBoxSetItems(false);
        resumeItems.setExcludeLocationTypes(new LocationType[] {LocationType.Virtual});
        resumeItems.setLimit(50);
        resumeItems.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
        resumeItems.setSortBy(new String[]{ItemSortBy.DatePlayed});
        resumeItems.setSortOrder(SortOrder.Descending);
        return resumeItems;
    }

    private void addLatestMovies() {
        LatestItemsQuery latestMovies = new LatestItemsQuery();
        latestMovies.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
        latestMovies.setIncludeItemTypes(new String[]{"Movie"});
        latestMovies.setImageTypeLimit(1);
        latestMovies.setLimit(50);
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest_movies), latestMovies, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.MoviePlayback}));

    }

    private void addLatestTVShows() {
        LatestItemsQuery latestTVShows = new LatestItemsQuery();
        latestTVShows.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
        latestTVShows.setIncludeItemTypes(new String[]{"Episode"});
        latestTVShows.setImageTypeLimit(1);
        latestTVShows.setLimit(50);
        latestTVShows.setGroupItems(true);
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest_tv_shows), latestTVShows, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated, ChangeTriggerType.TvPlayback}));
    }

    private void addNextUp() {
        NextUpQuery nextUpQuery = new NextUpQuery();
        nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        nextUpQuery.setImageTypeLimit(1);
        nextUpQuery.setLimit(50);
        nextUpQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_next_up_tv), nextUpQuery, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}));

    }

    private void addPremieres() {
        if (mApplication.getPrefs().getBoolean("pref_enable_premieres", false)) {
            StdItemQuery newQuery = new StdItemQuery(new ItemFields[]{ItemFields.DateCreated, ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
            newQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
            newQuery.setIncludeItemTypes(new String[]{"Episode"});
            newQuery.setRecursive(true);
            newQuery.setIsVirtualUnaired(false);
            newQuery.setIsMissing(false);
            newQuery.setImageTypeLimit(1);
            newQuery.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
            newQuery.setSortBy(new String[]{ItemSortBy.DateCreated});
            newQuery.setSortOrder(SortOrder.Descending);
            newQuery.setEnableTotalRecordCount(false);
            newQuery.setLimit(200);
            mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_new_premieres), newQuery, 0, true, true, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}, QueryType.Premieres));

        }

    }

    private void addOnNow() {
        if (TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvAccess()) {
            RecommendedProgramQuery onNow = new RecommendedProgramQuery();
            onNow.setIsAiring(true);
            onNow.setFields(new ItemFields[] {ItemFields.Overview, ItemFields.PrimaryImageAspectRatio, ItemFields.ChannelInfo});
            onNow.setUserId(TvApp.getApplication().getCurrentUser().getId());
            onNow.setImageTypeLimit(1);
            onNow.setEnableTotalRecordCount(false);
            onNow.setLimit(20);
            mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_on_now), onNow));
            //Latest Recordings
            RecordingQuery recordings = new RecordingQuery();
            recordings.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
            recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
            recordings.setEnableImages(true);
            recordings.setLimit(40);
            mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_recordings), recordings));
        }

    }

    private boolean hasResumeRow() {
        if (mRowsAdapter == null) return true;
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            ListRow row = (ListRow)mRowsAdapter.get(i);
            if (row.getAdapter() instanceof ItemRowAdapter && ((ItemRowAdapter)row.getAdapter()).getQueryType().equals(QueryType.ContinueWatching)) return true;
        }

        return false;
    }

    private void addContinueWatching() {
        //create the row and retrieve it to see if there are any before adding
        final ItemRowAdapter resume = new ItemRowAdapter(getResumeQuery(), 0, true, true, mCardPresenter, mRowsAdapter, QueryType.ContinueWatching);
        resume.setReRetrieveTriggers(new ChangeTriggerType[] {ChangeTriggerType.VideoQueueChange, ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback});
        final ListRow row = new ListRow(new HeaderItem(mApplication.getString(R.string.lbl_continue_watching)), resume);
        resume.setRow(row);
        resume.setRetrieveFinishedListener(new EmptyResponse() {
            @Override
            public void onResponse() {
                mApplication.getLogger().Info("*** Continue watching retrieve finished: "+resume.size());
                if (resume.size() > 0) {
                    mRowsAdapter.add(1, row);
                }
            }
        });
        resume.Retrieve();
    }

    private AudioEventListener audioEventListener = new AudioEventListener() {
        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            //remove on any change - it will re-add on resume
            if (nowPlayingRow != null) {
                mRowsAdapter.remove(nowPlayingRow);
                nowPlayingRow = null;
            }
        }
    };

    private ListRow nowPlayingRow;

    private void addNowPlaying() {
        if (MediaManager.isPlayingAudio()) {
            if (nowPlayingRow == null) {
                nowPlayingRow = new ListRow(new HeaderItem(getString(R.string.lbl_now_playing)), MediaManager.getManagedAudioQueue());
                mRowsAdapter.add(1, nowPlayingRow);
            }
        } else {
            if (nowPlayingRow != null) {
                mRowsAdapter.remove(nowPlayingRow);
                nowPlayingRow = null;
            }
        }
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);

        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_settings));

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter toolsRow = new ArrayObjectAdapter(mGridPresenter);
        toolsRow.add(new GridButton(SETTINGS, mApplication.getString(R.string.lbl_settings), R.drawable.tile_settings));
        toolsRow.add(new GridButton(LOGOUT, mApplication.getString(R.string.lbl_logout), R.drawable.tile_logout));
        rowAdapter.add(new ListRow(gridHeader, toolsRow));
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();
        mClickedListener.registerListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case LOGOUT:
                        TvApp app = TvApp.getApplication();
                        if (app.getIsAutoLoginConfigured()) {
                            // Present user selection
                            app.setLoginApiClient(app.getApiClient());
                            Intent userIntent = new Intent(getActivity(), SelectUserActivity.class);
                            userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            getActivity().startActivity(userIntent);

                        }

                        getActivity().finish(); //don't actually log out because we handle it ourselves

                        break;
                    case SETTINGS:
                        Intent settings = new Intent(getActivity(), SettingsActivity.class);
                        getActivity().startActivity(settings);
                        break;
                    default:
                        Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        }
    }


}
