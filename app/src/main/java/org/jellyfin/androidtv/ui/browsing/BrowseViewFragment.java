package org.jellyfin.androidtv.ui.browsing;

import static org.koin.java.KoinJavaComponent.inject;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Row;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.constant.LiveTvOption;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.querying.GetSeriesTimersRequest;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.livetv.RecordingQuery;
import org.jellyfin.apiclient.model.livetv.TimerInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.results.TimerInfoDtoResult;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.CollectionType;
import org.jellyfin.sdk.model.api.request.GetNextUpRequest;
import org.koin.java.KoinJavaComponent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class BrowseViewFragment extends EnhancedBrowseFragment {
    private boolean isLiveTvLibrary;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

    @Override
    protected void setupQueries(final RowLoader rowLoader) {
        CollectionType type = mFolder != null && mFolder.getCollectionType() != null ? mFolder.getCollectionType() : CollectionType.UNKNOWN;
        switch (type) {
            case MOVIES:
                itemType = BaseItemKind.MOVIE;

                //Resume
                mRows.add(new BrowseRowDef(getString(R.string.lbl_continue_watching), BrowsingUtils.createResumeItemsRequest(mFolder.getId(), BaseItemKind.MOVIE), 0, new ChangeTriggerType[]{ChangeTriggerType.MoviePlayback}));

                //Latest
                mRows.add(new BrowseRowDef(getString(R.string.lbl_latest), BrowsingUtils.createLatestMediaRequest(mFolder.getId()), new ChangeTriggerType[]{ChangeTriggerType.MoviePlayback, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                mRows.add(new BrowseRowDef(getString(R.string.lbl_favorites), BrowsingUtils.createFavoriteItemsRequest(mFolder.getId(), BaseItemKind.MOVIE), 60, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                //Collections
                mRows.add(new BrowseRowDef(getString(R.string.lbl_collections), BrowsingUtils.createCollectionsRequest(mFolder.getId()), 60, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));

                rowLoader.loadRows(mRows);
                break;
            case TVSHOWS:
                itemType = BaseItemKind.SERIES;

                //Resume
                mRows.add(new BrowseRowDef(getString(R.string.lbl_continue_watching), BrowsingUtils.createResumeItemsRequest(mFolder.getId(), BaseItemKind.EPISODE), 0, new ChangeTriggerType[]{ChangeTriggerType.TvPlayback}));

                //Next up
                GetNextUpRequest getNextUpRequest = BrowsingUtils.createGetNextUpRequest(mFolder.getId());
                mRows.add(new BrowseRowDef(getString(R.string.lbl_next_up), getNextUpRequest, new ChangeTriggerType[]{ChangeTriggerType.TvPlayback}));

                //Premieres
                if (KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getPremieresEnabled())) {
                    mRows.add(new BrowseRowDef(getString(R.string.lbl_new_premieres), BrowsingUtils.createPremieresRequest(mFolder.getId()), 0, true, true, new ChangeTriggerType[]{ChangeTriggerType.TvPlayback}, QueryType.Premieres));
                }

                //Latest content added
                mRows.add(new BrowseRowDef(getString(R.string.lbl_latest), BrowsingUtils.createLatestMediaRequest(mFolder.getId(), BaseItemKind.EPISODE, true), new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));

                //Favorites
                mRows.add(new BrowseRowDef(getString(R.string.lbl_favorites), BrowsingUtils.createFavoriteItemsRequest(mFolder.getId(), BaseItemKind.SERIES), 60, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                rowLoader.loadRows(mRows);
                break;
            case MUSIC:
                //Latest
                mRows.add(new BrowseRowDef(getString(R.string.lbl_latest), BrowsingUtils.createLatestMediaRequest(mFolder.getId(), BaseItemKind.AUDIO, true), new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}));

                //Last Played
                mRows.add(new BrowseRowDef(getString(R.string.lbl_last_played), BrowsingUtils.createLastPlayedRequest(mFolder.getId()), 0, false, true, new ChangeTriggerType[]{ChangeTriggerType.MusicPlayback, ChangeTriggerType.LibraryUpdated}));

                //Favorites
                mRows.add(new BrowseRowDef(getString(R.string.lbl_favorites), BrowsingUtils.createFavoriteItemsRequest(mFolder.getId(), BaseItemKind.MUSIC_ALBUM), 60, false, true, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated, ChangeTriggerType.FavoriteUpdate}));

                //AudioPlaylists
                mRows.add(new BrowseRowDef(getString(R.string.lbl_playlists), BrowsingUtils.createPlaylistsRequest(), 60, false, true, new ChangeTriggerType[]{ChangeTriggerType.LibraryUpdated}, QueryType.AudioPlaylists));

                rowLoader.loadRows(mRows);
                break;
            case LIVETV:
                isLiveTvLibrary = true;
                showViews = true;

                //On now
                mRows.add(new BrowseRowDef(getString(R.string.lbl_on_now), BrowsingUtils.createLiveTVOnNowRequest()));

                //Upcoming
                mRows.add(new BrowseRowDef(getString(R.string.lbl_coming_up), BrowsingUtils.createLiveTVUpcomingRequest()));

                //Fav Channels
                mRows.add(new BrowseRowDef(getString(R.string.lbl_favorite_channels), BrowsingUtils.createLiveTVChannelsRequest(true)));

                //Other Channels
                mRows.add(new BrowseRowDef(getString(R.string.lbl_other_channels), BrowsingUtils.createLiveTVChannelsRequest(false)));

                //Latest Recordings
                RecordingQuery recordings = new RecordingQuery();
                recordings.setFields(new ItemFields[]{
                        ItemFields.Overview,
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                recordings.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                recordings.setEnableImages(true);
                recordings.setLimit(40);

                //Do a straight query and then split the returned items into logical groups
                apiClient.getValue().GetLiveTvRecordingsAsync(recordings, new LifecycleAwareResponse<ItemsResult>(getLifecycle()) {
                    @Override
                    public void onResponse(ItemsResult response) {
                        if (!getActive()) return;

                        final ItemsResult recordingsResponse = response;
                        final long ticks24 = 1000 * 60 * 60 * 24;

                        // Also get scheduled recordings for next 24 hours
                        final TimerQuery scheduled = new TimerQuery();
                        apiClient.getValue().GetLiveTvTimersAsync(scheduled, new LifecycleAwareResponse<TimerInfoDtoResult>(getLifecycle()) {
                            @Override
                            public void onResponse(TimerInfoDtoResult response) {
                                if (!getActive()) return;

                                List<BaseItemDto> nearTimers = new ArrayList<>();
                                long next24 = Instant.now().toEpochMilli() + ticks24;
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

                                    long past24 = Instant.now().toEpochMilli() - ticks24;
                                    long pastWeek = Instant.now().toEpochMilli() - (ticks24 * 7);
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
                                    mRows.add(new BrowseRowDef(getString(R.string.lbl_recent_recordings), BrowsingUtils.createLiveTVRecordingsRequest(), 50));
                                    rowLoader.loadRows(mRows);

                                    //Now insert our smart rows
                                    if (weekItems.size() > 0) {
                                        ItemRowAdapter weekAdapter = new ItemRowAdapter(requireContext(), JavaCompat.mapBaseItemCollection(weekItems), mCardPresenter, mRowsAdapter, true);
                                        weekAdapter.Retrieve();
                                        ListRow weekRow = new ListRow(new HeaderItem("Past Week"), weekAdapter);
                                        mRowsAdapter.add(0, weekRow);
                                    }
                                    if (nearTimers.size() > 0) {
                                        ItemRowAdapter scheduledAdapter = new ItemRowAdapter(requireContext(), JavaCompat.mapBaseItemCollection(nearTimers), mCardPresenter, mRowsAdapter, true);
                                        scheduledAdapter.Retrieve();
                                        ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours"), scheduledAdapter);
                                        mRowsAdapter.add(0, scheduleRow);
                                    }
                                    if (dayItems.size() > 0) {
                                        ItemRowAdapter dayAdapter = new ItemRowAdapter(requireContext(), JavaCompat.mapBaseItemCollection(dayItems), mCardPresenter, mRowsAdapter, true);
                                        dayAdapter.Retrieve();
                                        ListRow dayRow = new ListRow(new HeaderItem("Past 24 Hours"), dayAdapter);
                                        mRowsAdapter.add(0, dayRow);
                                    }

                                } else {
                                    // no recordings
                                    rowLoader.loadRows(mRows);
                                    if (nearTimers.size() > 0) {
                                        ItemRowAdapter scheduledAdapter = new ItemRowAdapter(requireContext(), JavaCompat.mapBaseItemCollection(nearTimers), mCardPresenter, mRowsAdapter, true);
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
                        if (!getActive()) return;

                        Utils.showToast(getContext(), exception.getLocalizedMessage());
                    }
                });

                break;

            default:
                boolean isRecordingsView = getArguments().getBoolean(Extras.IsLiveTvSeriesRecordings, false);
                if (isRecordingsView) {
                    mRows.add(new BrowseRowDef(getString(R.string.lbl_series_recordings), GetSeriesTimersRequest.INSTANCE));
                    rowLoader.loadRows(mRows);
                }
        }
    }

    @Override
    protected void addAdditionalRows(MutableObjectAdapter<Row> rowAdapter) {
        if (isLiveTvLibrary) {
            //Views row
            HeaderItem gridHeader = new HeaderItem(mRowsAdapter.size(), getString(R.string.lbl_views));

            GridButtonPresenter mGridPresenter = new GridButtonPresenter();
            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
            gridRowAdapter.add(new GridButton(LiveTvOption.LIVE_TV_GUIDE_OPTION_ID, getString(R.string.lbl_live_tv_guide)));
            gridRowAdapter.add(new GridButton(LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID, getString(R.string.lbl_recorded_tv)));
            if (Utils.canManageRecordings(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue())) {
                gridRowAdapter.add(new GridButton(LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID, getString(R.string.lbl_schedule)));
                gridRowAdapter.add(new GridButton(LiveTvOption.LIVE_TV_SERIES_OPTION_ID, getString(R.string.lbl_series)));
            }

            mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        } else {
            super.addAdditionalRows(rowAdapter);
        }
    }
}
