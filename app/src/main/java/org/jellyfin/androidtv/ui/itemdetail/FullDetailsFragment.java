package org.jellyfin.androidtv.ui.itemdetail;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import org.jellyfin.androidtv.ui.browsing.BrowsingUtils;
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
import org.jellyfin.androidtv.util.ImageHelper;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.MarkdownRenderer;
import org.jellyfin.androidtv.util.PlaybackHelper;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.EmptyLifecycleAwareResponse;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.androidtv.util.sdk.TrailerUtils;
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.BaseItemPerson;
import org.jellyfin.sdk.model.api.ItemSortBy;
import org.jellyfin.sdk.model.api.MediaSourceInfo;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaType;
import org.jellyfin.sdk.model.api.PersonKind;
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto;
import org.jellyfin.sdk.model.api.UserDto;
import org.jellyfin.sdk.model.serializer.UUIDSerializerKt;
import org.koin.java.KoinJavaComponent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import kotlin.Lazy;
import kotlinx.serialization.json.Json;
import timber.log.Timber;

public class FullDetailsFragment extends Fragment implements RecordingIndicatorView, View.OnKeyListener {

    private int BUTTON_SIZE;

    TextUnderButton mResumeButton;
    private TextUnderButton mVersionsButton;
    TextUnderButton mPrevButton;
    private TextUnderButton mRecordButton;
    private TextUnderButton mRecSeriesButton;
    private TextUnderButton mSeriesSettingsButton;
    TextUnderButton mWatchedToggleButton;

    private DisplayMetrics mMetrics;

    protected BaseItemDto mProgramInfo;
    protected SeriesTimerInfoDto mSeriesTimerInfo;
    protected UUID mItemId;
    protected UUID mChannelId;
    protected BaseRowItem mCurrentItem;
    private Instant mLastUpdated;
    public UUID mPrevItemId;

    private RowsSupportFragment mRowsFragment;
    private MutableObjectAdapter<Row> mRowsAdapter;

    private MyDetailsOverviewRowPresenter mDorPresenter;
    private MyDetailsOverviewRow mDetailsOverviewRow;
    private CustomListRowPresenter mListRowPresenter;

    private Handler mLoopHandler = new Handler();
    private Runnable mClockLoop;

    BaseItemDto mBaseItem;

    private ArrayList<MediaSourceInfo> versions;
    private final Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private final Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private final Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);
    final Lazy<SystemPreferences> systemPreferences = inject(SystemPreferences.class);
    private final Lazy<DataRefreshService> dataRefreshService = inject(DataRefreshService.class);
    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    final Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    final Lazy<VideoQueueManager> videoQueueManager = inject(VideoQueueManager.class);
    private final Lazy<MarkdownRenderer> markdownRenderer = inject(MarkdownRenderer.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    final Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);
    private final Lazy<ItemLauncher> itemLauncher = inject(ItemLauncher.class);
    private final Lazy<KeyProcessor> keyProcessor = inject(KeyProcessor.class);
    final Lazy<PlaybackHelper> playbackHelper = inject(PlaybackHelper.class);
    private final Lazy<ImageHelper> imageHelper = inject(ImageHelper.class);

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

        mItemId = Utils.uuidOrNull(getArguments().getString("ItemId"));
        mChannelId = Utils.uuidOrNull(getArguments().getString("ChannelId"));
        String programJson = getArguments().getString("ProgramInfo");
        if (programJson != null) {
            mProgramInfo = Json.Default.decodeFromString(BaseItemDto.Companion.serializer(), programJson);
        }
        String timerJson = getArguments().getString("SeriesTimer");
        if (timerJson != null) {
            mSeriesTimerInfo = Json.Default.decodeFromString(SeriesTimerInfoDto.Companion.serializer(), timerJson);
        }

        CoroutineUtils.readCustomMessagesOnLifecycle(getLifecycle(), customMessageRepository.getValue(), message -> {
            if (message.equals(CustomMessage.ActionComplete.INSTANCE) && mSeriesTimerInfo != null) {
                //update info
                apiClient.getValue().GetLiveTvSeriesTimerAsync(mSeriesTimerInfo.getId(), new LifecycleAwareResponse<org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto>(getLifecycle()) {
                    @Override
                    public void onResponse(org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto response) {
                        if (!getActive()) return;

                        mSeriesTimerInfo = ModelCompat.asSdk(response);
                        mBaseItem = JavaCompat.copyWithOverview(mBaseItem, BaseItemUtils.getSeriesOverview(mSeriesTimerInfo, requireContext()));
                        mDorPresenter.getViewHolder().setSummary(mBaseItem.getOverview());
                    }
                });

                mRowsAdapter.clear();
                mRowsAdapter.add(mDetailsOverviewRow);
                //re-retrieve the schedule after giving it a second to rebuild
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                            return;

                        addAdditionalRows(mRowsAdapter);

                    }
                }, 1500);
            }
            return null;
        });

        loadItem(mItemId);

        return binding.getRoot();
    }

    int getResumePreroll() {
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

                Instant lastPlaybackTime = dataRefreshService.getValue().getLastPlayback();
                Timber.d("current time %s last playback event time %s last refresh time %s", Instant.now().toEpochMilli(), lastPlaybackTime, mLastUpdated.toEpochMilli());

                // if last playback event exists, and event time is greater than last sync or within 2 seconds of current time
                // the third condition accounts for a situation where a sync (dataRefresh) coincides with the end of playback
                if (lastPlaybackTime != null && (lastPlaybackTime.isAfter(mLastUpdated) || Instant.now().toEpochMilli() - lastPlaybackTime.toEpochMilli() < 2000) && mBaseItem.getType() != BaseItemKind.MUSIC_ARTIST) {
                    BaseItemDto lastPlayedItem = dataRefreshService.getValue().getLastPlayedItem();
                    if (mBaseItem.getType() == BaseItemKind.EPISODE && lastPlayedItem != null && !mBaseItem.getId().equals(lastPlayedItem.getId().toString()) && lastPlayedItem.getType() == BaseItemKind.EPISODE) {
                        Timber.i("Re-loading after new episode playback");
                        loadItem(lastPlayedItem.getId());
                        dataRefreshService.getValue().setLastPlayedItem(null); //blank this out so a detail screen we back up to doesn't also do this
                    } else {
                        Timber.d("Updating info after playback");
                        FullDetailsFragmentHelperKt.getItem(FullDetailsFragment.this, mBaseItem.getId(), item -> {
                            if (item == null) return null;

                            mBaseItem = item;
                            if (mResumeButton != null) {
                                boolean resumeVisible = (mBaseItem.getType() == BaseItemKind.SERIES && !mBaseItem.getUserData().getPlayed()) || JavaCompat.getCanResume(mBaseItem);
                                mResumeButton.setVisibility(resumeVisible ? View.VISIBLE : View.GONE);
                                if (JavaCompat.getCanResume(mBaseItem)) {
                                    mResumeButton.setLabel(getString(R.string.lbl_resume_from, TimeUtils.formatMillis((mBaseItem.getUserData().getPlaybackPositionTicks() / 10000) - getResumePreroll())));
                                }
                                if (resumeVisible) {
                                    mResumeButton.requestFocus();
                                } else if (playButton != null && ViewKt.isVisible(playButton)) {
                                    playButton.requestFocus();
                                }
                                showMoreButtonIfNeeded();
                            }
                            updateWatched();
                            mLastUpdated = Instant.now();
                            return null;
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
            return keyProcessor.getValue().handleKey(keyCode, mCurrentItem, requireActivity());
        } else if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) && BaseItemExtensionsKt.canPlay(mBaseItem)) {
            //default play action
            Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
            play(mBaseItem, pos.intValue(), false);
            return true;
        }

        return false;
    }

    private void startClock() {
        mClockLoop = new Runnable() {
            @Override
            public void run() {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;
                // View holder may be null when the base item is still loading - this is a rare case
                // which generally happens when the server is unresponsive
                MyDetailsOverviewRowPresenter.ViewHolder viewholder = mDorPresenter.getViewHolder();
                if (viewholder == null) return;

                if (mBaseItem != null && ((mBaseItem.getRunTimeTicks() != null && mBaseItem.getRunTimeTicks() > 0) || mBaseItem.getRunTimeTicks() != null)) {
                    viewholder.setInfoValue3(getEndTime());
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

    private void loadItem(UUID id) {
        if (mChannelId != null && mProgramInfo == null) {
            // if we are displaying a live tv channel - we want to get whatever is showing now on that channel
            apiClient.getValue().GetLiveTvChannelAsync(mChannelId.toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<ChannelInfoDto>(getLifecycle()) {
                @Override
                public void onResponse(ChannelInfoDto response) {
                    if (!getActive()) return;

                    mProgramInfo = ModelCompat.asSdk(response.getCurrentProgram());
                    mItemId = mProgramInfo.getId();
                    FullDetailsFragmentHelperKt.getItem(FullDetailsFragment.this, mItemId, item -> {
                        if (item != null) {
                            setBaseItem(item);
                        } else {
                            // Failed to load item
                            navigationRepository.getValue().goBack();
                        }
                        return null;
                    });
                }
            });
        } else if (mSeriesTimerInfo != null) {
            setBaseItem(FullDetailsFragmentHelperKt.createFakeSeriesTimerBaseItemDto(this, mSeriesTimerInfo));
        } else {
            FullDetailsFragmentHelperKt.getItem(FullDetailsFragment.this, id, item -> {
                if (item != null) {
                    setBaseItem(item);
                } else {
                    // Failed to load item
                    navigationRepository.getValue().goBack();
                }
                return null;
            });
        }

        mLastUpdated = Instant.now();
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
        if (mSeriesSettingsButton != null)
            mSeriesSettingsButton.setVisibility(id == null ? View.GONE : View.VISIBLE);

    }

    private class BuildDorTask extends AsyncTask<BaseItemDto, Integer, MyDetailsOverviewRow> {

        @Override
        protected MyDetailsOverviewRow doInBackground(BaseItemDto... params) {
            BaseItemDto item = params[0];

            // Figure image size
            Double aspect = imageHelper.getValue().getImageAspectRatio(item, false);
            posterHeight = aspect > 1 ? Utils.convertDpToPixel(requireContext(), 160) : Utils.convertDpToPixel(requireContext(), item.getType() == BaseItemKind.PERSON || item.getType() == BaseItemKind.MUSIC_ARTIST ? 300 : 200);

            mDetailsOverviewRow = new MyDetailsOverviewRow(item);

            String primaryImageUrl = imageHelper.getValue().getLogoImageUrl(mBaseItem, 600, true);
            if (primaryImageUrl == null) {
                primaryImageUrl = imageHelper.getValue().getPrimaryImageUrl(mBaseItem, false, null, posterHeight);
                if (item.getRunTimeTicks() != null && item.getRunTimeTicks() > 0 && item.getUserData() != null && item.getUserData().getPlaybackPositionTicks() > 0)
                    mDetailsOverviewRow.setProgress(((int) (item.getUserData().getPlaybackPositionTicks() * 100.0 / item.getRunTimeTicks())));
            }

            mDetailsOverviewRow.setSummary(item.getOverview());
            switch (item.getType()) {
                case PERSON:
                case MUSIC_ARTIST:
                    break;
                default:

                    BaseItemPerson director = BaseItemExtensionsKt.getFirstPerson(item, PersonKind.DIRECTOR);

                    InfoItem firstRow;
                    if (item.getType() == BaseItemKind.SERIES) {
                        firstRow = new InfoItem(
                                getString(R.string.lbl_seasons),
                                String.format("%d", Utils.getSafeValue(item.getChildCount(), 0)));
                    } else {
                        firstRow = new InfoItem(
                                getString(R.string.lbl_directed_by),
                                director != null ? director.getName() : getString(R.string.lbl_bracket_unknown));
                    }
                    mDetailsOverviewRow.setInfoItem1(firstRow);

                    if ((item.getRunTimeTicks() != null && item.getRunTimeTicks() > 0) || item.getRunTimeTicks() != null) {
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
        backgroundService.getValue().setBackground(item);
        if (mBaseItem != null) {
            if (mChannelId != null) {
                mBaseItem = JavaCompat.copyWithParentId(mBaseItem, mChannelId);
                mBaseItem = JavaCompat.copyWithDates(
                        mBaseItem,
                        mProgramInfo.getStartDate(),
                        mProgramInfo.getEndDate(),
                        mBaseItem.getOfficialRating(),
                        mProgramInfo.getRunTimeTicks()
                );
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
        Timber.d("Item type: %s", mBaseItem.getType().toString());

        if (mSeriesTimerInfo != null) {
            TimerQuery scheduled = new TimerQuery();
            scheduled.setSeriesTimerId(mSeriesTimerInfo.getId());
            TvManager.getScheduleRowsAsync(requireContext(), scheduled, new CardPresenter(true), adapter, new LifecycleAwareResponse<Integer>(getLifecycle()) {
            });
            return;
        }

        switch (mBaseItem.getType()) {
            case MOVIE:

                //Additional Parts
                if (mBaseItem.getPartCount() != null && mBaseItem.getPartCount() > 0) {
                    ItemRowAdapter additionalPartsAdapter = new ItemRowAdapter(requireContext(), new AdditionalPartsQuery(mBaseItem.getId()), new CardPresenter(), adapter);
                    addItemRow(adapter, additionalPartsAdapter, 0, getString(R.string.lbl_additional_parts));
                }

                //Cast/Crew
                if (mBaseItem.getPeople() != null && !mBaseItem.getPeople().isEmpty()) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(mBaseItem.getPeople(), requireContext(), new CardPresenter(true, 130), adapter);
                    addItemRow(adapter, castAdapter, 1, getString(R.string.lbl_cast_crew));
                }

                //Specials
                if (mBaseItem.getSpecialFeatureCount() != null && mBaseItem.getSpecialFeatureCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(requireContext(), new SpecialsQuery(mBaseItem.getId()), new CardPresenter(), adapter), 3, getString(R.string.lbl_specials));
                }

                //Trailers
                if (mBaseItem.getLocalTrailerCount() != null && mBaseItem.getLocalTrailerCount() > 1) {
                    addItemRow(adapter, new ItemRowAdapter(requireContext(), new TrailersQuery(mBaseItem.getId().toString()), new CardPresenter(), adapter), 4, getString(R.string.lbl_trailers));
                }

                //Chapters
                if (mBaseItem.getChapters() != null && !mBaseItem.getChapters().isEmpty()) {
                    List<ChapterItemInfo> chapters = BaseItemExtensionsKt.buildChapterItems(mBaseItem, api.getValue());
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(requireContext(), chapters, new CardPresenter(true, 120), adapter);
                    addItemRow(adapter, chapterAdapter, 2, getString(R.string.lbl_chapters));
                }

                //Similar
                SimilarItemsQuery similar = new SimilarItemsQuery();
                similar.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                similar.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                similar.setId(mBaseItem.getId().toString());
                similar.setLimit(10);

                ItemRowAdapter similarMoviesAdapter = new ItemRowAdapter(requireContext(), similar, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarMoviesAdapter, 5, getString(R.string.lbl_more_like_this));

                addInfoRows(adapter);
                break;
            case TRAILER:

                //Cast/Crew
                if (mBaseItem.getPeople() != null && !mBaseItem.getPeople().isEmpty()) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(mBaseItem.getPeople(), requireContext(), new CardPresenter(true, 130), adapter);
                    addItemRow(adapter, castAdapter, 0, getString(R.string.lbl_cast_crew));
                }

                //Similar
                SimilarItemsQuery similarTrailer = new SimilarItemsQuery();
                similarTrailer.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                similarTrailer.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                similarTrailer.setId(mBaseItem.getId().toString());
                similarTrailer.setLimit(10);

                ItemRowAdapter similarTrailerAdapter = new ItemRowAdapter(requireContext(), similarTrailer, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarTrailerAdapter, 4, getString(R.string.lbl_more_like_this));
                addInfoRows(adapter);
                break;
            case PERSON:

                ItemQuery personMovies = new ItemQuery();
                personMovies.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                personMovies.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                personMovies.setPersonIds(new String[]{mBaseItem.getId().toString()});
                personMovies.setRecursive(true);
                personMovies.setIncludeItemTypes(new String[]{"Movie"});
                personMovies.setSortBy(new String[]{ItemSortBy.SORT_NAME.getSerialName()});
                ItemRowAdapter personMoviesAdapter = new ItemRowAdapter(requireContext(), personMovies, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personMoviesAdapter, 0, getString(R.string.lbl_movies));

                ItemQuery personSeries = new ItemQuery();
                personSeries.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                personSeries.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                personSeries.setPersonIds(new String[]{mBaseItem.getId().toString()});
                personSeries.setRecursive(true);
                personSeries.setIncludeItemTypes(new String[]{"Series"});
                personSeries.setSortBy(new String[]{ItemSortBy.SORT_NAME.getSerialName()});
                ItemRowAdapter personSeriesAdapter = new ItemRowAdapter(requireContext(), personSeries, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personSeriesAdapter, 1, getString(R.string.lbl_tv_series));

                ItemQuery personEpisodes = new ItemQuery();
                personEpisodes.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                personEpisodes.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                personEpisodes.setPersonIds(new String[]{mBaseItem.getId().toString()});
                personEpisodes.setRecursive(true);
                personEpisodes.setIncludeItemTypes(new String[]{"Episode"});
                personEpisodes.setSortBy(new String[]{ItemSortBy.SERIES_SORT_NAME.getSerialName(), ItemSortBy.SORT_NAME.getSerialName()});
                ItemRowAdapter personEpisodesAdapter = new ItemRowAdapter(requireContext(), personEpisodes, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personEpisodesAdapter, 2, getString(R.string.lbl_episodes));

                break;
            case MUSIC_ARTIST:

                ItemQuery artistAlbums = new ItemQuery();
                artistAlbums.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                artistAlbums.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                artistAlbums.setArtistIds(new String[]{mBaseItem.getId().toString()});
                artistAlbums.setRecursive(true);
                artistAlbums.setIncludeItemTypes(new String[]{"MusicAlbum"});
                ItemRowAdapter artistAlbumsAdapter = new ItemRowAdapter(requireContext(), artistAlbums, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, artistAlbumsAdapter, 0, getString(R.string.lbl_albums));

                break;
            case SERIES:
                ItemRowAdapter nextUpAdapter = new ItemRowAdapter(requireContext(), BrowsingUtils.createSeriesGetNextUpRequest(mBaseItem.getId()), false, new CardPresenter(true, 130), adapter);
                addItemRow(adapter, nextUpAdapter, 0, getString(R.string.lbl_next_up));

                ItemRowAdapter seasonsAdapter = new ItemRowAdapter(requireContext(), BrowsingUtils.createSeasonsRequest(mBaseItem.getId()), new CardPresenter(), adapter);
                addItemRow(adapter, seasonsAdapter, 1, getString(R.string.lbl_seasons));

                //Specials
                if (mBaseItem.getSpecialFeatureCount() != null && mBaseItem.getSpecialFeatureCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(requireContext(), new SpecialsQuery(mBaseItem.getId()), new CardPresenter(), adapter), 3, getString(R.string.lbl_specials));
                }

                ItemRowAdapter upcomingAdapter = new ItemRowAdapter(requireContext(), BrowsingUtils.createUpcomingEpisodesRequest(mBaseItem.getId()), new CardPresenter(), adapter);
                addItemRow(adapter, upcomingAdapter, 2, getString(R.string.lbl_upcoming));

                if (mBaseItem.getPeople() != null && !mBaseItem.getPeople().isEmpty()) {
                    ItemRowAdapter seriesCastAdapter = new ItemRowAdapter(mBaseItem.getPeople(), requireContext(), new CardPresenter(true, 130), adapter);
                    addItemRow(adapter, seriesCastAdapter, 3, getString(R.string.lbl_cast_crew));
                }

                SimilarItemsQuery similarSeries = new SimilarItemsQuery();
                similarSeries.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                similarSeries.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                similarSeries.setId(mBaseItem.getId().toString());
                similarSeries.setLimit(20);
                ItemRowAdapter similarAdapter = new ItemRowAdapter(requireContext(), similarSeries, QueryType.SimilarSeries, new CardPresenter(), adapter);
                addItemRow(adapter, similarAdapter, 4, getString(R.string.lbl_more_like_this));
                break;

            case EPISODE:
                if (mBaseItem.getSeasonId() != null && mBaseItem.getIndexNumber() != null) {
                    StdItemQuery nextEpisodes = new StdItemQuery();
                    nextEpisodes.setParentId(mBaseItem.getSeasonId().toString());
                    nextEpisodes.setIncludeItemTypes(new String[]{"Episode"});
                    nextEpisodes.setStartIndex(mBaseItem.getIndexNumber()); // query index is zero-based but episode no is not
                    nextEpisodes.setLimit(20);
                    ItemRowAdapter nextAdapter = new ItemRowAdapter(requireContext(), nextEpisodes, 0, false, true, new CardPresenter(true, 120), adapter);
                    addItemRow(adapter, nextAdapter, 5, getString(R.string.lbl_next_episode));
                }

                //Guest stars
                if (mBaseItem.getPeople() != null && !mBaseItem.getPeople().isEmpty()) {
                    List<BaseItemPerson> guests = new ArrayList<>();
                    for (BaseItemPerson person : mBaseItem.getPeople()) {
                        if (person.getType() == PersonKind.GUEST_STAR) guests.add(person);
                    }
                    if (!guests.isEmpty()) {
                        ItemRowAdapter castAdapter = new ItemRowAdapter(guests, requireContext(), new CardPresenter(true, 130), adapter);
                        addItemRow(adapter, castAdapter, 0, getString(R.string.lbl_guest_stars));
                    }
                }

                //Chapters
                if (mBaseItem.getChapters() != null && !mBaseItem.getChapters().isEmpty()) {
                    List<ChapterItemInfo> chapters = BaseItemExtensionsKt.buildChapterItems(mBaseItem, api.getValue());
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(requireContext(), chapters, new CardPresenter(true, 120), adapter);
                    addItemRow(adapter, chapterAdapter, 1, getString(R.string.lbl_chapters));
                }

                addInfoRows(adapter);
                break;
        }
    }

    private void addInfoRows(MutableObjectAdapter<Row> adapter) {
        if (KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getDebuggingEnabled()) && mBaseItem.getMediaSources() != null) {
            for (MediaSourceInfo ms : mBaseItem.getMediaSources()) {
                if (ms.getMediaStreams() != null && !ms.getMediaStreams().isEmpty()) {
                    HeaderItem header = new HeaderItem("Media Details" + (ms.getContainer() != null ? " (" + ms.getContainer() + ")" : ""));
                    ArrayObjectAdapter infoAdapter = new ArrayObjectAdapter(new InfoCardPresenter());
                    for (MediaStream stream : ms.getMediaStreams()) {
                        infoAdapter.add(stream);
                    }

                    adapter.add(new ListRow(header, infoAdapter));

                }
            }
        }
    }

    private void updateInfo(BaseItemDto item) {
        if (buttonTypeList.contains(item.getType())) addButtons(BUTTON_SIZE);

        mLastUpdated = Instant.now();
    }

    public void setTitle(String title) {
        mDorPresenter.getViewHolder().setTitle(title);
    }

    private String getRunTime() {
        Long runtime = Utils.getSafeValue(mBaseItem.getRunTimeTicks(), mBaseItem.getRunTimeTicks());
        return runtime != null && runtime > 0 ? String.format("%d%s", (int) Math.ceil((double) runtime / 600000000), getString(R.string.lbl_min)) : "";
    }

    private String getEndTime() {
        if (mBaseItem != null && mBaseItem.getType() != BaseItemKind.MUSIC_ARTIST && mBaseItem.getType() != BaseItemKind.PERSON) {
            Long runtime = Utils.getSafeValue(mBaseItem.getRunTimeTicks(), mBaseItem.getRunTimeTicks());
            if (runtime != null && runtime > 0) {
                long endTimeTicks = mBaseItem.getType() == BaseItemKind.PROGRAM && mBaseItem.getEndDate() != null ? mBaseItem.getEndDate().toInstant(ZoneOffset.UTC).toEpochMilli() : Instant.now().toEpochMilli() + runtime / 10000;
                if (JavaCompat.getCanResume(mBaseItem)) {
                    endTimeTicks = Instant.now().toEpochMilli() + ((runtime - mBaseItem.getUserData().getPlaybackPositionTicks()) / 10000);
                }
                return android.text.format.DateFormat.getTimeFormat(requireContext()).format(new Date(endTimeTicks));
            }

        }
        return "";
    }

    void addItemToQueue() {
        BaseItemDto baseItem = mBaseItem;
        if (baseItem.getType() == BaseItemKind.AUDIO || baseItem.getType() == BaseItemKind.MUSIC_ALBUM || baseItem.getType() == BaseItemKind.MUSIC_ARTIST) {
            if (baseItem.getType() == BaseItemKind.MUSIC_ALBUM || baseItem.getType() == BaseItemKind.MUSIC_ARTIST) {
                playbackHelper.getValue().getItemsToPlay(getContext(), baseItem, false, false, new LifecycleAwareResponse<List<BaseItemDto>>(getLifecycle()) {
                    @Override
                    public void onResponse(List<BaseItemDto> response) {
                        if (!getActive()) return;

                        mediaManager.getValue().addToAudioQueue(response);
                    }
                });
            } else {
                mediaManager.getValue().addToAudioQueue(Arrays.asList(baseItem));
            }
        }
    }

    void gotoSeries() {
        navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(mBaseItem.getSeriesId()));
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
                            mBaseItem,
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
        BaseItemDto baseItem = mBaseItem;
        String buttonLabel;
        if (baseItem.getType() == BaseItemKind.SERIES) {
            buttonLabel = getString(R.string.lbl_play_next_up);
        } else {
            long startPos = 0;
            if (JavaCompat.getCanResume(mBaseItem)) {
                startPos = (mBaseItem.getUserData().getPlaybackPositionTicks() / 10000) - getResumePreroll();
            }
            buttonLabel = getString(R.string.lbl_resume_from, TimeUtils.formatMillis(startPos));
        }
        mResumeButton = TextUnderButton.create(requireContext(), R.drawable.ic_resume, buttonSize, 2, buttonLabel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullDetailsFragmentHelperKt.resumePlayback(FullDetailsFragment.this);
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

            if (mBaseItem.isFolder()) {
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
                boolean resumeButtonVisible = (baseItem.getType() == BaseItemKind.SERIES && !mBaseItem.getUserData().getPlayed()) || (JavaCompat.getCanResume(mBaseItem));
                mResumeButton.setVisibility(resumeButtonVisible ? View.VISIBLE : View.GONE);

                playButton = TextUnderButton.create(requireContext(), R.drawable.ic_play, buttonSize, 2, getString(BaseItemExtensionsKt.isLiveTv(mBaseItem) ? R.string.lbl_tune_to_channel : mBaseItem.isFolder() ? R.string.lbl_play_all : R.string.lbl_play), new View.OnClickListener() {
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
                        || (baseItem.getType() == BaseItemKind.PLAYLIST && MediaType.AUDIO.equals(baseItem.getMediaType()));

                if (isMusic) {
                    queueButton = TextUnderButton.create(requireContext(), R.drawable.ic_add, buttonSize, 2, getString(R.string.lbl_add_to_queue), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addItemToQueue();
                        }
                    });
                    mDetailsOverviewRow.addAction(queueButton);
                }

                if (mBaseItem.isFolder() || baseItem.getType() == BaseItemKind.MUSIC_ARTIST) {
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
                            playbackHelper.getValue().playInstantMix(requireContext(), baseItem);
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
                    if (versions != null) {
                        addVersionsMenu(v);
                    } else {
                        versions = new ArrayList<>(mBaseItem.getMediaSources());
                        addVersionsMenu(v);
                    }
                }
            });
            mDetailsOverviewRow.addAction(mVersionsButton);
        }

        if (TrailerUtils.hasPlayableTrailers(requireContext(), mBaseItem)) {
            trailerButton = TextUnderButton.create(requireContext(), R.drawable.ic_trailer, buttonSize, 0, getString(R.string.lbl_play_trailers), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FullDetailsFragmentHelperKt.playTrailers(FullDetailsFragment.this);
                }
            });

            mDetailsOverviewRow.addAction(trailerButton);
        }

        if (mProgramInfo != null && Utils.canManageRecordings(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue())) {
            if (mBaseItem.getEndDate().isAfter(LocalDateTime.now())) {
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
                                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<org.jellyfin.apiclient.model.dto.BaseItemDto>(getLifecycle()) {
                                                @Override
                                                public void onResponse(org.jellyfin.apiclient.model.dto.BaseItemDto response) {
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
                mRecSeriesButton = TextUnderButton.create(requireContext(), R.drawable.ic_record_series, buttonSize, 4, getString(R.string.lbl_record_series), new View.OnClickListener() {
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
                                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<org.jellyfin.apiclient.model.dto.BaseItemDto>(getLifecycle()) {
                                                @Override
                                                public void onResponse(org.jellyfin.apiclient.model.dto.BaseItemDto response) {
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

        org.jellyfin.sdk.model.api.UserItemDataDto userData = mBaseItem.getUserData();
        if (userData != null && mProgramInfo == null) {
            if (mBaseItem.getType() != BaseItemKind.MUSIC_ARTIST && mBaseItem.getType() != BaseItemKind.PERSON) {
                mWatchedToggleButton = TextUnderButton.create(requireContext(), R.drawable.ic_watch, buttonSize, 0, getString(R.string.lbl_watched), markWatchedListener);
                mWatchedToggleButton.setActivated(userData.getPlayed());
                mDetailsOverviewRow.addAction(mWatchedToggleButton);
            }

            //Favorite
            favButton = TextUnderButton.create(requireContext(), R.drawable.ic_heart, buttonSize, 2, getString(R.string.lbl_favorite), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    FullDetailsFragmentHelperKt.toggleFavorite(FullDetailsFragment.this);
                }
            });
            favButton.setActivated(userData.isFavorite());
            mDetailsOverviewRow.addAction(favButton);
        }

        if (mBaseItem.getType() == BaseItemKind.EPISODE && mBaseItem.getSeriesId() != null) {
            //add the prev button first so it will be there in proper position - we'll show it later if needed
            mPrevButton = TextUnderButton.create(requireContext(), R.drawable.ic_previous_episode, buttonSize, 3, getString(R.string.lbl_previous_episode), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPrevItemId != null) {
                        navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(mPrevItemId));
                    }
                }
            });

            mDetailsOverviewRow.addAction(mPrevButton);

            //now go get our prev episode id
            FullDetailsFragmentHelperKt.populatePreviousButton(FullDetailsFragment.this);

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
            if (mBaseItem.getType() == BaseItemKind.RECORDING && currentUser.getPolicy().getEnableLiveTvManagement() && mBaseItem.getCanDelete())
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

        if (mSeriesTimerInfo != null) {
            //Settings
            mDetailsOverviewRow.addAction(TextUnderButton.create(requireContext(), R.drawable.ic_settings, buttonSize, 0, getString(R.string.lbl_series_settings), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //show recording options
                    showRecordingOptions(mSeriesTimerInfo.getId(), mBaseItem, true);
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
                FullDetailsFragmentHelperKt.showDetailsMenu(FullDetailsFragment.this, v, mBaseItem);
            }
        });

        moreButton.setVisibility(View.GONE);
        mDetailsOverviewRow.addAction(moreButton);
        if (mBaseItem.getType() != BaseItemKind.EPISODE)
            showMoreButtonIfNeeded();  //Episodes check for previous and then call this above
    }

    private void addVersionsMenu(View v) {
        PopupMenu menu = new PopupMenu(requireContext(), v, Gravity.END);

        for (int i = 0; i < versions.size(); i++) {
            MenuItem item = menu.getMenu().add(Menu.NONE, i, Menu.NONE, versions.get(i).getName());
            item.setChecked(i == mDetailsOverviewRow.getSelectedMediaSourceIndex());
        }

        menu.getMenu().setGroupCheckable(0, true, false);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mDetailsOverviewRow.setSelectedMediaSourceIndex(menuItem.getItemId());
                FullDetailsFragmentHelperKt.getItem(FullDetailsFragment.this, UUIDSerializerKt.toUUID(versions.get(mDetailsOverviewRow.getSelectedMediaSourceIndex()).getId()), item -> {
                    if (item == null) return null;

                    mBaseItem = item;
                    mDorPresenter.getViewHolder().setItem(mDetailsOverviewRow);
                    if (mVersionsButton != null) mVersionsButton.requestFocus();
                    return null;
                });
                return true;
            }
        });

        menu.show();
    }

    int collapsedOptions = 0;

    void showMoreButtonIfNeeded() {
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

    public void showRecordingOptions(String id, final BaseItemDto program, final boolean recordSeries) {
        if (mRecordPopup == null) {
            int width = Utils.convertDpToPixel(requireContext(), 600);
            Point size = new Point();
            requireActivity().getWindowManager().getDefaultDisplay().getSize(size);
            mRecordPopup = new RecordPopup(requireActivity(), getLifecycle(), mRowsFragment.getView(), (size.x / 2) - (width / 2), mRowsFragment.getView().getTop() + 40, width);
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
                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<org.jellyfin.apiclient.model.dto.BaseItemDto>(getLifecycle()) {
                                @Override
                                public void onResponse(org.jellyfin.apiclient.model.dto.BaseItemDto response) {
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
            itemLauncher.getValue().launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow) row).getAdapter(), requireContext());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
            } else {
                mCurrentItem = (BaseRowItem) item;
            }
        }
    }

    private View.OnClickListener markWatchedListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            FullDetailsFragmentHelperKt.togglePlayed(FullDetailsFragment.this);
        }
    };

    void shufflePlay() {
        play(mBaseItem, 0, true);
    }

    void play(final BaseItemDto item, final int pos, final boolean shuffle) {
        playbackHelper.getValue().getItemsToPlay(getContext(), item, pos == 0 && item.getType() == BaseItemKind.MOVIE, shuffle, new LifecycleAwareResponse<List<BaseItemDto>>(getLifecycle()) {
            @Override
            public void onResponse(List<BaseItemDto> response) {
                if (!getActive()) return;

                if (item.getType() == BaseItemKind.MUSIC_ARTIST) {
                    mediaManager.getValue().playNow(requireContext(), response, 0, shuffle);
                } else {
                    videoQueueManager.getValue().setCurrentVideoQueue(response);
                    Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(item.getType(), pos);
                    navigationRepository.getValue().navigate(destination);
                }
            }
        });

    }

    void play(final List<BaseItemDto> items, final int pos, final boolean shuffle) {
        if (items.isEmpty()) return;
        if (shuffle) Collections.shuffle(items);
        videoQueueManager.getValue().setCurrentVideoQueue(items);
        Destination destination = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackDestination(items.get(0).getType(), pos);
        navigationRepository.getValue().navigate(destination);
    }
}
