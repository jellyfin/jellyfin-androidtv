package org.jellyfin.androidtv.ui.itemdetail;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewKt;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.InfoItem;
import org.jellyfin.androidtv.data.querying.AdditionalPartsQuery;
import org.jellyfin.androidtv.data.querying.SpecialsQuery;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.querying.TrailersQuery;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.FragmentFullDetailsBinding;
import org.jellyfin.androidtv.preference.SystemPreferences;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.ClockBehavior;
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer;
import org.jellyfin.androidtv.ui.RecordPopup;
import org.jellyfin.androidtv.ui.RecordingIndicatorView;
import org.jellyfin.androidtv.ui.TextUnderButton;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.navigation.Destination;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.ui.playback.VideoQueueManager;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter;
import org.jellyfin.androidtv.ui.presentation.InfoCardPresenter;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.ui.presentation.MyDetailsOverviewRowPresenter;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.MarkdownRenderer;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.EmptyLifecycleAwareResponse;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.androidtv.util.sdk.TrailerUtils;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerQuery;
import org.jellyfin.apiclient.model.querying.EpisodeQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.NextUpQuery;
import org.jellyfin.apiclient.model.querying.SeasonQuery;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;
import org.jellyfin.apiclient.model.querying.UpcomingEpisodesQuery;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.BaseItemPerson;
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto;
import org.jellyfin.sdk.model.api.UserDto;
import org.jellyfin.sdk.model.constant.ItemSortBy;
import org.jellyfin.sdk.model.constant.MediaType;
import org.jellyfin.sdk.model.constant.PersonType;
import org.jellyfin.sdk.model.serializer.UUIDSerializerKt;
import org.koin.java.KoinJavaComponent;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import kotlin.Lazy;
import kotlinx.serialization.json.Json;
import timber.log.Timber;

public class FullDetailsFragment extends Fragment implements RecordingIndicatorView, View.OnKeyListener {

    private int BUTTON_SIZE;

    private TextUnderButton mResumeButton;
    private TextUnderButton mVersionsButton;
    private TextUnderButton mPrevButton;
    private TextUnderButton mRecordButton;
    private TextUnderButton mRecSeriesButton;
    private TextUnderButton mSeriesSettingsButton;
    private TextUnderButton mWatchedToggleButton;

    private DisplayMetrics mMetrics;

    protected org.jellyfin.sdk.model.api.BaseItemDto mProgramInfo;
    protected SeriesTimerInfoDto mSeriesTimerInfo;
    protected String mItemId;
    protected String mChannelId;
    protected BaseRowItem mCurrentItem;
    private Calendar mLastUpdated;
    private String mPrevItemId;

    private RowsSupportFragment mRowsFragment;
    private MutableObjectAdapter<Row> mRowsAdapter;

    private MyDetailsOverviewRowPresenter mDorPresenter;
    private MyDetailsOverviewRow mDetailsOverviewRow;
    private CustomListRowPresenter mListRowPresenter;

    private Handler mLoopHandler = new Handler();
    private Runnable mClockLoop;

    BaseItemDto mBaseItem;

    private ArrayList<MediaSourceInfo> versions;
    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);
    Lazy<SystemPreferences> systemPreferences = inject(SystemPreferences.class);
    private Lazy<DataRefreshService> dataRefreshService = inject(DataRefreshService.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    Lazy<VideoQueueManager> videoQueueManager = inject(VideoQueueManager.class);
    private Lazy<MarkdownRenderer> markdownRenderer = inject(MarkdownRenderer.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    final Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentFullDetailsBinding binding = FragmentFullDetailsBinding.inflate(getLayoutInflater(), container, false);

        BUTTON_SIZE = Utils.convertDpToPixel(requireContext(), 40);

        mMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mRowsFragment = new RowsSupportFragment();
        getChildFragmentManager().beginTransaction().replace(R.id.rowsFragment, mRowsFragment).commit();

        mRowsFragment.setOnItemViewClickedListener(new ItemViewClickedListener());
        mRowsFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());

        mDorPresenter = new MyDetailsOverviewRowPresenter(markdownRenderer.getValue());

        mItemId = getArguments().getString("ItemId");
        mChannelId = getArguments().getString("ChannelId");
        String programJson = getArguments().getString("ProgramInfo");
        if (programJson != null) {
            mProgramInfo =Json.Default.decodeFromString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), programJson);
        }
        String timerJson = getArguments().getString("SeriesTimer");
        if (timerJson != null) {
            mSeriesTimerInfo = Json.Default.decodeFromString(SeriesTimerInfoDto.Companion.serializer(), timerJson);
        }

        CoroutineUtils.readCustomMessagesOnLifecycle(getLifecycle(), customMessageRepository.getValue(), message -> {
            if (message.equals(CustomMessage.ActionComplete.INSTANCE) && mSeriesTimerInfo != null && mBaseItem.getBaseItemType() == BaseItemType.SeriesTimer) {
                //update info
                apiClient.getValue().GetLiveTvSeriesTimerAsync(mSeriesTimerInfo.getId(), new LifecycleAwareResponse<org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto>(getLifecycle()) {
                    @Override
                    public void onResponse(org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto response) {
                        if (!getActive()) return;

                        mSeriesTimerInfo = ModelCompat.asSdk(response);
                        mBaseItem.setOverview(BaseItemUtils.getSeriesOverview(mSeriesTimerInfo, requireContext()));
                        mDorPresenter.getViewHolder().setSummary(mBaseItem.getOverview());
                    }
                });

                mRowsAdapter.clear();
                mRowsAdapter.add(mDetailsOverviewRow);
                //re-retrieve the schedule after giving it a second to rebuild
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

                        addAdditionalRows(mRowsAdapter);

                    }
                }, 1500);
            }
            return null;
        });

        loadItem(mItemId);

        return binding.getRoot();
    }

    private int getResumePreroll() {
        try {
            return Integer.parseInt(KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getResumeSubtractDuration())) * 1000;
        } catch (Exception e) {
            Timber.e(e, "Unable to parse resume preroll");
            return 0;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ClockBehavior clockBehavior = userPreferences.getValue().get(UserPreferences.Companion.getClockBehavior());
        if (clockBehavior == ClockBehavior.ALWAYS || clockBehavior == ClockBehavior.IN_MENUS) {
            startClock();
        }

        //Update information that may have changed - delay slightly to allow changes to take on the server
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

                long lastPlaybackTime = dataRefreshService.getValue().getLastPlayback();
                Timber.d("current time %s last playback event time %s last refresh time %s", System.currentTimeMillis(), lastPlaybackTime, mLastUpdated.getTimeInMillis());

                // if last playback event exists, and event time is greater than last sync or within 2 seconds of current time
                // the third condition accounts for a situation where a sync (dataRefresh) coincides with the end of playback
                if (lastPlaybackTime > 0 && (lastPlaybackTime > mLastUpdated.getTimeInMillis() || System.currentTimeMillis() - lastPlaybackTime < 2000) && ModelCompat.asSdk(mBaseItem).getType() != BaseItemKind.MUSIC_ARTIST) {
                    org.jellyfin.sdk.model.api.BaseItemDto lastPlayedItem = dataRefreshService.getValue().getLastPlayedItem();
                    if (ModelCompat.asSdk(mBaseItem).getType() == BaseItemKind.EPISODE && lastPlayedItem != null && !mBaseItem.getId().equals(lastPlayedItem.getId().toString()) && lastPlayedItem.getType() == BaseItemKind.EPISODE) {
                        Timber.i("Re-loading after new episode playback");
                        loadItem(lastPlayedItem.getId().toString());
                        dataRefreshService.getValue().setLastPlayedItem(null); //blank this out so a detail screen we back up to doesn't also do this
                    } else {
                        Timber.d("Updating info after playback");
                        apiClient.getValue().GetItemAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                            @Override
                            public void onResponse(BaseItemDto response) {
                                if (!getActive()) return;

                                mBaseItem = response;
                                if (mResumeButton != null) {
                                    boolean resumeVisible = (ModelCompat.asSdk(mBaseItem).getType() == BaseItemKind.SERIES && !mBaseItem.getUserData().getPlayed()) || response.getCanResume();
                                    mResumeButton.setVisibility(resumeVisible ? View.VISIBLE : View.GONE);
                                    if (response.getCanResume()) {
                                        mResumeButton.setLabel(getString(R.string.lbl_resume_from, TimeUtils.formatMillis((response.getUserData().getPlaybackPositionTicks()/10000) - getResumePreroll())));
                                    }
                                    if (resumeVisible) {
                                        mResumeButton.requestFocus();
                                    } else if (playButton != null && ViewKt.isVisible(playButton)) {
                                        playButton.requestFocus();
                                    }
                                    showMoreButtonIfNeeded();
                                }
                                updateWatched();
                                mLastUpdated = Calendar.getInstance();
                            }
                        });
                    }
                }
            }
        }, 750);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopClock();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopClock();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;

        if (mCurrentItem != null) {
            return KeyProcessor.HandleKey(keyCode, mCurrentItem, requireActivity());
        } else if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) && BaseItemExtensionsKt.canPlay(ModelCompat.asSdk(mBaseItem))) {
            //default play action
            Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
            play(mBaseItem, pos.intValue() , false);
            return true;
        }

        return false;
    }

    private void startClock() {
        mClockLoop = new Runnable() {
            @Override
            public void run() {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

                if (mBaseItem != null && ((mBaseItem.getRunTimeTicks() != null && mBaseItem.getRunTimeTicks() > 0) || mBaseItem.getOriginalRunTimeTicks() != null)) {
                    mDorPresenter.getViewHolder().setInfoValue3(getEndTime());
                    mLoopHandler.postDelayed(this, 15000);
                }
            }
        };

        mLoopHandler.postDelayed(mClockLoop, 15000);
    }

    private void stopClock() {
        if (mLoopHandler != null && mClockLoop != null) {
            mLoopHandler.removeCallbacks(mClockLoop);
        }
    }

    private static BaseItemKind[] buttonTypes = new BaseItemKind[]{
            BaseItemKind.EPISODE,
            BaseItemKind.MOVIE,
            BaseItemKind.SERIES,
            BaseItemKind.SEASON,
            BaseItemKind.FOLDER,
            BaseItemKind.VIDEO,
            BaseItemKind.RECORDING,
            BaseItemKind.PROGRAM,
            BaseItemKind.TRAILER,
            BaseItemKind.MUSIC_ARTIST,
            BaseItemKind.PERSON,
            BaseItemKind.MUSIC_VIDEO
    };

    private static List<BaseItemKind> buttonTypeList = Arrays.asList(buttonTypes);

    private void updateWatched() {
        if (mWatchedToggleButton != null && mBaseItem != null && mBaseItem.getUserData() != null) {
            mWatchedToggleButton.setActivated(mBaseItem.getUserData().getPlayed());
        }
    }

    private void loadItem(String id) {
        if (mChannelId != null && mProgramInfo == null) {
            // if we are displaying a live tv channel - we want to get whatever is showing now on that channel
            apiClient.getValue().GetLiveTvChannelAsync(mChannelId, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<ChannelInfoDto>(getLifecycle()) {
                @Override
                public void onResponse(ChannelInfoDto response) {
                    if (!getActive()) return;

                    mProgramInfo = ModelCompat.asSdk(response.getCurrentProgram());
                    mItemId = mProgramInfo.getId().toString();
                    apiClient.getValue().GetItemAsync(mItemId, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                        @Override
                        public void onResponse(BaseItemDto response) {
                            if (!getActive()) return;

                            setBaseItem(response);
                        }
                    });
                }
            });
        } else if (mSeriesTimerInfo != null) {
            // create base item from our timer
            BaseItemDto item = new BaseItemDto();
            item.setId(mSeriesTimerInfo.getId());
            item.setBaseItemType(BaseItemType.Folder);
            item.setSeriesTimerId(mSeriesTimerInfo.getId());
            item.setName(mSeriesTimerInfo.getName());
            item.setOverview(BaseItemUtils.getSeriesOverview(mSeriesTimerInfo, requireContext()));

            setBaseItem(item);
        } else {
            apiClient.getValue().GetItemAsync(id, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                @Override
                public void onResponse(BaseItemDto response) {
                    if (!getActive()) return;

                    setBaseItem(response);
                }
            });
        }

        mLastUpdated = Calendar.getInstance();
    }

    @Override
    public void setRecTimer(String id) {
        mProgramInfo = JavaCompat.copyWithTimerId(mProgramInfo, id);
        if (mRecordButton != null) mRecordButton.setActivated(id != null);
    }

    private int posterHeight;

    @Override
    public void setRecSeriesTimer(String id) {
        if (mProgramInfo != null) mProgramInfo = JavaCompat.copyWithTimerId(mProgramInfo, id);
        if (mRecSeriesButton != null) mRecSeriesButton.setActivated(id != null);
        if (mSeriesSettingsButton != null) mSeriesSettingsButton.setVisibility(id == null ? View.GONE : View.VISIBLE);

    }

    private class BuildDorTask extends AsyncTask<BaseItemDto, Integer, MyDetailsOverviewRow> {

        @Override
        protected MyDetailsOverviewRow doInBackground(BaseItemDto... params) {
            BaseItemDto item = params[0];

            // Figure image size
            Double aspect = ImageUtils.getImageAspectRatio(ModelCompat.asSdk(item), false);
            posterHeight = aspect > 1 ? Utils.convertDpToPixel(requireContext(), 160) : Utils.convertDpToPixel(requireContext(), ModelCompat.asSdk(item).getType() == BaseItemKind.PERSON || ModelCompat.asSdk(item).getType() == BaseItemKind.MUSIC_ARTIST ? 300 : 200);

            mDetailsOverviewRow = new MyDetailsOverviewRow(ModelCompat.asSdk(item));

            String primaryImageUrl = ImageUtils.getLogoImageUrl(ModelCompat.asSdk(mBaseItem), 600, true);
            if (primaryImageUrl == null) {
                primaryImageUrl = ImageUtils.getPrimaryImageUrl(ModelCompat.asSdk(mBaseItem), false, null, posterHeight);
                if (item.getRunTimeTicks() != null && item.getRunTimeTicks() > 0 && item.getUserData() != null && item.getUserData().getPlaybackPositionTicks() > 0)
                    mDetailsOverviewRow.setProgress(((int) (item.getUserData().getPlaybackPositionTicks() * 100.0 / item.getRunTimeTicks())));
            }

            mDetailsOverviewRow.setSummary(item.getOverview());
            switch (item.getBaseItemType()) {
                case Person:
                case MusicArtist:
                    break;
                default:

                    BaseItemPerson director = BaseItemExtensionsKt.getFirstPerson(ModelCompat.asSdk(item), PersonType.Director);

                    InfoItem firstRow;
                    if (ModelCompat.asSdk(item).getType() == BaseItemKind.SERIES) {
                        firstRow = new InfoItem(
                                getString(R.string.lbl_seasons),
                                String.format("%d", Utils.getSafeValue(item.getChildCount(), 0)));
                    } else {
                        firstRow = new InfoItem(
                                getString(R.string.lbl_directed_by),
                                director != null ? director.getName() : getString(R.string.lbl_bracket_unknown));
                    }
                    mDetailsOverviewRow.setInfoItem1(firstRow);

                    if ((item.getRunTimeTicks() != null && item.getRunTimeTicks() > 0) || item.getOriginalRunTimeTicks() != null) {
                        mDetailsOverviewRow.setInfoItem2(new InfoItem(getString(R.string.lbl_runs), getRunTime()));
                        ClockBehavior clockBehavior = userPreferences.getValue().get(UserPreferences.Companion.getClockBehavior());
                        if (clockBehavior == ClockBehavior.ALWAYS || clockBehavior == ClockBehavior.IN_MENUS) {
                            mDetailsOverviewRow.setInfoItem3(new InfoItem(getString(R.string.lbl_ends), getEndTime()));
                        } else {
                            mDetailsOverviewRow.setInfoItem3(new InfoItem());
                        }
                    } else {
                        mDetailsOverviewRow.setInfoItem2(new InfoItem());
                        mDetailsOverviewRow.setInfoItem3(new InfoItem());
                    }

            }

            mDetailsOverviewRow.setImageDrawable(primaryImageUrl);

            return mDetailsOverviewRow;
        }

        @Override
        protected void onPostExecute(MyDetailsOverviewRow detailsOverviewRow) {
            super.onPostExecute(detailsOverviewRow);

            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

            ClassPresenterSelector ps = new ClassPresenterSelector();
            ps.addClassPresenter(MyDetailsOverviewRow.class, mDorPresenter);
            mListRowPresenter = new CustomListRowPresenter(Utils.convertDpToPixel(requireContext(), 10));
            ps.addClassPresenter(ListRow.class, mListRowPresenter);
            mRowsAdapter = new MutableObjectAdapter<Row>(ps);
            mRowsFragment.setAdapter(mRowsAdapter);
            mRowsAdapter.add(detailsOverviewRow);

            updateInfo(detailsOverviewRow.getItem());
            addAdditionalRows(mRowsAdapter);

        }
    }

    public void setBaseItem(BaseItemDto item) {
        mBaseItem = item;
        backgroundService.getValue().setBackground(ModelCompat.asSdk(item));
        if (mBaseItem != null) {
            if (mChannelId != null) {
                mBaseItem.setParentId(mChannelId);
                mBaseItem.setPremiereDate(TimeUtils.getDate(mProgramInfo.getStartDate()));
                mBaseItem.setEndDate(TimeUtils.getDate(mProgramInfo.getEndDate(), ZoneId.systemDefault()));
                mBaseItem.setRunTimeTicks(mProgramInfo.getRunTimeTicks());
            }
            new BuildDorTask().execute(item);
        }
    }

    protected void addItemRow(MutableObjectAdapter<Row> parent, ItemRowAdapter row, int index, String headerText) {
        HeaderItem header = new HeaderItem(index, headerText);
        ListRow listRow = new ListRow(header, row);
        parent.add(listRow);
        row.setRow(listRow);
        row.Retrieve();
    }

    protected void addAdditionalRows(MutableObjectAdapter<Row> adapter) {
        Timber.d("Item type: %s", mBaseItem.getBaseItemType().toString());
        switch (mBaseItem.getBaseItemType()) {
            case Movie:

                //Additional Parts
                if (mBaseItem.getPartCount() != null && mBaseItem.getPartCount() > 0) {
                    ItemRowAdapter additionalPartsAdapter = new ItemRowAdapter(requireContext(), new AdditionalPartsQuery(mBaseItem.getId()), new CardPresenter(), adapter);
                    addItemRow(adapter, additionalPartsAdapter, 0, getString(R.string.lbl_additional_parts));
                }

                //Cast/Crew
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(requireContext(), ModelCompat.asSdk(mBaseItem.getPeople()), new CardPresenter(true, 130), adapter);
                    addItemRow(adapter, castAdapter, 1, getString(R.string.lbl_cast_crew));
                }

                //Specials
                if (mBaseItem.getSpecialFeatureCount() != null && mBaseItem.getSpecialFeatureCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(requireContext(), new SpecialsQuery(mBaseItem.getId()), new CardPresenter(), adapter), 3, getString(R.string.lbl_specials));
                }

                //Trailers
                if (mBaseItem.getLocalTrailerCount() != null && mBaseItem.getLocalTrailerCount() > 1) {
                    addItemRow(adapter, new ItemRowAdapter(requireContext(), new TrailersQuery(mBaseItem.getId()), new CardPresenter(), adapter), 4, getString(R.string.lbl_trailers));
                }

                //Chapters
                if (mBaseItem.getChapters() != null && mBaseItem.getChapters().size() > 0) {
                    List<ChapterItemInfo> chapters = BaseItemExtensionsKt.buildChapterItems(ModelCompat.asSdk(mBaseItem), api.getValue());
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(requireContext(), chapters, new CardPresenter(true, 120), adapter);
                    addItemRow(adapter, chapterAdapter, 2, getString(R.string.lbl_chapters));
                }

                //Similar
                SimilarItemsQuery similar = new SimilarItemsQuery();
                similar.setFields(new ItemFields[] {
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                similar.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                similar.setId(mBaseItem.getId());
                similar.setLimit(10);

                ItemRowAdapter similarMoviesAdapter = new ItemRowAdapter(requireContext(), similar, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarMoviesAdapter, 5, getString(R.string.lbl_more_like_this));

                addInfoRows(adapter);
                break;
            case Trailer:

                //Cast/Crew
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(requireContext(), ModelCompat.asSdk(mBaseItem.getPeople()), new CardPresenter(true, 130), adapter);
                    addItemRow(adapter, castAdapter, 0, getString(R.string.lbl_cast_crew));
                }

                //Similar
                SimilarItemsQuery similarTrailer = new SimilarItemsQuery();
                similarTrailer.setFields(new ItemFields[] {
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                similarTrailer.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                similarTrailer.setId(mBaseItem.getId());
                similarTrailer.setLimit(10);

                ItemRowAdapter similarTrailerAdapter = new ItemRowAdapter(requireContext(), similarTrailer, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarTrailerAdapter, 4, getString(R.string.lbl_more_like_this));
                addInfoRows(adapter);
                break;
            case Person:

                ItemQuery personMovies = new ItemQuery();
                personMovies.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                personMovies.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                personMovies.setPersonIds(new String[] {mBaseItem.getId()});
                personMovies.setRecursive(true);
                personMovies.setIncludeItemTypes(new String[] {"Movie"});
                personMovies.setSortBy(new String[] {ItemSortBy.SortName});
                ItemRowAdapter personMoviesAdapter = new ItemRowAdapter(requireContext(), personMovies, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personMoviesAdapter, 0, getString(R.string.lbl_movies));

                ItemQuery personSeries = new ItemQuery();
                personSeries.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                personSeries.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                personSeries.setPersonIds(new String[] {mBaseItem.getId()});
                personSeries.setRecursive(true);
                personSeries.setIncludeItemTypes(new String[] {"Series"});
                personSeries.setSortBy(new String[] {ItemSortBy.SortName});
                ItemRowAdapter personSeriesAdapter = new ItemRowAdapter(requireContext(), personSeries, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personSeriesAdapter, 1, getString(R.string.lbl_tv_series));

                ItemQuery personEpisodes = new ItemQuery();
                personEpisodes.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                personEpisodes.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                personEpisodes.setPersonIds(new String[] {mBaseItem.getId()});
                personEpisodes.setRecursive(true);
                personEpisodes.setIncludeItemTypes(new String[] {"Episode"});
                personEpisodes.setSortBy(new String[] {ItemSortBy.SeriesSortName, ItemSortBy.SortName});
                ItemRowAdapter personEpisodesAdapter = new ItemRowAdapter(requireContext(), personEpisodes, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personEpisodesAdapter, 2, getString(R.string.lbl_episodes));

                break;
            case MusicArtist:

                ItemQuery artistAlbums = new ItemQuery();
                artistAlbums.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                artistAlbums.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                artistAlbums.setArtistIds(new String[]{mBaseItem.getId()});
                artistAlbums.setRecursive(true);
                artistAlbums.setIncludeItemTypes(new String[]{"MusicAlbum"});
                ItemRowAdapter artistAlbumsAdapter = new ItemRowAdapter(requireContext(), artistAlbums, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, artistAlbumsAdapter, 0, getString(R.string.lbl_albums));

                break;
            case Series:
                NextUpQuery nextUpQuery = new NextUpQuery();
                nextUpQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                nextUpQuery.setSeriesId(mBaseItem.getId());
                nextUpQuery.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                ItemRowAdapter nextUpAdapter = new ItemRowAdapter(requireContext(), nextUpQuery, false, new CardPresenter(true, 130), adapter);
                addItemRow(adapter, nextUpAdapter, 0, getString(R.string.lbl_next_up));

                SeasonQuery seasons = new SeasonQuery();
                seasons.setSeriesId(mBaseItem.getId());
                seasons.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                seasons.setFields(new ItemFields[] {
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                ItemRowAdapter seasonsAdapter = new ItemRowAdapter(requireContext(), seasons, new CardPresenter(), adapter);
                addItemRow(adapter, seasonsAdapter, 1, getString(R.string.lbl_seasons));

                //Specials
                if (mBaseItem.getSpecialFeatureCount() != null && mBaseItem.getSpecialFeatureCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(requireContext(), new SpecialsQuery(mBaseItem.getId()), new CardPresenter(), adapter), 3, getString(R.string.lbl_specials));
                }

                UpcomingEpisodesQuery upcoming = new UpcomingEpisodesQuery();
                upcoming.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                upcoming.setParentId(mBaseItem.getId());
                upcoming.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                ItemRowAdapter upcomingAdapter = new ItemRowAdapter(requireContext(), upcoming, new CardPresenter(), adapter);
                addItemRow(adapter, upcomingAdapter, 2, getString(R.string.lbl_upcoming));

                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter seriesCastAdapter = new ItemRowAdapter(requireContext(), ModelCompat.asSdk(mBaseItem.getPeople()), new CardPresenter(true, 130), adapter);
                    addItemRow(adapter, seriesCastAdapter, 3, getString(R.string.lbl_cast_crew));

                }

                SimilarItemsQuery similarSeries = new SimilarItemsQuery();
                similarSeries.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                similarSeries.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                similarSeries.setId(mBaseItem.getId());
                similarSeries.setLimit(20);
                ItemRowAdapter similarAdapter = new ItemRowAdapter(requireContext(), similarSeries, QueryType.SimilarSeries, new CardPresenter(), adapter);
                addItemRow(adapter, similarAdapter, 4, getString(R.string.lbl_more_like_this));
                break;

            case Episode:
                if (mBaseItem.getSeasonId() != null && mBaseItem.getIndexNumber() != null) {
                    StdItemQuery nextEpisodes = new StdItemQuery();
                    nextEpisodes.setParentId(mBaseItem.getSeasonId());
                    nextEpisodes.setIncludeItemTypes(new String[]{"Episode"});
                    nextEpisodes.setStartIndex(mBaseItem.getIndexNumber()); // query index is zero-based but episode no is not
                    nextEpisodes.setLimit(20);
                    ItemRowAdapter nextAdapter = new ItemRowAdapter(requireContext(), nextEpisodes, 0 , false, true, new CardPresenter(true, 120), adapter);
                    addItemRow(adapter, nextAdapter, 5, getString(R.string.lbl_next_episode));
                }

                //Guest stars
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    List<BaseItemPerson> guests = new ArrayList<>();
                    for (BaseItemPerson person : ModelCompat.asSdk(mBaseItem.getPeople())) {
                        if (person.getType() == PersonType.GuestStar) guests.add(person);
                    }
                    if (guests.size() > 0) {
                        ItemRowAdapter castAdapter = new ItemRowAdapter(requireContext(), guests.toArray(new BaseItemPerson[guests.size()]), new CardPresenter(true, 130), adapter);
                        addItemRow(adapter, castAdapter, 0, getString(R.string.lbl_guest_stars));
                    }
                }

                //Chapters
                if (mBaseItem.getChapters() != null && mBaseItem.getChapters().size() > 0) {
                    List<ChapterItemInfo> chapters = BaseItemExtensionsKt.buildChapterItems(ModelCompat.asSdk(mBaseItem), api.getValue());
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(requireContext(), chapters, new CardPresenter(true, 120), adapter);
                    addItemRow(adapter, chapterAdapter, 1, getString(R.string.lbl_chapters));
                }

                addInfoRows(adapter);
                break;

            case SeriesTimer:
                TimerQuery scheduled = new TimerQuery();
                scheduled.setSeriesTimerId(mSeriesTimerInfo.getId());
                TvManager.getScheduleRowsAsync(requireContext(), scheduled, new CardPresenter(true), adapter, new LifecycleAwareResponse<Integer>(getLifecycle()) {});
                break;
        }


    }

    private void addInfoRows(MutableObjectAdapter<Row> adapter) {
        if (KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getDebuggingEnabled()) && mBaseItem.getMediaSources() != null) {
            for (MediaSourceInfo ms : mBaseItem.getMediaSources()) {
                if (ms.getMediaStreams() != null && ms.getMediaStreams().size() > 0) {
                    HeaderItem header = new HeaderItem("Media Details"+(ms.getContainer() != null ? " (" +ms.getContainer()+")" : ""));
                    ArrayObjectAdapter infoAdapter = new ArrayObjectAdapter(new InfoCardPresenter());
                    for (MediaStream stream : ms.getMediaStreams()) {
                        infoAdapter.add(ModelCompat.asSdk(stream));
                    }

                    adapter.add(new ListRow(header, infoAdapter));

                }
            }
        }
    }

    private void updateInfo(org.jellyfin.sdk.model.api.BaseItemDto item) {
        if (buttonTypeList.contains(item.getType())) addButtons(BUTTON_SIZE);

        mLastUpdated = Calendar.getInstance();
    }

    public void setTitle(String title) {
        mDorPresenter.getViewHolder().setTitle(title);
    }

    void playTrailers() {
        // External trailer
        if (mBaseItem.getLocalTrailerCount() == null || mBaseItem.getLocalTrailerCount() < 1) {
            Intent intent = TrailerUtils.getExternalTrailerIntent(requireContext(), ModelCompat.asSdk(mBaseItem));

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException exception) {
                Timber.w(exception, "Unable to open external trailer");
                Utils.showToast(requireContext(), getString(R.string.no_player_message));
            }

            return;
        }

        // Local trailer
        apiClient.getValue().GetLocalTrailersAsync(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), mBaseItem.getId(), new LifecycleAwareResponse<BaseItemDto[]>(getLifecycle()) {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (!getActive()) return;

                play(response, 0, false);
            }

            @Override
            public void onError(Exception exception) {
                if (!getActive()) return;

                Timber.e(exception, "Error retrieving trailers for playback");
                Utils.showToast(requireContext(), R.string.msg_video_playback_error);
            }
        });
    }

    private String getRunTime() {
        Long runtime = Utils.getSafeValue(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
        return runtime != null && runtime > 0 ? String.format("%d%s", (int) Math.ceil((double) runtime / 600000000), getString(R.string.lbl_min)) : "";
    }

    private String getEndTime() {
        if (mBaseItem != null && ModelCompat.asSdk(mBaseItem).getType() != BaseItemKind.MUSIC_ARTIST && ModelCompat.asSdk(mBaseItem).getType() != BaseItemKind.PERSON) {
            Long runtime = Utils.getSafeValue(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
            if (runtime != null && runtime > 0) {
                long endTimeTicks = ModelCompat.asSdk(mBaseItem).getType() == BaseItemKind.PROGRAM && mBaseItem.getEndDate() != null ? TimeUtils.convertToLocalDate(mBaseItem.getEndDate()).getTime() : System.currentTimeMillis() + runtime / 10000;
                if (mBaseItem.getCanResume()) {
                    endTimeTicks = System.currentTimeMillis() + ((runtime - mBaseItem.getUserData().getPlaybackPositionTicks()) / 10000);
                }
                return android.text.format.DateFormat.getTimeFormat(requireContext()).format(new Date(endTimeTicks));
            }

        }
        return "";
    }

    void addItemToQueue() {
        org.jellyfin.sdk.model.api.BaseItemDto baseItem = ModelCompat.asSdk(mBaseItem);
        if (baseItem.getType() == BaseItemKind.AUDIO || baseItem.getType() == BaseItemKind.MUSIC_ALBUM || baseItem.getType() == BaseItemKind.MUSIC_ARTIST) {
            if (baseItem.getType() == BaseItemKind.MUSIC_ALBUM || baseItem.getType() == BaseItemKind.MUSIC_ARTIST) {
                PlaybackHelper.getItemsToPlay(baseItem, false, false, new LifecycleAwareResponse<List<org.jellyfin.sdk.model.api.BaseItemDto>>(getLifecycle()) {
                    @Override
                    public void onResponse(List<org.jellyfin.sdk.model.api.BaseItemDto> response) {
                        if (!getActive()) return;

                        mediaManager.getValue().addToAudioQueue(response);
                    }
                });
            } else {
                mediaManager.getValue().addToAudioQueue(Arrays.asList(baseItem));
            }
        }
    }

    void toggleFavorite() {
        UserItemDataDto data = mBaseItem.getUserData();
        apiClient.getValue().UpdateFavoriteStatusAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), !data.getIsFavorite(), new LifecycleAwareResponse<UserItemDataDto>(getLifecycle()) {
            @Override
            public void onResponse(UserItemDataDto response) {
                if (!getActive()) return;

                mBaseItem.setUserData(response);
                favButton.setActivated(response.getIsFavorite());
                dataRefreshService.getValue().setLastFavoriteUpdate(System.currentTimeMillis());
            }
        });
    }

    void gotoSeries() {
        navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(UUIDSerializerKt.toUUID(mBaseItem.getSeriesId())));
    }

    private void deleteItem() {
        Timber.i("Showing item delete confirmation");
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.item_delete_confirm_title))
                .setMessage(getString(R.string.item_delete_confirm_message))
                .setNegativeButton(R.string.lbl_no, null)
                .setPositiveButton(R.string.lbl_delete, (dialog, which) -> {
                    FullDetailsFragmentHelperKt.deleteItem(
                            this,
                            api.getValue(),
                            ModelCompat.asSdk(mBaseItem),
                            dataRefreshService.getValue(),
                            navigationRepository.getValue()
                    );
                })
                .show();
    }

    TextUnderButton favButton = null;
    TextUnderButton shuffleButton = null;
    TextUnderButton goToSeriesButton = null;
    TextUnderButton queueButton = null;
    TextUnderButton deleteButton = null;
    TextUnderButton moreButton;
    TextUnderButton playButton = null;
    TextUnderButton trailerButton = null;

    private void addButtons(int buttonSize) {
        org.jellyfin.sdk.model.api.BaseItemDto baseItem = ModelCompat.asSdk(mBaseItem);
        String buttonLabel;
        if (baseItem.getType() == BaseItemKind.SERIES) {
            buttonLabel = getString(R.string.lbl_play_next_up);
        } else {
            long startPos = 0;
            if (mBaseItem.getCanResume()) {
                startPos = (mBaseItem.getUserData().getPlaybackPositionTicks()/10000) - getResumePreroll();
            }
            buttonLabel = getString(R.string.lbl_resume_from, TimeUtils.formatMillis(startPos));
        }
        mResumeButton = TextUnderButton.create(requireContext(), R.drawable.ic_resume, buttonSize, 2, buttonLabel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baseItem.getType() == BaseItemKind.SERIES) {
                    //play next up
                    NextUpQuery nextUpQuery = new NextUpQuery();
                    nextUpQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                    nextUpQuery.setSeriesId(mBaseItem.getId());
                    apiClient.getValue().GetNextUpEpisodesAsync(nextUpQuery, new LifecycleAwareResponse<ItemsResult>(getLifecycle()) {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (!getActive()) return;

                            if (response.getItems().length > 0) {
                                play(response.getItems()[0], 0 , false);
                            } else {
                                Utils.showToast(requireContext(), "Unable to find next up episode");
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            if (!getActive()) return;

                            Timber.e(exception, "Error playing next up episode");
                            Utils.showToast(requireContext(), getString(R.string.msg_video_playback_error));
                        }
                    });
                } else {
                    //resume
                    Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
                    play(mBaseItem, pos.intValue() - getResumePreroll(), false);

                }
            }
        });

        //playButton becomes playWith button
        if (userPreferences.getValue().get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.CHOOSE && (baseItem.getType() == BaseItemKind.SERIES || baseItem.getType() == BaseItemKind.MOVIE || baseItem.getType() == BaseItemKind.VIDEO || baseItem.getType() == BaseItemKind.EPISODE)) {
            playButton = TextUnderButton.create(requireContext(), R.drawable.ic_play, buttonSize, 3, getString(R.string.play_with), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FullDetailsFragmentHelperKt.showPlayWithMenu(FullDetailsFragment.this, view, false);
                }
            });
            mDetailsOverviewRow.addAction(playButton);

            if (mBaseItem.getIsFolderItem()) {
                shuffleButton = TextUnderButton.create(requireContext(), R.drawable.ic_shuffle, buttonSize, 2, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FullDetailsFragmentHelperKt.showPlayWithMenu(FullDetailsFragment.this, view, true);
                    }
                });
                mDetailsOverviewRow.addAction(shuffleButton);
            }
        } else { //here playButton is only a play button
            if (BaseItemExtensionsKt.canPlay(baseItem)) {
                mDetailsOverviewRow.addAction(mResumeButton);
                boolean resumeButtonVisible = (baseItem.getType() == BaseItemKind.SERIES && !mBaseItem.getUserData().getPlayed()) || (mBaseItem.getCanResume());
                mResumeButton.setVisibility(resumeButtonVisible ? View.VISIBLE : View.GONE);

                playButton = TextUnderButton.create(requireContext(), R.drawable.ic_play, buttonSize, 2, getString(BaseItemExtensionsKt.isLiveTv(ModelCompat.asSdk(mBaseItem)) ? R.string.lbl_tune_to_channel : mBaseItem.getIsFolderItem() ? R.string.lbl_play_all : R.string.lbl_play), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        play(mBaseItem, 0, false);
                    }
                });

                mDetailsOverviewRow.addAction(playButton);

                if (resumeButtonVisible) {
                    mResumeButton.requestFocus();
                } else {
                    playButton.requestFocus();
                }

                boolean isMusic = baseItem.getType() == BaseItemKind.MUSIC_ALBUM
                        || baseItem.getType() == BaseItemKind.MUSIC_ARTIST
                        || baseItem.getType() == BaseItemKind.AUDIO
                        || (baseItem.getType() == BaseItemKind.PLAYLIST && MediaType.Audio.equals(mBaseItem.getMediaType()));

                if (isMusic) {
                    queueButton = TextUnderButton.create(requireContext(), R.drawable.ic_add, buttonSize, 2, getString(R.string.lbl_add_to_queue), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addItemToQueue();
                        }
                    });
                    mDetailsOverviewRow.addAction(queueButton);
                }

                if (mBaseItem.getIsFolderItem() || baseItem.getType() == BaseItemKind.MUSIC_ARTIST) {
                    shuffleButton = TextUnderButton.create(requireContext(), R.drawable.ic_shuffle, buttonSize, 2, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            play(mBaseItem, 0, true);
                        }
                    });
                    mDetailsOverviewRow.addAction(shuffleButton);
                }

                if (baseItem.getType() == BaseItemKind.MUSIC_ARTIST) {
                    TextUnderButton imix = TextUnderButton.create(requireContext(), R.drawable.ic_mix, buttonSize, 0, getString(R.string.lbl_instant_mix), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PlaybackHelper.playInstantMix(requireContext(), baseItem);
                        }
                    });
                    mDetailsOverviewRow.addAction(imix);
                }

            }
        }
        //Video versions button
        if (mBaseItem.getMediaSources() != null && mBaseItem.getMediaSources().size() > 1) {
            mVersionsButton = TextUnderButton.create(requireContext(), R.drawable.ic_guide, buttonSize, 0, getString(R.string.select_version), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (versions != null ) {
                        addVersionsMenu(v);
                    } else {
                        versions = mBaseItem.getMediaSources();
                        addVersionsMenu(v);
                    }
                }
            });
            mDetailsOverviewRow.addAction(mVersionsButton);
        }

        if (TrailerUtils.hasPlayableTrailers(requireContext(), ModelCompat.asSdk(mBaseItem))) {
            trailerButton = TextUnderButton.create(requireContext(), R.drawable.ic_trailer, buttonSize, 0, getString(R.string.lbl_play_trailers), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playTrailers();
                }
            });

            mDetailsOverviewRow.addAction(trailerButton);
        }

        if (mProgramInfo != null && Utils.canManageRecordings(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue())) {
            if (TimeUtils.convertToLocalDate(mBaseItem.getEndDate()).getTime() > System.currentTimeMillis()) {
                //Record button
                mRecordButton = TextUnderButton.create(requireContext(), R.drawable.ic_record, buttonSize, 4, getString(R.string.lbl_record), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getTimerId() == null) {
                            //Create one-off recording with defaults
                            apiClient.getValue().GetDefaultLiveTvTimerInfo(mProgramInfo.getId().toString(), new LifecycleAwareResponse<org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto>(getLifecycle()) {
                                @Override
                                public void onResponse(org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto response) {
                                    if (!getActive()) return;

                                    apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyLifecycleAwareResponse(getLifecycle()) {
                                        @Override
                                        public void onResponse() {
                                            if (!getActive()) return;

                                            // we have to re-retrieve the program to get the timer id
                                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                                                @Override
                                                public void onResponse(BaseItemDto response) {
                                                    if (!getActive()) return;

                                                    mProgramInfo = ModelCompat.asSdk(response);
                                                    setRecSeriesTimer(response.getSeriesTimerId());
                                                    setRecTimer(response.getTimerId());
                                                    Utils.showToast(requireContext(), R.string.msg_set_to_record);

                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            if (!getActive()) return;

                                            Timber.e(ex, "Error creating recording");
                                            Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception exception) {
                                    if (!getActive()) return;

                                    Timber.e(exception, "Error creating recording");
                                    Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                                }
                            });
                        } else {
                            apiClient.getValue().CancelLiveTvTimerAsync(mProgramInfo.getTimerId(), new EmptyLifecycleAwareResponse(getLifecycle()) {
                                @Override
                                public void onResponse() {
                                    if (!getActive()) return;

                                    setRecTimer(null);
                                    dataRefreshService.getValue().setLastDeletedItemId(mProgramInfo.getId());
                                    Utils.showToast(requireContext(), R.string.msg_recording_cancelled);
                                }

                                @Override
                                public void onError(Exception ex) {
                                    if (!getActive()) return;

                                    Utils.showToast(requireContext(), R.string.msg_unable_to_cancel);
                                }
                            });

                        }
                    }
                });
                mRecordButton.setActivated(mProgramInfo.getTimerId() != null);

                mDetailsOverviewRow.addAction(mRecordButton);
            }

            if (mProgramInfo.isSeries() != null && mProgramInfo.isSeries()) {
                mRecSeriesButton= TextUnderButton.create(requireContext(), R.drawable.ic_record_series, buttonSize, 4, getString(R.string.lbl_record_series), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getSeriesTimerId() == null) {
                            //Create series recording with default options
                            apiClient.getValue().GetDefaultLiveTvTimerInfo(mProgramInfo.getId().toString(), new LifecycleAwareResponse<org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto>(getLifecycle()) {
                                @Override
                                public void onResponse(org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto response) {
                                    apiClient.getValue().CreateLiveTvSeriesTimerAsync(response, new EmptyLifecycleAwareResponse(getLifecycle()) {
                                        @Override
                                        public void onResponse() {
                                            if (!getActive()) return;

                                            // we have to re-retrieve the program to get the timer id
                                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                                                @Override
                                                public void onResponse(BaseItemDto response) {
                                                    if (!getActive()) return;

                                                    mProgramInfo = ModelCompat.asSdk(response);
                                                    setRecSeriesTimer(response.getSeriesTimerId());
                                                    setRecTimer(response.getTimerId());
                                                    Utils.showToast(requireContext(), R.string.msg_set_to_record);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            if (!getActive()) return;

                                            Timber.e(ex, "Error creating recording");
                                            Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception exception) {
                                    if (!getActive()) return;

                                    Timber.e(exception, "Error creating recording");
                                    Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                                }
                            });

                        } else {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(getString(R.string.lbl_cancel_series))
                                    .setMessage(getString(R.string.msg_cancel_entire_series))
                                    .setNegativeButton(R.string.lbl_no, null)
                                    .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            apiClient.getValue().CancelLiveTvSeriesTimerAsync(mProgramInfo.getSeriesTimerId(), new EmptyLifecycleAwareResponse(getLifecycle()) {
                                                @Override
                                                public void onResponse() {
                                                    if (!getActive()) return;

                                                    setRecSeriesTimer(null);
                                                    setRecTimer(null);
                                                    dataRefreshService.getValue().setLastDeletedItemId(mProgramInfo.getId());
                                                    Utils.showToast(requireContext(), R.string.msg_recording_cancelled);
                                                }

                                                @Override
                                                public void onError(Exception ex) {
                                                    if (!getActive()) return;

                                                    Utils.showToast(requireContext(), R.string.msg_unable_to_cancel);
                                                }
                                            });
                                        }
                                    }).show();

                        }
                    }
                });
                mRecSeriesButton.setActivated(mProgramInfo.getSeriesTimerId() != null);

                mDetailsOverviewRow.addAction(mRecSeriesButton);

                mSeriesSettingsButton = TextUnderButton.create(requireContext(), R.drawable.ic_settings, buttonSize, 2, getString(R.string.lbl_series_settings), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showRecordingOptions(mProgramInfo.getSeriesTimerId(), mProgramInfo, true);
                    }
                });

                mSeriesSettingsButton.setVisibility(mProgramInfo.getSeriesTimerId() != null ? View.VISIBLE : View.GONE);

                mDetailsOverviewRow.addAction(mSeriesSettingsButton);
            }
        }

        UserItemDataDto userData = mBaseItem.getUserData();
        if (userData != null && mProgramInfo == null) {
            if (ModelCompat.asSdk(mBaseItem).getType() != BaseItemKind.MUSIC_ARTIST && ModelCompat.asSdk(mBaseItem).getType() != BaseItemKind.PERSON) {
                mWatchedToggleButton = TextUnderButton.create(requireContext(), R.drawable.ic_watch, buttonSize, 0, getString(R.string.lbl_watched), markWatchedListener);
                mWatchedToggleButton.setActivated(userData.getPlayed());
                mDetailsOverviewRow.addAction(mWatchedToggleButton);
            }

            //Favorite
            favButton = TextUnderButton.create(requireContext(), R.drawable.ic_heart, buttonSize, 2, getString(R.string.lbl_favorite), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    toggleFavorite();
                }
            });
            favButton.setActivated(userData.getIsFavorite());
            mDetailsOverviewRow.addAction(favButton);
        }

        if (ModelCompat.asSdk(mBaseItem).getType() == BaseItemKind.EPISODE && mBaseItem.getSeriesId() != null) {
            //add the prev button first so it will be there in proper position - we'll show it later if needed
            mPrevButton = TextUnderButton.create(requireContext(), R.drawable.ic_previous_episode, buttonSize, 3, getString(R.string.lbl_previous_episode), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPrevItemId != null) {
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(UUIDSerializerKt.toUUID(mPrevItemId)));
                    }
                }
            });

            mDetailsOverviewRow.addAction(mPrevButton);

            //now go get our prev episode id
            EpisodeQuery adjacent = new EpisodeQuery();
            adjacent.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
            adjacent.setSeriesId(mBaseItem.getSeriesId());
            adjacent.setAdjacentTo(mBaseItem.getId());
            apiClient.getValue().GetEpisodesAsync(adjacent, new LifecycleAwareResponse<ItemsResult>(getLifecycle()) {
                @Override
                public void onResponse(ItemsResult response) {
                    if (!getActive()) return;

                    if (response.getTotalRecordCount() > 0) {
                        //Just look at first item - if it isn't us, then it is the prev episode
                        if (!mBaseItem.getId().equals(response.getItems()[0].getId())) {
                            mPrevItemId = response.getItems()[0].getId();
                            mPrevButton.setVisibility(View.VISIBLE);
                        } else {
                            mPrevButton.setVisibility(View.GONE);
                        }
                    }
                    showMoreButtonIfNeeded();
                }
            });

            goToSeriesButton = TextUnderButton.create(requireContext(), R.drawable.ic_tv, buttonSize, 0, getString(R.string.lbl_goto_series), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotoSeries();
                }
            });
            mDetailsOverviewRow.addAction(goToSeriesButton);
        }

        if (userPreferences.getValue().get(UserPreferences.Companion.getMediaManagementEnabled())) {
            boolean deletableItem = false;
            UserDto currentUser = KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue();
            if (mBaseItem.getBaseItemType() == BaseItemType.Recording && currentUser.getPolicy().getEnableLiveTvManagement() && mBaseItem.getCanDelete())
                deletableItem = true;
            else if (mBaseItem.getCanDelete()) deletableItem = true;

            if (deletableItem) {
                deleteButton = TextUnderButton.create(requireContext(), R.drawable.ic_delete, buttonSize, 0, getString(R.string.lbl_delete), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteItem();
                    }
                });
                mDetailsOverviewRow.addAction(deleteButton);
            }
        }

        if (mSeriesTimerInfo != null && mBaseItem.getBaseItemType() == BaseItemType.SeriesTimer) {
            //Settings
            mDetailsOverviewRow.addAction(TextUnderButton.create(requireContext(), R.drawable.ic_settings, buttonSize, 0, getString(R.string.lbl_series_settings), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //show recording options
                    showRecordingOptions(mSeriesTimerInfo.getId(), ModelCompat.asSdk(mBaseItem), true);
                }
            }));

            //Delete
            TextUnderButton del = TextUnderButton.create(requireContext(), R.drawable.ic_trash, buttonSize, 0, getString(R.string.lbl_cancel_series), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.lbl_delete)
                            .setMessage(getString(R.string.msg_cancel_entire_series))
                            .setPositiveButton(R.string.lbl_cancel_series, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    apiClient.getValue().CancelLiveTvSeriesTimerAsync(mSeriesTimerInfo.getId(), new EmptyLifecycleAwareResponse(getLifecycle()) {
                                        @Override
                                        public void onResponse() {
                                            if (!getActive()) return;

                                            Utils.showToast(requireContext(), mSeriesTimerInfo.getName() + " Canceled");
                                            dataRefreshService.getValue().setLastDeletedItemId(UUIDSerializerKt.toUUID(mSeriesTimerInfo.getId()));
                                            if (navigationRepository.getValue().getCanGoBack()) {
                                                navigationRepository.getValue().goBack();
                                            } else {
                                                navigationRepository.getValue().reset(Destinations.INSTANCE.getHome());
                                            }
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            if (!getActive()) return;

                                            Utils.showToast(requireContext(), ex.getLocalizedMessage());
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(R.string.lbl_no, null)
                            .show();

                }
            });
            mDetailsOverviewRow.addAction(del);

        }

        //Now, create a more button to show if needed
        moreButton = TextUnderButton.create(requireContext(), R.drawable.ic_more, buttonSize, 0, getString(R.string.lbl_other_options), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullDetailsFragmentHelperKt.showDetailsMenu(FullDetailsFragment.this, v, ModelCompat.asSdk(mBaseItem));
            }
        });

        moreButton.setVisibility(View.GONE);
        mDetailsOverviewRow.addAction(moreButton);
        if (ModelCompat.asSdk(mBaseItem).getType() != BaseItemKind.EPISODE) showMoreButtonIfNeeded();  //Episodes check for previous and then call this above
    }

    private void addVersionsMenu(View v) {
        PopupMenu menu = new PopupMenu(requireContext(), v, Gravity.END);

        for (int i = 0; i< versions.size(); i++) {
            MenuItem item = menu.getMenu().add(Menu.NONE, i, Menu.NONE, versions.get(i).getName());
            item.setChecked(i == mDetailsOverviewRow.getSelectedMediaSourceIndex());
        }

        menu.getMenu().setGroupCheckable(0,true,false);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mDetailsOverviewRow.setSelectedMediaSourceIndex(menuItem.getItemId());
                apiClient.getValue().GetItemAsync(versions.get(mDetailsOverviewRow.getSelectedMediaSourceIndex()).getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (!getActive()) return;

                        mBaseItem = response;
                        mDorPresenter.getViewHolder().setItem(mDetailsOverviewRow);
                        if (mVersionsButton != null) {
                            mVersionsButton.requestFocus();
                        }
                    }
                });
                return true;
            }
        });

        menu.show();
    }

    int collapsedOptions = 0 ;

    private void showMoreButtonIfNeeded() {
        int visibleOptions = mDetailsOverviewRow.getVisibleActions();

        List<TextUnderButton> actionsList = new ArrayList<>();
        // added in order of priority (should match res/menu/menu_details_more.xml)
        if (queueButton != null) actionsList.add(queueButton);
        if (shuffleButton != null) actionsList.add(shuffleButton);
        if (trailerButton != null) actionsList.add(trailerButton);
        if (favButton != null) actionsList.add(favButton);
        if (goToSeriesButton != null) actionsList.add(goToSeriesButton);

        // reverse the list so the less important actions are hidden first
        Collections.reverse(actionsList);

        collapsedOptions = 0;
        for (TextUnderButton action : actionsList) {
            if (visibleOptions - (ViewKt.isVisible(action) ? 1 : 0) + (!ViewKt.isVisible(moreButton) && collapsedOptions > 0 ? 1 : 0) < 5) {
                if (!ViewKt.isVisible(action)) {
                    action.setVisibility(View.VISIBLE);
                    visibleOptions++;
                }
            } else {
                if (ViewKt.isVisible(action)) {
                    action.setVisibility(View.GONE);
                    visibleOptions--;
                }
                collapsedOptions++;
            }
        }
        moreButton.setVisibility(collapsedOptions > 0 ? View.VISIBLE : View.GONE);
    }

    RecordPopup mRecordPopup;
    public void showRecordingOptions(String id, final org.jellyfin.sdk.model.api.BaseItemDto program, final boolean recordSeries) {
        if (mRecordPopup == null) {
            int width = Utils.convertDpToPixel(requireContext(), 600);
            Point size = new Point();
            requireActivity().getWindowManager().getDefaultDisplay().getSize(size);
            mRecordPopup = new RecordPopup(requireActivity(), getLifecycle(), mRowsFragment.getView(), (size.x/2) - (width/2), mRowsFragment.getView().getTop()+40, width);
        }
        apiClient.getValue().GetLiveTvSeriesTimerAsync(id, new LifecycleAwareResponse<org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto>(getLifecycle()) {
            @Override
            public void onResponse(org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto response) {
                if (!getActive()) return;

                if (recordSeries || Utils.isTrue(program.isSports())) {
                    mRecordPopup.setContent(requireContext(), program, response, FullDetailsFragment.this, recordSeries);
                    mRecordPopup.show();
                } else {
                    //just record with defaults
                    apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyLifecycleAwareResponse(getLifecycle()) {
                        @Override
                        public void onResponse() {
                            if (!getActive()) return;

                            // we have to re-retrieve the program to get the timer id
                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    setRecTimer(response.getTimerId());
                                }
                            });
                            Utils.showToast(requireContext(), R.string.msg_set_to_record);
                        }

                        @Override
                        public void onError(Exception ex) {
                            if (!getActive()) return;

                            Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                        }
                    });

                }
            }
        });
    }



    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(), ((BaseRowItem)item).getIndex(), requireContext());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
            } else {
                mCurrentItem = (BaseRowItem)item;
            }
        }
    }

    private View.OnClickListener markWatchedListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final UserItemDataDto data = mBaseItem.getUserData();
            if (mBaseItem.getIsFolderItem()) {
                if (data.getPlayed())
                    markUnPlayed();
                else
                    markPlayed();
            } else {
                if (data.getPlayed()) {
                    markUnPlayed();
                } else {
                    markPlayed();
                }
            }

        }
    };

    private void markPlayed() {
        apiClient.getValue().MarkPlayedAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), null, new LifecycleAwareResponse<UserItemDataDto>(getLifecycle()) {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                mWatchedToggleButton.setActivated(true);
                //adjust resume
                if (mResumeButton != null && !mBaseItem.getCanResume())
                    mResumeButton.setVisibility(View.GONE);
                //force lists to re-fetch
                dataRefreshService.getValue().setLastPlayback(System.currentTimeMillis());
                switch (mBaseItem.getType()) {
                    case "Movie":
                        dataRefreshService.getValue().setLastMoviePlayback(System.currentTimeMillis());
                        break;
                    case "Episode":
                        dataRefreshService.getValue().setLastTvPlayback(System.currentTimeMillis());
                        break;
                }
                showMoreButtonIfNeeded();
            }
        });

    }

    private void markUnPlayed() {
        apiClient.getValue().MarkUnplayedAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<UserItemDataDto>(getLifecycle()) {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                mWatchedToggleButton.setActivated(false);
                //adjust resume
                if (mResumeButton != null && !mBaseItem.getCanResume())
                    mResumeButton.setVisibility(View.GONE);
                //force lists to re-fetch
                dataRefreshService.getValue().setLastPlayback(System.currentTimeMillis());
                switch (mBaseItem.getType()) {
                    case "Movie":
                        dataRefreshService.getValue().setLastMoviePlayback(System.currentTimeMillis());
                        break;
                    case "Episode":
                        dataRefreshService.getValue().setLastTvPlayback(System.currentTimeMillis());
                        break;
                }
                showMoreButtonIfNeeded();
            }
        });

    }

    void shufflePlay() {
        play(mBaseItem, 0, true);
    }

    protected void play(final BaseItemDto item, final int pos, final boolean shuffle) {
        PlaybackHelper.getItemsToPlay(ModelCompat.asSdk(item), pos == 0 && ModelCompat.asSdk(item).getType() == BaseItemKind.MOVIE, shuffle, new LifecycleAwareResponse<List<org.jellyfin.sdk.model.api.BaseItemDto>>(getLifecycle()) {
            @Override
            public void onResponse(List<org.jellyfin.sdk.model.api.BaseItemDto> response) {
                if (!getActive()) return;

                PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
                if (playbackLauncher.interceptPlayRequest(requireContext(), ModelCompat.asSdk(item))) return;

                if (ModelCompat.asSdk(item).getType() == BaseItemKind.MUSIC_ARTIST) {
                    mediaManager.getValue().playNow(requireContext(), response, shuffle);
                } else {
                    videoQueueManager.getValue().setCurrentVideoQueue(response);
                    Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(ModelCompat.asSdk(item).getType(), pos);
                    navigationRepository.getValue().navigate(destination);
                }
            }
        });

    }

    protected void play(final BaseItemDto[] items, final int pos, final boolean shuffle) {
        List<BaseItemDto> itemsToPlay = Arrays.asList(items);
        if (shuffle) Collections.shuffle(itemsToPlay);
        videoQueueManager.getValue().setCurrentVideoQueue(JavaCompat.mapBaseItemCollection(itemsToPlay));
        Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(ModelCompat.asSdk(items[0]).getType(), pos);
        navigationRepository.getValue().navigate(destination);
    }
}
