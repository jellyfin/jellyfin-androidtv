package org.jellyfin.androidtv.browsing;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.app.AlertDialog;
import android.widget.Toast;

import java.io.IOException;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.livetv.RecommendedProgramQuery;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.CustomMessage;
import org.jellyfin.androidtv.base.IMessageListener;
import org.jellyfin.androidtv.integration.RecommendationManager;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.model.ChangeTriggerType;
import org.jellyfin.androidtv.playback.AudioEventListener;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.presentation.ThemeManager;
import org.jellyfin.androidtv.querying.QueryType;
import org.jellyfin.androidtv.querying.StdItemQuery;
import org.jellyfin.androidtv.querying.ViewQuery;
import org.jellyfin.androidtv.settings.SettingsActivity;
import org.jellyfin.androidtv.startup.LogonCredentials;
import org.jellyfin.androidtv.startup.SelectUserActivity;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by Eric on 12/4/2014.
 */
public class HomeFragment extends StdBrowseFragment {
    private static final int LOGOUT = 0;
    private static final int SETTINGS = 1;
    private static final int REPORT = 2;
    private static final int UNLOCK = 3;
    private static final int LOGOUT_CONNECT = 4;

    private ArrayObjectAdapter toolsRow;
    private GridButton unlockButton;
    private GridButton sendLogsButton;
    private GridButton premiereButton;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        MainTitle = this.getString(R.string.home_title);

        super.onActivityCreated(savedInstanceState);

        //Save last login so we can get back proper context on entry
        try {
            Utils.SaveLoginCredentials(new LogonCredentials(TvApp.getApplication().getApiClient().getServerInfo(), TvApp.getApplication().getCurrentUser()), "credentials.json");
        } catch (IOException e) {
            TvApp.getApplication().getLogger().ErrorException("Unable to save login creds", e);
        }

        //Init recommendations
        RecommendationManager.init();

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

    }

    @Override
    public void onResume() {
        super.onResume();

        //if we were locked before and have just unlocked, remove the button
        if (unlockButton != null && (TvApp.getApplication().isRegistered() || TvApp.getApplication().isPaid())) {
            toolsRow.remove(unlockButton);
            if (!TvApp.getApplication().isRegistered()) {
                premiereButton = new GridButton(UNLOCK, mApplication.getString(R.string.btn_jellyfin_premiere), R.drawable.logoicon512);
                toolsRow.add(premiereButton);
            }
        } else {
            if (premiereButton != null && TvApp.getApplication().isRegistered()) {
                toolsRow.remove(premiereButton);
            }
        }
        addLogsButton();
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
                    suggestions.setSortBy(new String[]{ItemSortBy.DatePlayed});
                    suggestions.setSortOrder(SortOrder.Ascending);
                    mRows.add(new BrowseRowDef(ThemeManager.getSuggestionTitle(), suggestions, 0, true, true, new ChangeTriggerType[]{}));

                }

                mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_continue_watching), getResumeQuery(), 0, true, true, new ChangeTriggerType[]{ChangeTriggerType.MoviePlayback, ChangeTriggerType.TvPlayback, ChangeTriggerType.VideoQueueChange}, QueryType.ContinueWatching));

                //Now others based on first library type
                if (response.getTotalRecordCount() > 0) {
                    String firstType = ("tvshows".equals(response.getItems()[0].getCollectionType())) ? "s" : ("livetv".equals(response.getItems()[0].getCollectionType()) ? "t" : "m");
                    switch (firstType) {
                        case "s":
                            addNextUp();
                            addPremieres();
                            addLatestMovies();
                            addOnNow();
                            break;

                        case "t":
                            addOnNow();
                            addNextUp();
                            addPremieres();
                            addLatestMovies();
                            break;

                        default:
                            addLatestMovies();
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

    protected StdItemQuery getResumeQuery() {
        StdItemQuery resumeItems = new StdItemQuery();
        resumeItems.setIncludeItemTypes(new String[]{"Movie", "Episode", "Video", "Program"});
        resumeItems.setRecursive(true);
        resumeItems.setImageTypeLimit(1);
        resumeItems.setLimit(50);
        resumeItems.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
        resumeItems.setSortBy(new String[]{ItemSortBy.DatePlayed});
        resumeItems.setSortOrder(SortOrder.Descending);
        return resumeItems;
    }

    protected void addLatestMovies() {
        StdItemQuery latestMovies = new StdItemQuery();
        latestMovies.setIncludeItemTypes(new String[]{"Movie"});
        latestMovies.setRecursive(true);
        latestMovies.setImageTypeLimit(1);
        latestMovies.setLimit(50);
        latestMovies.setCollapseBoxSetItems(false);
        if (TvApp.getApplication().getCurrentUser().getConfiguration().getHidePlayedInLatest()) latestMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
        latestMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
        latestMovies.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest_movies), latestMovies, 0, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.MoviePlayback}));

    }

    protected void addNextUp() {
        NextUpQuery nextUpQuery = new NextUpQuery();
        nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        nextUpQuery.setImageTypeLimit(1);
        nextUpQuery.setLimit(50);
        nextUpQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Overview});
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_next_up_tv), nextUpQuery, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}));

    }

    protected void addPremieres() {
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
        newQuery.setLimit(300);
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_new_premieres), newQuery, 0, true, true, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}, QueryType.Premieres));

    }

    protected void addOnNow() {
        if (TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvAccess()) {
            RecommendedProgramQuery onNow = new RecommendedProgramQuery();
            onNow.setIsAiring(true);
            onNow.setFields(new ItemFields[] {ItemFields.Overview, ItemFields.PrimaryImageAspectRatio, ItemFields.ChannelInfo});
            onNow.setUserId(TvApp.getApplication().getCurrentUser().getId());
            onNow.setImageTypeLimit(1);
            onNow.setLimit(20);
            mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_on_now), onNow));
            //Latest Recordings
            RecordingQuery recordings = new RecordingQuery();
            recordings.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
            recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
            recordings.setEnableImages(true);
            recordings.setLimit(40);
            mRows.add(new BrowseRowDef("Latest Recordings", recordings));
        }

    }

    protected boolean hasResumeRow() {
        if (mRowsAdapter == null) return true;
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            ListRow row = (ListRow)mRowsAdapter.get(i);
            if (row.getAdapter() instanceof ItemRowAdapter && ((ItemRowAdapter)row.getAdapter()).getQueryType().equals(QueryType.ContinueWatching)) return true;
        }

        return false;
    }

    protected void addContinueWatching() {
        //create the row and retrieve it to see if there are any before adding
        ItemRowAdapter resume = new ItemRowAdapter(getResumeQuery(), 0, true, true, mCardPresenter, mRowsAdapter, QueryType.ContinueWatching);
        resume.setReRetrieveTriggers(new ChangeTriggerType[] {ChangeTriggerType.VideoQueueChange, ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback});
        ListRow row = new ListRow(new HeaderItem(mApplication.getString(R.string.lbl_continue_watching)), resume);
        mRowsAdapter.add(1, row);
        resume.setRow(row);
        resume.Retrieve();
    }

    protected AudioEventListener audioEventListener = new AudioEventListener() {
        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            //remove on any change - it will re-add on resume
            if (nowPlayingRow != null) {
                mRowsAdapter.remove(nowPlayingRow);
                nowPlayingRow = null;
            }
        }
    };

    protected ListRow nowPlayingRow;

    protected void addNowPlaying() {
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
        toolsRow = new ArrayObjectAdapter(mGridPresenter);
        toolsRow.add(new GridButton(SETTINGS, mApplication.getString(R.string.lbl_app_settings), R.drawable.gears));
        toolsRow.add(new GridButton(LOGOUT, mApplication.getString(R.string.lbl_logout) + TvApp.getApplication().getCurrentUser().getName(), R.drawable.logout));
        if (TvApp.getApplication().isConnectLogin()) toolsRow.add(new GridButton(LOGOUT_CONNECT, mApplication.getString(R.string.lbl_logout_connect), R.drawable.unlink));
        //give this some time to have validated
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!TvApp.getApplication().isRegistered() && !TvApp.getApplication().isPaid()) {
                    unlockButton = new GridButton(UNLOCK, mApplication.getString(R.string.lbl_unlock), R.drawable.unlock);
                    toolsRow.add(unlockButton);
                } else if (!TvApp.getApplication().isRegistered()) {
                    premiereButton = new GridButton(UNLOCK, mApplication.getString(R.string.btn_jellyfin_premiere), R.drawable.logoicon512);
                    toolsRow.add(premiereButton);
                }
            }
        }, 5000);

        sendLogsButton = new GridButton(REPORT, mApplication.getString(R.string.lbl_send_logs), R.drawable.upload);
        rowAdapter.add(new ListRow(gridHeader, toolsRow));
    }

    private void addLogsButton() {
        if (toolsRow != null && TvApp.getApplication().getPrefs().getBoolean("pref_enable_debug",false) && Utils.is50()) {
            if (toolsRow.indexOf(sendLogsButton) < 0) toolsRow.add(sendLogsButton);
            else if (toolsRow.indexOf(sendLogsButton) > -1) toolsRow.remove(sendLogsButton);
        }

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
                    case REPORT:
                        Utils.reportError(getActivity(), "Send Log to Dev");
                        break;
                    case LOGOUT_CONNECT:
                        TvApp.getApplication().getConnectionManager().Logout(new EmptyResponse() {
                            @Override
                            public void onResponse() {
                                mApplication.setConnectLogin(false);
                                getActivity().finish();
                            }
                        });
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
