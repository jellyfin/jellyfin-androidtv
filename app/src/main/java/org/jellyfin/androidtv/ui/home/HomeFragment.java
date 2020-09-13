package org.jellyfin.androidtv.ui.home;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.HomeSectionType;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.LogonCredentials;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.integration.ChannelManager;
import org.jellyfin.androidtv.preference.SystemPreferences;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.AudioBehavior;
import org.jellyfin.androidtv.ui.shared.IMessageListener;
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef;
import org.jellyfin.androidtv.ui.browsing.IRowLoader;
import org.jellyfin.androidtv.ui.browsing.StdBrowseFragment;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.ui.playback.AudioEventListener;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.entities.DisplayPreferences;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.entities.MediaType;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.livetv.RecommendedProgramQuery;
import org.jellyfin.apiclient.model.livetv.RecordingQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemFilter;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.NextUpQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

public class HomeFragment extends StdBrowseFragment {
    // Copied from jellyfin-web (homesections.js#getDefaultSection)
    private static final HomeSectionType[] DEFAULT_SECTIONS = new HomeSectionType[]{
            HomeSectionType.LIBRARY_TILES_SMALL,
            HomeSectionType.RESUME,
            HomeSectionType.RESUME_AUDIO,
            HomeSectionType.LIVE_TV,
            HomeSectionType.NEXT_UP,
            HomeSectionType.LATEST_MEDIA,
            HomeSectionType.NONE
    };

    private List<HomeFragmentRow> rows = new ArrayList<>();
    private ItemsResult views;
    private HomeFragmentNowPlayingRow nowPlaying;
    private HomeFragmentLiveTVRow liveTVRow;
    private HomeFragmentFooterRow footer;

    private ChannelManager channelManager;

    private AudioEventListener audioEventListener = new AudioEventListener() {
        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            nowPlaying.update(mRowsAdapter);
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        MainTitle = this.getString(R.string.home_title);

        super.onActivityCreated(savedInstanceState);

        // Save last login so we can get back proper context on entry
        try {
            AuthenticationHelper.saveLoginCredentials(new LogonCredentials(get(ApiClient.class).getServerInfo(), TvApp.getApplication().getCurrentUser()), TvApp.CREDENTIALS_PATH);
        } catch (IOException e) {
            Timber.e(e, "Unable to save login credentials");
        }

        // Init leanback home channels;
        channelManager = new ChannelManager();

        // Get auto bitrate
        TvApp.getApplication().determineAutoBitrate();

        //First time audio message
        if (!get(SystemPreferences.class).get(SystemPreferences.Companion.getAudioWarned())) {
            get(SystemPreferences.class).set(SystemPreferences.Companion.getAudioWarned(), true);

            new AlertDialog.Builder(mActivity)
                    .setTitle(mApplication.getString(R.string.lbl_audio_capabilitites))
                    .setMessage(mApplication.getString(R.string.msg_audio_warning))
                    .setPositiveButton(mApplication.getString(R.string.btn_got_it), null)
                    .setNegativeButton(mApplication.getString(R.string.btn_set_compatible_audio), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            get(UserPreferences.class).set(UserPreferences.Companion.getAudioBehaviour(), AudioBehavior.DOWNMIX_TO_STEREO);
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

        //Subscribe to Audio messages
        MediaManager.addAudioEventListener(audioEventListener);

        // Setup activity messages
        mActivity.registerMessageListener(new IMessageListener() {
            @Override
            public void onMessageReceived(CustomMessage message) {
                if (message == CustomMessage.RefreshRows) {
                    if (hasResumeRow()) {
                        refreshRows();
                    }
                }
            }
        });

        if (get(UserPreferences.class).get(UserPreferences.Companion.getLiveTvMode())) {
            // Open guide activity and tell it to start last channel
            Intent guide = new Intent(getActivity(), LiveTvGuideActivity.class);
            guide.putExtra("loadLast", true);

            startActivity(guide);
        }

        nowPlaying = new HomeFragmentNowPlayingRow(getActivity());
        liveTVRow = new HomeFragmentLiveTVRow(getActivity());
        footer = new HomeFragmentFooterRow(getActivity());
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
                nowPlaying.update(mRowsAdapter);
            }
        }, 750);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MediaManager.removeAudioEventListener(audioEventListener);
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();

        mClickedListener.registerListener((itemViewHolder, item, rowViewHolder, row) -> {
            liveTVRow.onItemClicked(itemViewHolder, item, rowViewHolder, row);
            footer.onItemClicked(itemViewHolder, item, rowViewHolder, row);
        });
    }

    public void addSection(HomeSectionType type) {
        switch (type) {
            case LATEST_MEDIA:
                rows.add(loadRecentlyAdded());
                break;
            case LIBRARY_TILES_SMALL:
                rows.add(loadLibraryTiles());
                break;
            case LIBRARY_BUTTONS:
                rows.add(loadLibraryButtons());
                break;
            case RESUME:
                rows.add(loadResumeVideo());
                break;
            case RESUME_AUDIO:
                rows.add(loadResumeAudio());
                break;
            case ACTIVE_RECORDINGS:
                rows.add(loadLatestLiveTvRecordings());
                break;
            case NEXT_UP:
                rows.add(loadNextUp());
                break;
            case LIVE_TV:
                if (TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvAccess()) {
                    rows.add(liveTVRow);
                    rows.add(loadOnNow());
                }
                break;
        }
    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        TvApp application = TvApp.getApplication();

        // Update the views before creating rows
        get(ApiClient.class).GetUserViews(application.getCurrentUser().getId(), new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                views = response;

                // Use "emby" as app because jellyfin-web version uses the same
                TvApp.getApplication().getDisplayPrefsAsync("usersettings", "emby", new Response<DisplayPreferences>() {
                    @Override
                    public void onResponse(DisplayPreferences response) {
                        HashMap<String, String> prefs = response.getCustomPrefs();

                        // Section key pattern
                        Pattern pattern = Pattern.compile("^homesection(\\d+)$");

                        // Add sections to map first to make sure they stay in the correct order
                        ArrayMap<Integer, HomeSectionType> sections = new ArrayMap<>();

                        // Set defaults
                        for (int i = 0; i < DEFAULT_SECTIONS.length; i++) {
                            sections.put(i, DEFAULT_SECTIONS[i]);
                        }

                        // Overwrite with user-preferred
                        for (String key : prefs.keySet()) {
                            Matcher matcher = pattern.matcher(key);
                            if (!matcher.matches()) continue;

                            int index = Integer.parseInt(matcher.group(1));
                            HomeSectionType sectionType = HomeSectionType.getByName(prefs.get(key));

                            if (sectionType != null)
                                sections.put(index, sectionType);
                        }

                        // Fallback when no customization is done by the user
                        rows.clear();

                        // Actually add the sections
                        for (HomeSectionType section : sections.values()) {
                            if (section != HomeSectionType.NONE)
                                addSection(section);
                        }

                        loadRows();
                    }

                    @Override
                    public void onError(Exception exception) {
                        Timber.e(exception, "Unable to retrieve home sections");

                        // Fallback to default sections
                        for (HomeSectionType section : DEFAULT_SECTIONS) {
                            addSection(section);
                        }

                        loadRows();
                    }
                });
            }
        });
    }

    @Override
    public void loadRows(List<BrowseRowDef> rows) {
        // Override to make sure it is ignored because we have our custom row management
    }

    private void loadRows() {
        // Add sections to layout
        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
        mCardPresenter = new CardPresenter();

        nowPlaying.addToRowsAdapter(mCardPresenter, mRowsAdapter);

        for (HomeFragmentRow row : rows)
            row.addToRowsAdapter(mCardPresenter, mRowsAdapter);

        footer.addToRowsAdapter(mCardPresenter, mRowsAdapter);

        setAdapter(mRowsAdapter);
    }

    private HomeFragmentRow loadRecentlyAdded() {
        return new HomeFragmentLatestRow(this.views);
    }

    private HomeFragmentRow loadLibraryTiles() {
        ViewQuery query = new ViewQuery();
        return new HomeFragmentBrowseRowDefRow(new BrowseRowDef(mApplication.getString(R.string.lbl_my_media), query));
    }


    private HomeFragmentRow loadLibraryButtons() {
        // Currently not implemented, fallback to large "library tiles" until this gets implemented
        return loadLibraryTiles();
    }

    private HomeFragmentRow loadResume(String title, String[] mediaTypes) {
        StdItemQuery query = new StdItemQuery();
        query.setMediaTypes(mediaTypes);
        query.setRecursive(true);
        query.setImageTypeLimit(1);
        query.setEnableTotalRecordCount(false);
        query.setCollapseBoxSetItems(false);
        query.setExcludeLocationTypes(new LocationType[]{LocationType.Virtual});
        query.setLimit(50);
        query.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
        query.setSortBy(new String[]{ItemSortBy.DatePlayed});
        query.setSortOrder(SortOrder.Descending);

        return new HomeFragmentBrowseRowDefRow(new BrowseRowDef(title, query, 0, new ChangeTriggerType[]{ChangeTriggerType.VideoQueueChange, ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback}));
    }

    private HomeFragmentRow loadResumeVideo() {
        return loadResume(mApplication.getString(R.string.lbl_continue_watching), new String[]{MediaType.Video});
    }

    private HomeFragmentRow loadResumeAudio() {
        return loadResume(mApplication.getString(R.string.lbl_continue_watching), new String[]{MediaType.Audio});
    }

    private HomeFragmentRow loadLatestLiveTvRecordings() {
        RecordingQuery query = new RecordingQuery();
        query.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setEnableImages(true);
        query.setLimit(40);

        return new HomeFragmentBrowseRowDefRow(new BrowseRowDef(mActivity.getString(R.string.lbl_recordings), query));
    }

    private HomeFragmentRow loadNextUp() {
        NextUpQuery query = new NextUpQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setImageTypeLimit(1);
        query.setLimit(50);
        query.setFields(new ItemFields[]{
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.Overview,
                ItemFields.ChildCount
        });

        return new HomeFragmentBrowseRowDefRow(new BrowseRowDef(mApplication.getString(R.string.lbl_next_up), query, new ChangeTriggerType[]{ChangeTriggerType.TvPlayback}));
    }

    private HomeFragmentRow loadOnNow() {
        RecommendedProgramQuery query = new RecommendedProgramQuery();
        query.setIsAiring(true);
        query.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChannelInfo,
                ItemFields.ChildCount
        });
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setImageTypeLimit(1);
        query.setEnableTotalRecordCount(false);
        query.setLimit(20);

        return new HomeFragmentBrowseRowDefRow(new BrowseRowDef(mApplication.getString(R.string.lbl_on_now), query));
    }

    private boolean hasResumeRow() {
        if (mRowsAdapter == null) return true;
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            ListRow row = (ListRow) mRowsAdapter.get(i);
            if (row.getAdapter() instanceof ItemRowAdapter && ((ItemRowAdapter) row.getAdapter()).getQueryType().equals(QueryType.ContinueWatching))
                return true;
        }

        return false;
    }
}
