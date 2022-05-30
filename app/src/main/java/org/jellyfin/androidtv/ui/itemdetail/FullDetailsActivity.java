package org.jellyfin.androidtv.ui.itemdetail;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.core.view.ViewKt;
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

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.InfoItem;
import org.jellyfin.androidtv.data.querying.SpecialsQuery;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.querying.TrailersQuery;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.ActivityFullDetailsBinding;
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
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter;
import org.jellyfin.androidtv.ui.presentation.InfoCardPresenter;
import org.jellyfin.androidtv.ui.presentation.MyDetailsOverviewRowPresenter;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.ui.shared.MessageListener;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.MarkdownRenderer;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemPerson;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.PersonType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerQuery;
import org.jellyfin.apiclient.model.querying.EpisodeQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.querying.NextUpQuery;
import org.jellyfin.apiclient.model.querying.SeasonQuery;
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery;
import org.jellyfin.apiclient.model.querying.UpcomingEpisodesQuery;
import org.jellyfin.apiclient.serialization.GsonJsonSerializer;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class FullDetailsActivity extends BaseActivity implements RecordingIndicatorView {

    private int BUTTON_SIZE;

    private TextUnderButton mResumeButton;
    private TextUnderButton mPrevButton;
    private TextUnderButton mRecordButton;
    private TextUnderButton mRecSeriesButton;
    private TextUnderButton mSeriesSettingsButton;
    private TextUnderButton mWatchedToggleButton;

    private DisplayMetrics mMetrics;

    protected BaseItemDto mProgramInfo;
    protected SeriesTimerInfoDto mSeriesTimerInfo;
    protected String mItemId;
    protected String mChannelId;
    protected BaseRowItem mCurrentItem;
    private Calendar mLastUpdated;
    private String mPrevItemId;

    private RowsSupportFragment mRowsFragment;
    private ArrayObjectAdapter mRowsAdapter;

    private MyDetailsOverviewRowPresenter mDorPresenter;
    private MyDetailsOverviewRow mDetailsOverviewRow;
    private CustomListRowPresenter mListRowPresenter;

    private FullDetailsActivity mActivity;
    private Handler mLoopHandler = new Handler();
    private Runnable mClockLoop;

    private BaseItemDto mBaseItem;

    private ArrayList<MediaSourceInfo> versions;
    private int selectedVersionPopupIndex = 0;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<GsonJsonSerializer> serializer = inject(GsonJsonSerializer.class);
    private Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);
    private Lazy<SystemPreferences> systemPreferences = inject(SystemPreferences.class);
    private Lazy<DataRefreshService> dataRefreshService = inject(DataRefreshService.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private Lazy<MarkdownRenderer> markdownRenderer = inject(MarkdownRenderer.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent activity from crashing after recovering from a crash
        if (KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue() == null) {
            finish();
            return;
        }

        ActivityFullDetailsBinding binding = ActivityFullDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BUTTON_SIZE = Utils.convertDpToPixel(this, 40);
        mActivity = this;
        backgroundService.getValue().attach(this);

        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mRowsFragment = new RowsSupportFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.rowsFragment, mRowsFragment).commit();

        mRowsFragment.setOnItemViewClickedListener(new ItemViewClickedListener());
        mRowsFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());

        mDorPresenter = new MyDetailsOverviewRowPresenter(markdownRenderer.getValue());

        mItemId = getIntent().getStringExtra("ItemId");
        mChannelId = getIntent().getStringExtra("ChannelId");
        String programJson = getIntent().getStringExtra("ProgramInfo");
        if (programJson != null) {
            mProgramInfo = serializer.getValue().DeserializeFromString(programJson, BaseItemDto.class);
        }
        String timerJson = getIntent().getStringExtra("SeriesTimer");
        if (timerJson != null) {
            mSeriesTimerInfo = serializer.getValue().DeserializeFromString(timerJson, SeriesTimerInfoDto.class);
        }

        registerMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(CustomMessage message) {
                if (message == CustomMessage.ActionComplete && mSeriesTimerInfo != null && mBaseItem.getBaseItemType() == BaseItemType.SeriesTimer) {
                    //update info
                    apiClient.getValue().GetLiveTvSeriesTimerAsync(mSeriesTimerInfo.getId(), new Response<SeriesTimerInfoDto>() {
                        @Override
                        public void onResponse(SeriesTimerInfoDto response) {
                            mSeriesTimerInfo = response;
                            mBaseItem.setOverview(BaseItemUtils.getSeriesOverview(mSeriesTimerInfo, mActivity));
                            mDorPresenter.getSummaryView().setText(mBaseItem.getOverview());
                        }
                    });

                    mRowsAdapter.removeItems(1, mRowsAdapter.size()-1); // delete all but detail row
                    //re-retrieve the schedule after giving it a second to rebuild
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            addAdditionalRows(mRowsAdapter);

                        }
                    }, 1500);
                }

            }
        });

        loadItem(mItemId);

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
    protected void onResume() {
        super.onResume();

        ClockBehavior clockBehavior = userPreferences.getValue().get(UserPreferences.Companion.getClockBehavior());
        if (clockBehavior == ClockBehavior.ALWAYS || clockBehavior == ClockBehavior.IN_MENUS) {
            startClock();
        }

        //Update information that may have changed - delay slightly to allow changes to take on the server
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                long lastPlaybackTime = dataRefreshService.getValue().getLastPlayback();
                Timber.d("current time %s last playback event time %s last refresh time %s", System.currentTimeMillis(), lastPlaybackTime, mLastUpdated.getTimeInMillis());

                // if last playback event exists, and event time is greater than last sync or within 2 seconds of current time
                // the third condition accounts for a situation where a sync (dataRefresh) coincides with the end of playback
                if (lastPlaybackTime > 0 && (lastPlaybackTime > mLastUpdated.getTimeInMillis() || System.currentTimeMillis() - lastPlaybackTime < 2000) && mBaseItem.getBaseItemType() != BaseItemType.MusicArtist) {
                    org.jellyfin.sdk.model.api.BaseItemDto lastPlayedItem = dataRefreshService.getValue().getLastPlayedItem();
                    if (mBaseItem.getBaseItemType() == BaseItemType.Episode && lastPlayedItem != null && !mBaseItem.getId().equals(lastPlayedItem.getId().toString()) && lastPlayedItem.getType() == BaseItemKind.EPISODE) {
                        Timber.i("Re-loading after new episode playback");
                        loadItem(lastPlayedItem.getId().toString());
                        dataRefreshService.getValue().setLastPlayedItem(null); //blank this out so a detail screen we back up to doesn't also do this
                    } else {
                        Timber.d("Updating info after playback");
                        apiClient.getValue().GetItemAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                            @Override
                            public void onResponse(BaseItemDto response) {
                                if (!isFinishing()) {
                                    mBaseItem = response;
                                    if (mResumeButton != null) {
                                        boolean resumeVisible = (mBaseItem.getBaseItemType() == BaseItemType.Series && !mBaseItem.getUserData().getPlayed()) || response.getCanResume();
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
                                    updatePlayedDate();
                                    updateWatched();
                                    //updatePoster();
                                    mLastUpdated = Calendar.getInstance();
                                }
                            }
                        });
                    }
                }
            }
        }, 750);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClock();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopClock();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mCurrentItem != null) {
            return KeyProcessor.HandleKey(keyCode, mCurrentItem, this) || super.onKeyUp(keyCode, event);
        } else if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) && BaseItemUtils.canPlay(mBaseItem)) {
            //default play action
            Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
            play(mBaseItem, pos.intValue() , false);
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void startClock() {
        mClockLoop = new Runnable() {
            @Override
            public void run() {
                if (mBaseItem != null && ((mBaseItem.getRunTimeTicks() != null && mBaseItem.getRunTimeTicks() > 0) || mBaseItem.getOriginalRunTimeTicks() != null)) {
                    mDorPresenter.updateEndTime(getEndTime());
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

    private static BaseItemType[] buttonTypes = new BaseItemType[] {
        BaseItemType.Episode,
        BaseItemType.Movie,
        BaseItemType.Series,
        BaseItemType.Season,
        BaseItemType.Folder,
        BaseItemType.Video,
        BaseItemType.Recording,
        BaseItemType.Program,
        BaseItemType.ChannelVideoItem,
        BaseItemType.Trailer,
        BaseItemType.MusicArtist,
        BaseItemType.Person,
        BaseItemType.MusicVideo,
        BaseItemType.SeriesTimer
    };


    private static List<BaseItemType> buttonTypeList = Arrays.asList(buttonTypes);
    private static String[] directPlayableTypes = new String[] {"Episode","Movie","Video","Recording","Program"};

    private void updateWatched() {
        if (mWatchedToggleButton != null && mBaseItem != null && mBaseItem.getUserData() != null && !isFinishing()) {
            mWatchedToggleButton.setActivated(mBaseItem.getUserData().getPlayed());
        }
    }

    private void loadItem(String id) {
        if (mChannelId != null && mProgramInfo == null) {
            // if we are displaying a live tv channel - we want to get whatever is showing now on that channel
            final FullDetailsActivity us = this;
            apiClient.getValue().GetLiveTvChannelAsync(mChannelId, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<ChannelInfoDto>() {
                @Override
                public void onResponse(ChannelInfoDto response) {
                    mProgramInfo = response.getCurrentProgram();
                    mItemId = mProgramInfo.getId();
                    apiClient.getValue().GetItemAsync(mItemId, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                        @Override
                        public void onResponse(BaseItemDto response) {
                            setBaseItem(response);
                        }
                    });
                }
            });
        } else if (mSeriesTimerInfo != null) {
            // create base item from our timer
            BaseItemDto item = new BaseItemDto();
            item.setId(mSeriesTimerInfo.getId());
            item.setSeriesTimerId(mSeriesTimerInfo.getId());
            item.setBaseItemType(BaseItemType.SeriesTimer);
            item.setName(mSeriesTimerInfo.getName());
            item.setOverview(BaseItemUtils.getSeriesOverview(mSeriesTimerInfo, this));

            setBaseItem(item);
        } else {
            apiClient.getValue().GetItemAsync(id, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto response) {
                    setBaseItem(response);
                }
            });
        }

        mLastUpdated = Calendar.getInstance();
    }

    @Override
    public void setRecTimer(String id) {
        mProgramInfo.setTimerId(id);
        if (mRecordButton != null) mRecordButton.setActivated(id != null);
    }

    private int posterWidth;
    private int posterHeight;

    @Override
    public void setRecSeriesTimer(String id) {
        if (mProgramInfo != null) mProgramInfo.setSeriesTimerId(id);
        if (mRecSeriesButton != null) mRecSeriesButton.setActivated(id != null);
        if (mSeriesSettingsButton != null) mSeriesSettingsButton.setVisibility(id == null ? View.GONE : View.VISIBLE);

    }

    private class BuildDorTask extends AsyncTask<BaseItemDto, Integer, MyDetailsOverviewRow> {

        @Override
        protected MyDetailsOverviewRow doInBackground(BaseItemDto... params) {
            BaseItemDto item = params[0];

            // Figure image size
            Double aspect = ImageUtils.getImageAspectRatio(item, false);
            posterHeight = aspect > 1 ? Utils.convertDpToPixel(mActivity, 160) : Utils.convertDpToPixel(mActivity, item.getBaseItemType() == BaseItemType.Person || item.getBaseItemType() == BaseItemType.MusicArtist ? 300 : 200);
            posterWidth = (int)((aspect) * posterHeight);
            if (posterHeight < 10) posterWidth = Utils.convertDpToPixel(mActivity, 150);  //Guard against zero size images causing picasso to barf

            mDetailsOverviewRow = new MyDetailsOverviewRow(item);

            String primaryImageUrl = ImageUtils.getLogoImageUrl(mBaseItem, 600, true);
            if (primaryImageUrl == null) {
                primaryImageUrl = ImageUtils.getPrimaryImageUrl(mActivity, mBaseItem, false, posterHeight);
                if (item.getRunTimeTicks() != null && item.getRunTimeTicks() > 0 && item.getUserData() != null && item.getUserData().getPlaybackPositionTicks() > 0)
                    mDetailsOverviewRow.setProgress(((int) (item.getUserData().getPlaybackPositionTicks() * 100.0 / item.getRunTimeTicks())));
            }

            mDetailsOverviewRow.setSummary(item.getOverview());
            switch (item.getBaseItemType()) {
                case Person:
                case MusicArtist:
                    break;
                default:

                    BaseItemPerson director = BaseItemUtils.getFirstPerson(item, PersonType.Director);

                    InfoItem firstRow;
                    if (item.getBaseItemType() == BaseItemType.Series) {
                        firstRow = new InfoItem(
                                getString(R.string.lbl_seasons),
                                Utils.getSafeValue(item.getChildCount(), 0).toString());
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

            mDetailsOverviewRow.setImageBitmap(primaryImageUrl);

            return mDetailsOverviewRow;
        }

        @Override
        protected void onPostExecute(MyDetailsOverviewRow detailsOverviewRow) {
            super.onPostExecute(detailsOverviewRow);

            if (isFinishing()) return;

            ClassPresenterSelector ps = new ClassPresenterSelector();
            ps.addClassPresenter(MyDetailsOverviewRow.class, mDorPresenter);
            mListRowPresenter = new CustomListRowPresenter(Utils.convertDpToPixel(mActivity, 10));
            ps.addClassPresenter(ListRow.class, mListRowPresenter);
            mRowsAdapter = new ArrayObjectAdapter(ps);
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
                mBaseItem.setParentId(mChannelId);
                mBaseItem.setPremiereDate(mProgramInfo.getStartDate());
                mBaseItem.setEndDate(mProgramInfo.getEndDate());
                mBaseItem.setRunTimeTicks(mProgramInfo.getRunTimeTicks());
            }
            new BuildDorTask().execute(item);
        }
    }

    protected void addItemRow(ArrayObjectAdapter parent, ItemRowAdapter row, int index, String headerText) {
        HeaderItem header = new HeaderItem(index, headerText);
        ListRow listRow = new ListRow(header, row);
        parent.add(listRow);
        row.setRow(listRow);
        row.Retrieve();
    }

    protected void addAdditionalRows(ArrayObjectAdapter adapter) {
        Timber.d("Item type: %s", mBaseItem.getBaseItemType().toString());
        switch (mBaseItem.getBaseItemType()) {
            case Movie:

                //Cast/Crew
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(this, mBaseItem.getPeople(), new CardPresenter(true, 260), adapter);
                    addItemRow(adapter, castAdapter, 0, getString(R.string.lbl_cast_crew));
                }

                //Specials
                if (mBaseItem.getSpecialFeatureCount() != null && mBaseItem.getSpecialFeatureCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(this, new SpecialsQuery(mBaseItem.getId()), new CardPresenter(), adapter), 2, getString(R.string.lbl_specials));
                }

                //Trailers
                if (mBaseItem.getLocalTrailerCount() != null && mBaseItem.getLocalTrailerCount() > 1) {
                    addItemRow(adapter, new ItemRowAdapter(this, new TrailersQuery(mBaseItem.getId()), new CardPresenter(), adapter), 3, getString(R.string.lbl_trailers));
                }

                //Chapters
                if (mBaseItem.getChapters() != null && mBaseItem.getChapters().size() > 0) {
                    List<ChapterItemInfo> chapters = BaseItemUtils.buildChapterItems(mBaseItem);
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(this, chapters, new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, chapterAdapter, 1, getString(R.string.lbl_chapters));
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

                ItemRowAdapter similarMoviesAdapter = new ItemRowAdapter(this, similar, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarMoviesAdapter, 4, getString(R.string.lbl_more_like_this));

                addInfoRows(adapter);
                break;
            case Trailer:

                //Cast/Crew
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(this, mBaseItem.getPeople(), new CardPresenter(true, 260), adapter);
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

                ItemRowAdapter similarTrailerAdapter = new ItemRowAdapter(this, similarTrailer, QueryType.SimilarMovies, new CardPresenter(), adapter);
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
                ItemRowAdapter personMoviesAdapter = new ItemRowAdapter(this, personMovies, 100, false, new CardPresenter(), adapter);
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
                ItemRowAdapter personSeriesAdapter = new ItemRowAdapter(this, personSeries, 100, false, new CardPresenter(), adapter);
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
                ItemRowAdapter personEpisodesAdapter = new ItemRowAdapter(this, personEpisodes, 100, false, new CardPresenter(), adapter);
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
                ItemRowAdapter artistAlbumsAdapter = new ItemRowAdapter(this, artistAlbums, 100, false, new CardPresenter(), adapter);
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
                ItemRowAdapter nextUpAdapter = new ItemRowAdapter(this, nextUpQuery, false, new CardPresenter(true, 260), adapter);
                addItemRow(adapter, nextUpAdapter, 0, getString(R.string.lbl_next_up));

                SeasonQuery seasons = new SeasonQuery();
                seasons.setSeriesId(mBaseItem.getId());
                seasons.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                seasons.setFields(new ItemFields[] {
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.DisplayPreferencesId,
                        ItemFields.ChildCount
                });
                ItemRowAdapter seasonsAdapter = new ItemRowAdapter(this, seasons, new CardPresenter(), adapter);
                addItemRow(adapter, seasonsAdapter, 1, getString(R.string.lbl_seasons));

                UpcomingEpisodesQuery upcoming = new UpcomingEpisodesQuery();
                upcoming.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                upcoming.setParentId(mBaseItem.getId());
                upcoming.setFields(new ItemFields[]{
                        ItemFields.PrimaryImageAspectRatio,
                        ItemFields.ChildCount
                });
                ItemRowAdapter upcomingAdapter = new ItemRowAdapter(this, upcoming, new CardPresenter(), adapter);
                addItemRow(adapter, upcomingAdapter, 2, getString(R.string.lbl_upcoming));

                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter seriesCastAdapter = new ItemRowAdapter(this, mBaseItem.getPeople(), new CardPresenter(true, 260), adapter);
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
                ItemRowAdapter similarAdapter = new ItemRowAdapter(this, similarSeries, QueryType.SimilarSeries, new CardPresenter(), adapter);
                addItemRow(adapter, similarAdapter, 4, getString(R.string.lbl_more_like_this));
                break;

            case Episode:
                if (mBaseItem.getSeasonId() != null && mBaseItem.getIndexNumber() != null) {
                    StdItemQuery nextEpisodes = new StdItemQuery();
                    nextEpisodes.setParentId(mBaseItem.getSeasonId());
                    nextEpisodes.setIncludeItemTypes(new String[]{"Episode"});
                    nextEpisodes.setStartIndex(mBaseItem.getIndexNumber()); // query index is zero-based but episode no is not
                    nextEpisodes.setLimit(20);
                    ItemRowAdapter nextAdapter = new ItemRowAdapter(this, nextEpisodes, 0 , false, true, new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, nextAdapter, 5, getString(R.string.lbl_next_episode));
                }

                //Guest stars
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    List<BaseItemPerson> guests = new ArrayList<>();
                    for (BaseItemPerson person : mBaseItem.getPeople()) {
                        if (person.getPersonType() == PersonType.GuestStar) guests.add(person);
                    }
                    if (guests.size() > 0) {
                        ItemRowAdapter castAdapter = new ItemRowAdapter(this, guests.toArray(new BaseItemPerson[guests.size()]), new CardPresenter(true, 260), adapter);
                        addItemRow(adapter, castAdapter, 0, getString(R.string.lbl_guest_stars));
                    }
                }

                //Chapters
                if (mBaseItem.getChapters() != null && mBaseItem.getChapters().size() > 0) {
                    List<ChapterItemInfo> chapters = BaseItemUtils.buildChapterItems(mBaseItem);
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(this, chapters, new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, chapterAdapter, 1, getString(R.string.lbl_chapters));
                }

                addInfoRows(adapter);
                break;

            case SeriesTimer:
                TimerQuery scheduled = new TimerQuery();
                scheduled.setSeriesTimerId(mSeriesTimerInfo.getId());
                TvManager.getScheduleRowsAsync(this, scheduled, new CardPresenter(true), adapter, new Response<Integer>());
                break;
        }


    }

    private void addInfoRows(ArrayObjectAdapter adapter) {
        if (KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getDebuggingEnabled()) && mBaseItem.getMediaSources() != null) {
            for (MediaSourceInfo ms : mBaseItem.getMediaSources()) {
                if (ms.getMediaStreams() != null && ms.getMediaStreams().size() > 0) {
                    HeaderItem header = new HeaderItem("Media Details"+(ms.getContainer() != null ? " (" +ms.getContainer()+")" : ""));
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
        if (buttonTypeList.contains(item.getBaseItemType())) addButtons(BUTTON_SIZE);
//        updatePlayedDate();

        mLastUpdated = Calendar.getInstance();
    }

    public void setTitle(String title) {
        mDorPresenter.setTitle(title);
    }

    private void updatePlayedDate() {
//        if (directPlayableTypeList.contains(mBaseItem.getType())) {
//            mLastPlayedText.setText(mBaseItem.getUserData() != null && mBaseItem.getUserData().getLastPlayedDate() != null ?
//                    getString(R.string.lbl_last_played)+ DateUtils.getRelativeTimeSpanString(Utils.convertToLocalDate(mBaseItem.getUserData().getLastPlayedDate()).getTime()).toString()
//                    : getString(R.string.lbl_never_played));
//        } else {
//            mLastPlayedText.setText("");
//        }
    }

    private void playTrailers() {
        apiClient.getValue().GetLocalTrailersAsync(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), mBaseItem.getId(), new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                play(response, 0, false);
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception, "Error retrieving trailers for playback");
                Utils.showToast(mActivity, R.string.msg_video_playback_error);
            }
        });
    }

    private String getRunTime() {
        Long runtime = Utils.getSafeValue(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
        return runtime != null && runtime > 0 ? (int) Math.ceil((double) runtime / 600000000) + getString(R.string.lbl_min) : "";
    }

    private String getEndTime() {
        if (mBaseItem != null && mBaseItem.getBaseItemType() != BaseItemType.MusicArtist && mBaseItem.getBaseItemType() != BaseItemType.Person) {
            Long runtime = Utils.getSafeValue(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
            if (runtime != null && runtime > 0) {
                long endTimeTicks = mBaseItem.getBaseItemType() == BaseItemType.Program && mBaseItem.getEndDate() != null ? TimeUtils.convertToLocalDate(mBaseItem.getEndDate()).getTime() : System.currentTimeMillis() + runtime / 10000;
                if (mBaseItem.getCanResume()) {
                    endTimeTicks = System.currentTimeMillis() + ((runtime - mBaseItem.getUserData().getPlaybackPositionTicks()) / 10000);
                }
                return android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks));
            }

        }
        return "";
    }

    private void addItemToQueue() {
        if (mBaseItem.getBaseItemType() == BaseItemType.Audio || mBaseItem.getBaseItemType() == BaseItemType.MusicAlbum || mBaseItem.getBaseItemType() == BaseItemType.MusicArtist) {
            if (mBaseItem.getBaseItemType() == BaseItemType.MusicAlbum || mBaseItem.getBaseItemType() == BaseItemType.MusicArtist) {
                PlaybackHelper.getItemsToPlay(mBaseItem, false, false, new Response<List<BaseItemDto>>() {
                    @Override
                    public void onResponse(List<BaseItemDto> response) {
                        mediaManager.getValue().addToAudioQueue(response);
                    }
                });
            } else {
                mediaManager.getValue().addToAudioQueue(Arrays.asList(mBaseItem));
            }
        } else {
            mediaManager.getValue().addToVideoQueue(mBaseItem);
        }
    }

    private void toggleFavorite() {
        UserItemDataDto data = mBaseItem.getUserData();
        apiClient.getValue().UpdateFavoriteStatusAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                favButton.setActivated(response.getIsFavorite());
                dataRefreshService.getValue().setLastFavoriteUpdate(System.currentTimeMillis());
            }
        });
    }

    private void gotoSeries() {
        Intent intent = new Intent(mActivity, FullDetailsActivity.class);
        intent.putExtra("ItemId", mBaseItem.getSeriesId());
        mActivity.startActivity(intent);
    }

    private void deleteItem() {
        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.lbl_delete)
                .setMessage("This will PERMANENTLY DELETE " + mBaseItem.getName() + " from your library.  Are you VERY sure?")
                .setPositiveButton("Delete",
                        (dialog, whichButton) -> apiClient.getValue().DeleteItem(mBaseItem.getId(), new EmptyResponse() {
                            @Override
                            public void onResponse() {
                                Utils.showToast(mActivity, mBaseItem.getName() + " Deleted");
                                dataRefreshService.getValue().setLastDeletedItemId(mBaseItem.getId());
                                finish();
                            }

                            @Override
                            public void onError(Exception ex) {
                                Utils.showToast(mActivity, ex.getLocalizedMessage());
                            }
                        }))
                .setNegativeButton("Cancel", (dialog, which) -> Utils.showToast(mActivity, "Item NOT Deleted"))
                .show()
                .getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    private TextUnderButton favButton = null;
    private TextUnderButton shuffleButton = null;
    private TextUnderButton goToSeriesButton = null;
    private TextUnderButton queueButton = null;
    private TextUnderButton deleteButton = null;
    private TextUnderButton moreButton;
    private TextUnderButton playButton = null;
    private TextUnderButton trailerButton = null;

    private void addButtons(int buttonSize) {
        String buttonLabel;
        if (mBaseItem.getBaseItemType() == BaseItemType.Series || mBaseItem.getBaseItemType() == BaseItemType.SeriesTimer) {
            buttonLabel = getString(R.string.lbl_play_next_up);
        } else {
            long startPos = 0;
            if (mBaseItem.getCanResume()) {
                startPos = (mBaseItem.getUserData().getPlaybackPositionTicks()/10000) - getResumePreroll();
            }
            buttonLabel = getString(R.string.lbl_resume_from, TimeUtils.formatMillis(startPos));
        }
        mResumeButton = TextUnderButton.create(this, R.drawable.ic_resume, buttonSize, 2, buttonLabel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBaseItem.getBaseItemType() == BaseItemType.Series) {
                    //play next up
                    NextUpQuery nextUpQuery = new NextUpQuery();
                    nextUpQuery.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
                    nextUpQuery.setSeriesId(mBaseItem.getId());
                    apiClient.getValue().GetNextUpEpisodesAsync(nextUpQuery, new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getItems().length > 0) {
                                play(response.getItems()[0], 0 , false);
                            } else {
                                Utils.showToast(FullDetailsActivity.this, "Unable to find next up episode");
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            Timber.e(exception, "Error playing next up episode");
                            Utils.showToast(FullDetailsActivity.this, getString(R.string.msg_video_playback_error));
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
        if (userPreferences.getValue().get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.CHOOSE && (mBaseItem.getBaseItemType() == BaseItemType.Series || mBaseItem.getBaseItemType() == BaseItemType.Movie || mBaseItem.getBaseItemType() == BaseItemType.Video || mBaseItem.getBaseItemType() == BaseItemType.Episode)) {
            playButton = TextUnderButton.create(this, R.drawable.ic_play, buttonSize, 3, getString(R.string.play_with), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu more = new PopupMenu(mActivity, view);
                    more.inflate(R.menu.menu_details_play_with);
                    more.setOnMenuItemClickListener(playWithMenuListener);
                    more.show();
                }
            });
            mDetailsOverviewRow.addAction(playButton);
        } else { //here playButton is only a play button
            if (BaseItemUtils.canPlay(mBaseItem)) {
                mDetailsOverviewRow.addAction(mResumeButton);
                boolean resumeButtonVisible = (mBaseItem.getBaseItemType() == BaseItemType.Series && !mBaseItem.getUserData().getPlayed()) || (mBaseItem.getCanResume());
                mResumeButton.setVisibility(resumeButtonVisible ? View.VISIBLE : View.GONE);

                playButton = TextUnderButton.create(this, R.drawable.ic_play, buttonSize, 2, getString(BaseItemUtils.isLiveTv(mBaseItem) ? R.string.lbl_tune_to_channel : mBaseItem.getIsFolderItem() ? R.string.lbl_play_all : R.string.lbl_play), new View.OnClickListener() {
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

                boolean isMusic = mBaseItem.getBaseItemType() == BaseItemType.MusicAlbum
                        || mBaseItem.getBaseItemType() == BaseItemType.MusicArtist
                        || mBaseItem.getBaseItemType() == BaseItemType.Audio
                        || (mBaseItem.getBaseItemType() == BaseItemType.Playlist && "Audio".equals(mBaseItem.getMediaType()));

                if (isMusic) {
                    queueButton = TextUnderButton.create(this, R.drawable.ic_add, buttonSize, 2, getString(R.string.lbl_add_to_queue), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addItemToQueue();
                        }
                    });
                    mDetailsOverviewRow.addAction(queueButton);
                }

                if (mBaseItem.getIsFolderItem() || mBaseItem.getBaseItemType() == BaseItemType.MusicArtist) {
                    shuffleButton = TextUnderButton.create(this, R.drawable.ic_shuffle, buttonSize, 2, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            play(mBaseItem, 0, true);
                        }
                    });
                    mDetailsOverviewRow.addAction(shuffleButton);
                }

                if (mBaseItem.getBaseItemType() == BaseItemType.MusicArtist) {
                    TextUnderButton imix = TextUnderButton.create(this, R.drawable.ic_mix, buttonSize, 0, getString(R.string.lbl_instant_mix), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PlaybackHelper.playInstantMix(FullDetailsActivity.this, mBaseItem);
                        }
                    });
                    mDetailsOverviewRow.addAction(imix);
                }

            }
        }
        //Video versions button
        if (mBaseItem.getMediaSources() != null && mBaseItem.getMediaSources().size() > 1){
            TextUnderButton versionsButton = TextUnderButton.create(this, R.drawable.ic_guide, buttonSize, 0, getString(R.string.select_version), new View.OnClickListener() {
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
            mDetailsOverviewRow.addAction(versionsButton);
        }


        if (mBaseItem.getLocalTrailerCount() != null && mBaseItem.getLocalTrailerCount() > 0) {
            trailerButton = TextUnderButton.create(this, R.drawable.ic_trailer, buttonSize, 0, getString(R.string.lbl_play_trailers), new View.OnClickListener() {
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
                mRecordButton = TextUnderButton.create(this, R.drawable.ic_record, buttonSize, 4, getString(R.string.lbl_record), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getTimerId() == null) {
                            //Create one-off recording with defaults
                            apiClient.getValue().GetDefaultLiveTvTimerInfo(mProgramInfo.getId(), new Response<SeriesTimerInfoDto>() {
                                @Override
                                public void onResponse(SeriesTimerInfoDto response) {
                                    apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            // we have to re-retrieve the program to get the timer id
                                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                                                @Override
                                                public void onResponse(BaseItemDto response) {
                                                    mProgramInfo = response;
                                                    setRecSeriesTimer(response.getSeriesTimerId());
                                                    setRecTimer(response.getTimerId());
                                                    Utils.showToast(mActivity, R.string.msg_set_to_record);

                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            Timber.e(ex, "Error creating recording");
                                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception exception) {
                                    Timber.e(exception, "Error creating recording");
                                    Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                }
                            });
                        } else {
                            apiClient.getValue().CancelLiveTvTimerAsync(mProgramInfo.getTimerId(), new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    setRecTimer(null);
                                    dataRefreshService.getValue().setLastDeletedItemId(mProgramInfo.getId());
                                    Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                                }

                                @Override
                                public void onError(Exception ex) {
                                    Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                                }
                            });

                        }
                    }
                });
                mRecordButton.setActivated(mProgramInfo.getTimerId() != null);

                mDetailsOverviewRow.addAction(mRecordButton);
            }

            if (mProgramInfo.getIsSeries() != null && mProgramInfo.getIsSeries()) {
                mRecSeriesButton= TextUnderButton.create(this, R.drawable.ic_record_series, buttonSize, 4, getString(R.string.lbl_record_series), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getSeriesTimerId() == null) {
                            //Create series recording with default options
                            apiClient.getValue().GetDefaultLiveTvTimerInfo(mProgramInfo.getId(), new Response<SeriesTimerInfoDto>() {
                                @Override
                                public void onResponse(SeriesTimerInfoDto response) {
                                    apiClient.getValue().CreateLiveTvSeriesTimerAsync(response, new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            // we have to re-retrieve the program to get the timer id
                                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                                                @Override
                                                public void onResponse(BaseItemDto response) {
                                                    mProgramInfo = response;
                                                    setRecSeriesTimer(response.getSeriesTimerId());
                                                    setRecTimer(response.getTimerId());
                                                    Utils.showToast(mActivity, R.string.msg_set_to_record);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            Timber.e(ex, "Error creating recording");
                                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception exception) {
                                    Timber.e(exception, "Error creating recording");
                                    Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                }
                            });

                        } else {
                            new AlertDialog.Builder(mActivity)
                                    .setTitle(getString(R.string.lbl_cancel_series))
                                    .setMessage(getString(R.string.msg_cancel_entire_series))
                                    .setNegativeButton(R.string.lbl_no, null)
                                    .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            apiClient.getValue().CancelLiveTvSeriesTimerAsync(mProgramInfo.getSeriesTimerId(), new EmptyResponse() {
                                                @Override
                                                public void onResponse() {
                                                    setRecSeriesTimer(null);
                                                    setRecTimer(null);
                                                    dataRefreshService.getValue().setLastDeletedItemId(mProgramInfo.getId());
                                                    Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                                                }

                                                @Override
                                                public void onError(Exception ex) {
                                                    Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                                                }
                                            });
                                        }
                                    }).show();

                        }
                    }
                });
                mRecSeriesButton.setActivated(mProgramInfo.getSeriesTimerId() != null);

                mDetailsOverviewRow.addAction(mRecSeriesButton);

                mSeriesSettingsButton = TextUnderButton.create(this, R.drawable.ic_settings, buttonSize, 2, getString(R.string.lbl_series_settings), new View.OnClickListener() {
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
            if (mBaseItem.getBaseItemType() != BaseItemType.MusicArtist && mBaseItem.getBaseItemType() != BaseItemType.Person) {
                mWatchedToggleButton = TextUnderButton.create(this, R.drawable.ic_watch, buttonSize, 0, getString(R.string.lbl_watched), markWatchedListener);
                mWatchedToggleButton.setActivated(userData.getPlayed());
                mDetailsOverviewRow.addAction(mWatchedToggleButton);
            }

            //Favorite
            favButton = TextUnderButton.create(this, R.drawable.ic_heart, buttonSize, 2, getString(R.string.lbl_favorite), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    toggleFavorite();
                }
            });
            favButton.setActivated(userData.getIsFavorite());
            mDetailsOverviewRow.addAction(favButton);
        }

        if (mBaseItem.getBaseItemType() == BaseItemType.Episode && mBaseItem.getSeriesId() != null) {
            //add the prev button first so it will be there in proper position - we'll show it later if needed
            mPrevButton = TextUnderButton.create(this, R.drawable.ic_previous_episode, buttonSize, 3, getString(R.string.lbl_previous_episode), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPrevItemId != null) {
                        Intent intent = new Intent(mActivity, FullDetailsActivity.class);
                        intent.putExtra("ItemId", mPrevItemId);
                        mActivity.startActivity(intent);
                    }
                }
            });

            mDetailsOverviewRow.addAction(mPrevButton);

            //now go get our prev episode id
            EpisodeQuery adjacent = new EpisodeQuery();
            adjacent.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
            adjacent.setSeriesId(mBaseItem.getSeriesId());
            adjacent.setAdjacentTo(mBaseItem.getId());
            apiClient.getValue().GetEpisodesAsync(adjacent, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
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

            goToSeriesButton = TextUnderButton.create(this, R.drawable.ic_tv, buttonSize, 0, getString(R.string.lbl_goto_series), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotoSeries();
                }
            });
            mDetailsOverviewRow.addAction(goToSeriesButton);
        }

        if ((mBaseItem.getBaseItemType() == BaseItemType.Recording && KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getPolicy().getEnableLiveTvManagement() && mBaseItem.getCanDelete()) ||
                ((mBaseItem.getBaseItemType() == BaseItemType.Movie || mBaseItem.getBaseItemType() == BaseItemType.Episode || mBaseItem.getBaseItemType() == BaseItemType.Video) && KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getPolicy().getEnableContentDeletion())) {
            deleteButton = TextUnderButton.create(this, R.drawable.ic_trash, buttonSize, 0, getString(R.string.lbl_delete), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteItem();
                }
            });
            mDetailsOverviewRow.addAction(deleteButton);
        }

        if (mSeriesTimerInfo != null && mBaseItem.getBaseItemType() == BaseItemType.SeriesTimer) {
            //Settings
            mDetailsOverviewRow.addAction(TextUnderButton.create(this, R.drawable.ic_settings, buttonSize, 0, getString(R.string.lbl_series_settings), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //show recording options
                    showRecordingOptions(mSeriesTimerInfo.getId(), mBaseItem, true);
                }
            }));

            //Delete
            final Activity activity = this;
            TextUnderButton del = TextUnderButton.create(this, R.drawable.ic_trash, buttonSize, 0, getString(R.string.lbl_cancel_series), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.lbl_delete)
                            .setMessage(getString(R.string.msg_cancel_entire_series))
                            .setPositiveButton(R.string.lbl_cancel_series, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    apiClient.getValue().CancelLiveTvSeriesTimerAsync(mSeriesTimerInfo.getId(), new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            Utils.showToast(activity, mSeriesTimerInfo.getName() + " Canceled");
                                            dataRefreshService.getValue().setLastDeletedItemId(mSeriesTimerInfo.getId());
                                            finish();
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            Utils.showToast(activity, ex.getLocalizedMessage());
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
        moreButton = TextUnderButton.create(this, R.drawable.ic_more, buttonSize, 0, getString(R.string.lbl_other_options), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show popup
                PopupMenu more = new PopupMenu(mActivity, v);
                more.inflate(R.menu.menu_details_more);
                more.setOnMenuItemClickListener(moreMenuListener);

                if (favButton != null && !ViewKt.isVisible(favButton)) {
                    if (mBaseItem.getUserData().getIsFavorite()) {
                        more.getMenu().getItem(0).setVisible(false);
                        more.getMenu().getItem(1).setVisible(true);
                    } else {
                        more.getMenu().getItem(0).setVisible(true);
                        more.getMenu().getItem(1).setVisible(false);
                    }
                } else {
                    more.getMenu().getItem(0).setVisible(false);
                    more.getMenu().getItem(1).setVisible(false);
                }

                if (trailerButton == null || ViewKt.isVisible(trailerButton)) {
                    more.getMenu().getItem(2).setVisible(false);
                } else if (trailerButton != null) {
                    more.getMenu().getItem(2).setVisible(true);
                }

                if (goToSeriesButton == null || ViewKt.isVisible(goToSeriesButton)) {
                    more.getMenu().getItem(3).setVisible(false);
                } else if (goToSeriesButton != null) {
                    more.getMenu().getItem(3).setVisible(true);
                }

                if (shuffleButton == null || ViewKt.isVisible(shuffleButton)) {
                    more.getMenu().getItem(4).setVisible(false);
                } else if (shuffleButton != null){
                    more.getMenu().getItem(4).setVisible(true);
                }

                if (queueButton == null || ViewKt.isVisible(queueButton)) {
                    more.getMenu().getItem(5).setVisible(false);
                } else if (queueButton != null) {
                    more.getMenu().getItem(5).setVisible(true);
                }

                if (deleteButton == null || ViewKt.isVisible(deleteButton)) {
                    more.getMenu().getItem(6).setVisible(false);
                } else if (deleteButton != null) {
                    more.getMenu().getItem(6).setVisible(true);
                }

                more.show();
            }
        });

        moreButton.setVisibility(View.GONE);
        mDetailsOverviewRow.addAction(moreButton);
        if (mBaseItem.getBaseItemType() != BaseItemType.Episode) showMoreButtonIfNeeded();  //Episodes check for previous and then call this above

    }

    private void addVersionsMenu(View v) {
        PopupMenu menu = new PopupMenu(this, v, Gravity.END);

        for (int i = 0; i< versions.size(); i++) {
            MenuItem item = menu.getMenu().add(Menu.NONE, i, Menu.NONE, versions.get(i).getName());
            item.setChecked(i == selectedVersionPopupIndex);
        }

        menu.getMenu().setGroupCheckable(0,true,false);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                selectedVersionPopupIndex = menuItem.getItemId();
                apiClient.getValue().GetItemAsync(versions.get(selectedVersionPopupIndex).getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        mBaseItem = response;
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
        // added in order of priority
        if (trailerButton != null) actionsList.add(trailerButton);
        if (favButton != null) actionsList.add(favButton);
        if (goToSeriesButton != null) actionsList.add(goToSeriesButton);
        if (shuffleButton != null) actionsList.add(shuffleButton);
        if (queueButton != null) actionsList.add(queueButton);
        if (deleteButton != null) actionsList.add(deleteButton);

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

    PopupMenu.OnMenuItemClickListener moreMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.addFav:
                case R.id.remFav:
                    toggleFavorite();
                    return true;
                case R.id.playTrailers:
                    playTrailers();
                case R.id.gotoSeries:
                    gotoSeries();
                    return true;
                case R.id.shuffleAll:
                    play(mBaseItem, 0, true);
                    return true;
                case R.id.addQueue:
                    addItemToQueue();
                    return true;
                case R.id.delete:
                    deleteItem();
                    return true;
            }
            return false;
        }
    };

    private PopupMenu.OnMenuItemClickListener playWithMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {

                case R.id.play_with_vlc:
                    systemPreferences.getValue().set(SystemPreferences.Companion.getChosenPlayer(),PreferredVideoPlayer.VLC);
                    play(mBaseItem, 0, false);
                    return true;
                case R.id.play_with_exo:
                    systemPreferences.getValue().set(SystemPreferences.Companion.getChosenPlayer(),PreferredVideoPlayer.EXOPLAYER);
                    play(mBaseItem, 0, false);
                    return true;
                case R.id.play_with_external:
                    systemPreferences.getValue().set(SystemPreferences.Companion.getChosenPlayer(),PreferredVideoPlayer.EXTERNAL);
                    PlaybackHelper.getItemsToPlay(mBaseItem, false , false, new Response<List<BaseItemDto>>() {
                        @Override
                        public void onResponse(List<BaseItemDto> response) {
                            if (mBaseItem.getBaseItemType() == BaseItemType.MusicArtist) {
                                mediaManager.getValue().playNow(FullDetailsActivity.this, response, false);
                            } else {
                                Intent intent = new Intent(FullDetailsActivity.this, ExternalPlayerActivity.class);
                                mediaManager.getValue().setCurrentVideoQueue(response);
                                intent.putExtra("Position", 0);
                                startActivity(intent);
                            }
                        }
                    });
                    return true;

            }
            return false;
        }
    };

    RecordPopup mRecordPopup;
    public void showRecordingOptions(String id, final BaseItemDto program, final boolean recordSeries) {
        if (mRecordPopup == null) {
            int width = Utils.convertDpToPixel(this, 600);
            Point size = new Point();
            getWindowManager().getDefaultDisplay().getSize(size);
            mRecordPopup = new RecordPopup(this, mRowsFragment.getView(), (size.x/2) - (width/2), mRowsFragment.getView().getTop()+40, width);
        }
        apiClient.getValue().GetLiveTvSeriesTimerAsync(id, new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                if (recordSeries || Utils.isTrue(program.getIsSports())){
                    mRecordPopup.setContent(mActivity, program, response, mActivity, recordSeries);
                    mRecordPopup.show();
                } else {
                    //just record with defaults
                    apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                        @Override
                        public void onResponse() {
                            // we have to re-retrieve the program to get the timer id
                            apiClient.getValue().GetLiveTvProgramAsync(mProgramInfo.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    setRecTimer(response.getTimerId());
                                }
                            });
                            Utils.showToast(mActivity, R.string.msg_set_to_record);
                        }

                        @Override
                        public void onError(Exception ex) {
                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
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
            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(), ((BaseRowItem)item).getIndex(), mActivity);
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
        apiClient.getValue().MarkPlayedAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), null, new Response<UserItemDataDto>() {
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
        apiClient.getValue().MarkUnplayedAsync(mBaseItem.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<UserItemDataDto>() {
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

    protected void play(final BaseItemDto item, final int pos, final boolean shuffle) {
        PlaybackHelper.getItemsToPlay(item, pos == 0 && item.getBaseItemType() == BaseItemType.Movie, shuffle, new Response<List<BaseItemDto>>() {
            @Override
            public void onResponse(List<BaseItemDto> response) {
                PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
                if (playbackLauncher.interceptPlayRequest(FullDetailsActivity.this, item)) return;

                if (item.getBaseItemType() == BaseItemType.MusicArtist) {
                    mediaManager.getValue().playNow(FullDetailsActivity.this, response, shuffle);
                } else {
                    Class activity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(item.getBaseItemType());
                    Intent intent = new Intent(FullDetailsActivity.this, activity);
                    mediaManager.getValue().setCurrentVideoQueue(response);
                    intent.putExtra("Position", pos);
                    startActivity(intent);
                }
            }
        });

    }

    protected void play(final BaseItemDto[] items, final int pos, final boolean shuffle) {
        List<BaseItemDto> itemsToPlay = Arrays.asList(items);
        Class activity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(items[0].getBaseItemType());
        Intent intent = new Intent(this, activity);
        if (shuffle) Collections.shuffle(itemsToPlay);
        mediaManager.getValue().setCurrentVideoQueue(itemsToPlay);
        intent.putExtra("Position", pos);
        startActivity(intent);

    }
}
