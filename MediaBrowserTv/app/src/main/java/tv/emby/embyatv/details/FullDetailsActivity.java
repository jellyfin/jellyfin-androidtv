package tv.emby.embyatv.details;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.PersonType;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.livetv.TimerQuery;
import mediabrowser.model.querying.EpisodeQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.SeasonQuery;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.querying.UpcomingEpisodesQuery;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.base.IMessageListener;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.livetv.TvManager;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.model.InfoItem;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.CustomListRowPresenter;
import tv.emby.embyatv.presentation.InfoCardPresenter;
import tv.emby.embyatv.presentation.MyDetailsOverviewRowPresenter;
import tv.emby.embyatv.presentation.MyPicassoBackgroundManagerTarget;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.SpecialsQuery;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.querying.TrailersQuery;
import tv.emby.embyatv.ui.IRecordingIndicatorView;
import tv.emby.embyatv.ui.RecordPopup;
import tv.emby.embyatv.ui.TextUnderButton;
import tv.emby.embyatv.util.DelayedMessage;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 2/19/2015.
 */
public class FullDetailsActivity extends BaseActivity implements IRecordingIndicatorView {

    private int BUTTON_SIZE;

    private TextUnderButton mResumeButton;
    private TextUnderButton mPrevButton;
    private TextUnderButton mRecordButton;
    private TextUnderButton mRecSeriesButton;
    private TextUnderButton mSeriesSettingsButton;
    private TextUnderButton mWatchedToggleButton;

    private Target mBackgroundTarget;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    protected BaseItemDto mProgramInfo;
    protected SeriesTimerInfoDto mSeriesTimerInfo;
    protected String mItemId;
    protected String mChannelId;
    protected BaseRowItem mCurrentItem;
    private Calendar mLastUpdated;
    private String mPrevItemId;

    private RowsFragment mRowsFragment;
    private ArrayObjectAdapter mRowsAdapter;

    private MyDetailsOverviewRowPresenter mDorPresenter;
    private MyDetailsOverviewRow mDetailsOverviewRow;
    private CustomListRowPresenter mListRowPresenter;
    private Drawable mRowBackground = TvApp.getApplication().getResources().getDrawable(R.drawable.dark_gradient);

    private TvApp mApplication;
    private FullDetailsActivity mActivity;
    private Handler mLoopHandler = new Handler();
    private Runnable mBackdropLoop;
    private Runnable mClockLoop;
    public static int BACKDROP_ROTATION_INTERVAL = 8000;
    private Typeface roboto;

    private BaseItemDto mBaseItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_details);

        BUTTON_SIZE = Utils.convertDpToPixel(this, 40);
        mApplication = TvApp.getApplication();
        mActivity = this;
        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        backgroundManager.attach(getWindow());
        backgroundManager.setDimLayer(getDrawable(R.drawable.left_fade));

        if (TvApp.getApplication().getCurrentBackground() != null) {
            int[] colors = new int[2];
            colors[0] = Utils.darker(Palette.from(TvApp.getApplication().getCurrentBackground()).generate().getMutedColor(TvApp.getApplication().getResources().getColor(R.color.black_transparent)), .6f);
            colors[1] = Utils.darker(colors[0], .1f);

            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, colors);
            gd.setCornerRadius(0f);
            gd.setGradientCenter(.6f, .5f);
            gd.setAlpha(200);
            mRowBackground = gd;
        }

        mBackgroundTarget = new MyPicassoBackgroundManagerTarget(backgroundManager);
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mRowsFragment = new RowsFragment();
        getFragmentManager().beginTransaction().add(R.id.rowsFragment, mRowsFragment).commit();

        mRowsFragment.setOnItemViewClickedListener(new ItemViewClickedListener());
        mRowsFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());

        mDorPresenter = new MyDetailsOverviewRowPresenter();

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mItemId = getIntent().getStringExtra("ItemId");
        mChannelId = getIntent().getStringExtra("ChannelId");
        String programJson = getIntent().getStringExtra("ProgramInfo");
        if (programJson != null) mProgramInfo = mApplication.getSerializer().DeserializeFromString(programJson, BaseItemDto.class);
        String timerJson = getIntent().getStringExtra("SeriesTimer");
        if (timerJson != null) mSeriesTimerInfo = mApplication.getSerializer().DeserializeFromString(timerJson, SeriesTimerInfoDto.class);

        registerMessageListener(new IMessageListener() {
            @Override
            public void onMessageReceived(CustomMessage message) {
                if (message == CustomMessage.ActionComplete && mSeriesTimerInfo != null && "SeriesTimer".equals(mBaseItem.getType())) {
                    //update info
                    mApplication.getApiClient().GetLiveTvSeriesTimerAsync(mSeriesTimerInfo.getId(), new Response<SeriesTimerInfoDto>() {
                        @Override
                        public void onResponse(SeriesTimerInfoDto response) {
                            mSeriesTimerInfo = response;
                            mBaseItem.setOverview(Utils.buildOverview(mSeriesTimerInfo));
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

    @Override
    protected void onResume() {
        super.onResume();

        startClock();
        rotateBackdrops();

        //Update information that may have changed - delay slightly to allow changes to take on the server
        if (mApplication.getLastPlayback().after(mLastUpdated) && !"MusicArtist".equals(mBaseItem.getType())) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if ("Episode".equals(mBaseItem.getType()) && mApplication.getLastPlayback().after(mLastUpdated) && mApplication.getLastPlayedItem() != null && !mBaseItem.getId().equals(mApplication.getLastPlayedItem().getId()) && "Episode".equals(mApplication.getLastPlayedItem().getType())) {
                        mApplication.getLogger().Info("Re-loading after new episode playback");
                        loadItem(mApplication.getLastPlayedItem().getId());
                        mApplication.setLastPlayedItem(null); //blank this out so a detail screen we back up to doesn't also do this
                    } else {
                        mApplication.getLogger().Debug("Updating info after playback");
                        mApplication.getApiClient().GetItemAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
                            @Override
                            public void onResponse(BaseItemDto response) {
                                if (!isFinishing()) {
                                    mBaseItem = response;
                                    if (mResumeButton != null) {
                                        mResumeButton.setVisibility(("Series".equals(mBaseItem.getType()) && ! mBaseItem.getUserData().getPlayed()) || response.getCanResume() ? View.VISIBLE : View.GONE);
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
            }, 750);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClock();
        stopRotate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopClock();
        stopRotate();
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mCurrentItem != null) {
            return KeyProcessor.HandleKey(keyCode, mCurrentItem, this) || super.onKeyUp(keyCode, event);

        } else if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) && Utils.CanPlay(mBaseItem)) {
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
                mDorPresenter.updateEndTime(getEndTime());
                mLoopHandler.postDelayed(this, 15000);
            }
        };

        mLoopHandler.postDelayed(mClockLoop, 15000);
    }

    private void stopClock() {
        if (mLoopHandler != null && mClockLoop != null) {
            mLoopHandler.removeCallbacks(mClockLoop);
        }
    }

    private static String[] buttonTypes = new String[] {"Episode","Movie","Series","Season","Folder","Video","Recording","Program","ChannelVideoItem","Trailer","MusicArtist","Person","MusicVideo","SeriesTimer"};
    private static List<String> buttonTypeList = Arrays.asList(buttonTypes);
    private static String[] directPlayableTypes = new String[] {"Episode","Movie","Video","Recording","Program"};
    private static List<String> directPlayableTypeList = Arrays.asList(directPlayableTypes);

    private void updatePoster() {
        if (isFinishing()) return;
        Picasso.with(mActivity)
                .load(Utils.getPrimaryImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true, false, false, posterHeight))
                .skipMemoryCache()
                .resize(posterWidth, posterHeight)
                .centerInside()
                .into(mDorPresenter.getPosterView());
    }

    private void updateWatched() {
        if (mWatchedToggleButton != null && mBaseItem != null && mBaseItem.getUserData() != null && !isFinishing()) {
            mWatchedToggleButton.setImageResource(mBaseItem.getUserData().getPlayed() ? R.drawable.redcheck : R.drawable.whitecheck);
        }
    }

    private void loadItem(String id) {
        if (mChannelId != null && mProgramInfo == null) {
            // if we are displaying a live tv channel - we want to get whatever is showing now on that channel
            final FullDetailsActivity us = this;
            mApplication.getApiClient().GetLiveTvChannelAsync(mChannelId, TvApp.getApplication().getCurrentUser().getId(), new Response<ChannelInfoDto>() {
                @Override
                public void onResponse(ChannelInfoDto response) {
                    mProgramInfo = response.getCurrentProgram();
                    mItemId = mProgramInfo.getId();
                    mApplication.getApiClient().GetItemAsync(mItemId, mApplication.getCurrentUser().getId(), new DetailItemLoadResponse(us));

                }
            });
        } else if (mSeriesTimerInfo != null) {
            // create base item from our timer
            BaseItemDto item = new BaseItemDto();
            item.setId(mSeriesTimerInfo.getId());
            item.setSeriesTimerId(mSeriesTimerInfo.getId());
            item.setType("SeriesTimer");
            item.setName(mSeriesTimerInfo.getName());
            item.setOverview(Utils.buildOverview(mSeriesTimerInfo));

            setBaseItem(item);
        } else {
            mApplication.getApiClient().GetItemAsync(id, mApplication.getCurrentUser().getId(), new DetailItemLoadResponse(this));
        }

        mLastUpdated = Calendar.getInstance();
    }

    @Override
    public void setRecTimer(String id) {
        mProgramInfo.setTimerId(id);
        if (mRecordButton != null) mRecordButton.setImageResource(id == null ? R.drawable.recwhite : R.drawable.rec);
    }

    private int posterWidth;
    private int posterHeight;

    @Override
    public void setRecSeriesTimer(String id) {
        if (mProgramInfo != null) mProgramInfo.setSeriesTimerId(id);
        if (mRecSeriesButton != null) mRecSeriesButton.setImageResource(id == null ? R.drawable.recserieswhite : R.drawable.recseries);
        if (mSeriesSettingsButton != null) mSeriesSettingsButton.setVisibility(id == null ? View.GONE : View.VISIBLE);

    }

    private class BuildDorTask extends AsyncTask<BaseItemDto, Integer, MyDetailsOverviewRow> {

        @Override
        protected MyDetailsOverviewRow doInBackground(BaseItemDto... params) {
            BaseItemDto item = params[0];

            // Figure image size
            Double aspect = Utils.getImageAspectRatio(item, false);
            posterHeight = aspect > 1 ? Utils.convertDpToPixel(mActivity, 160) : Utils.convertDpToPixel(mActivity, 200);
            posterWidth = (int)((aspect) * posterHeight);
            if (posterHeight < 10) posterWidth = Utils.convertDpToPixel(mActivity, 150);  //Guard against zero size images causing picasso to barf

            String primaryImageUrl = Utils.getLogoImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), 600);
            if (primaryImageUrl == null) primaryImageUrl = Utils.getPrimaryImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true, false, false, posterHeight);
            mDetailsOverviewRow = new MyDetailsOverviewRow(item);

            mDetailsOverviewRow.setSummary(item.getOverview());
            switch (item.getType()) {
                case "Person":
                case "MusicArtist":
                    mDetailsOverviewRow.setSummarySubTitle("");
                    break;
                default:

                    BaseItemPerson director = Utils.GetFirstPerson(item, PersonType.Director);

                    mDetailsOverviewRow.setInfoItem1(new InfoItem(getString(R.string.lbl_directed_by), director != null ? director.getName() : getString(R.string.lbl_bracket_unknown)));

                    if ((item.getRunTimeTicks() != null && item.getRunTimeTicks() > 0) || item.getOriginalRunTimeTicks() != null) {
                        mDetailsOverviewRow.setInfoItem2(new InfoItem(getString(R.string.lbl_runs), getRunTime()));
                        mDetailsOverviewRow.setInfoItem3(new InfoItem(getString(R.string.lbl_ends), getEndTime()));
                    } else {
                        mDetailsOverviewRow.setInfoItem2(new InfoItem());
                        mDetailsOverviewRow.setInfoItem3(new InfoItem());
                    }

            }
            try {
                //Main image
                Bitmap poster = Picasso.with(mActivity)
                        .load(primaryImageUrl)
                        .skipMemoryCache()
                        .get();
                mDetailsOverviewRow.setImageBitmap(mActivity, poster);

                //Studio image
                int height = Utils.convertDpToPixel(mActivity, 40);
                int width = Utils.convertDpToPixel(mActivity, 100);
                if (item.getStudios() != null && item.getStudios().length > 0 && item.getStudios()[0].getHasPrimaryImage()) {
                    String studioImageUrl = Utils.getPrimaryImageUrl(item.getStudios()[0], mApplication.getApiClient(), height);
                    if (studioImageUrl != null) mDetailsOverviewRow.setStudioBitmap(mActivity, Picasso.with(mActivity).load(studioImageUrl).resize(width, height).centerInside().get());
                } else {
                    if (item.getSeriesStudio() != null) {
                        String studioImageUrl = null;
                        try {
                            ImageOptions options = new ImageOptions();
                            options.setMaxHeight(height);
                            options.setImageType(ImageType.Primary);
                            studioImageUrl = mApplication.getApiClient().GetStudioImageUrl(URLEncoder.encode(item.getSeriesStudio(), "utf-8"), options);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        if (studioImageUrl != null) mDetailsOverviewRow.setStudioBitmap(mActivity, Picasso.with(mActivity).load(studioImageUrl).resize(width, height).centerInside().get());

                    }
                }

            } catch (IOException e) {
                TvApp.getApplication().getLogger().ErrorException("Error loading image", e);

            }

            return mDetailsOverviewRow;
        }

        @Override
        protected void onPostExecute(MyDetailsOverviewRow detailsOverviewRow) {
            super.onPostExecute(detailsOverviewRow);

            if (isFinishing()) return;

            ClassPresenterSelector ps = new ClassPresenterSelector();
            ps.addClassPresenter(MyDetailsOverviewRow.class, mDorPresenter);
            mListRowPresenter = new CustomListRowPresenter(mRowBackground, Utils.convertDpToPixel(mActivity, 10));
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
        TvApp.getApplication().getLogger().Debug("Item type: "+mBaseItem.getType());
        switch (mBaseItem.getType()) {
            case "Movie":

                //Cast/Crew
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, castAdapter, 0, mActivity.getString(R.string.lbl_cast_crew));
                }

                //Specials
                if (mBaseItem.getSpecialFeatureCount() != null && mBaseItem.getSpecialFeatureCount() > 0) {
                    addItemRow(adapter, new ItemRowAdapter(new SpecialsQuery(mBaseItem.getId()), new CardPresenter(), adapter), 2, mActivity.getString(R.string.lbl_specials));
                }

                //Trailers
                if (mBaseItem.getLocalTrailerCount() != null && mBaseItem.getLocalTrailerCount() > 1) {
                    addItemRow(adapter, new ItemRowAdapter(new TrailersQuery(mBaseItem.getId()), new CardPresenter(), adapter), 3, mActivity.getString(R.string.lbl_trailers));
                }

                //Chapters
                if (mBaseItem.getChapters() != null && mBaseItem.getChapters().size() > 0) {
                    List<ChapterItemInfo> chapters = Utils.buildChapterItems(mBaseItem);
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(chapters, new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, chapterAdapter, 1, mActivity.getString(R.string.lbl_chapters));
                }

                //Similar
                SimilarItemsQuery similar = new SimilarItemsQuery();
                similar.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
                similar.setUserId(TvApp.getApplication().getCurrentUser().getId());
                similar.setId(mBaseItem.getId());
                similar.setLimit(10);

                ItemRowAdapter similarMoviesAdapter = new ItemRowAdapter(similar, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarMoviesAdapter, 4, mActivity.getString(R.string.lbl_similar_movies));

                addInfoRows(adapter);
                break;
            case "Trailer":

                //Cast/Crew
                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter castAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, castAdapter, 0, mActivity.getString(R.string.lbl_cast_crew));
                }

                //Similar
                SimilarItemsQuery similarTrailer = new SimilarItemsQuery();
                similarTrailer.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
                similarTrailer.setUserId(TvApp.getApplication().getCurrentUser().getId());
                similarTrailer.setId(mBaseItem.getId());
                similarTrailer.setLimit(10);

                ItemRowAdapter similarTrailerAdapter = new ItemRowAdapter(similarTrailer, QueryType.SimilarMovies, new CardPresenter(), adapter);
                addItemRow(adapter, similarTrailerAdapter, 4, mActivity.getString(R.string.lbl_similar_movies));
                addInfoRows(adapter);
                break;
            case "Person":

                ItemQuery personMovies = new ItemQuery();
                personMovies.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                personMovies.setUserId(TvApp.getApplication().getCurrentUser().getId());
                personMovies.setPersonIds(new String[] {mBaseItem.getId()});
                personMovies.setRecursive(true);
                personMovies.setIncludeItemTypes(new String[] {"Movie"});
                ItemRowAdapter personMoviesAdapter = new ItemRowAdapter(personMovies, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personMoviesAdapter, 0, mApplication.getString(R.string.lbl_movies));

                ItemQuery personSeries = new ItemQuery();
                personSeries.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.DisplayPreferencesId});
                personSeries.setUserId(TvApp.getApplication().getCurrentUser().getId());
                personSeries.setPersonIds(new String[] {mBaseItem.getId()});
                personSeries.setRecursive(true);
                personSeries.setIncludeItemTypes(new String[] {"Series", "Episode"});
                ItemRowAdapter personSeriesAdapter = new ItemRowAdapter(personSeries, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, personSeriesAdapter, 1, mApplication.getString(R.string.lbl_tv_series));

                break;
            case "MusicArtist":

                ItemQuery artistAlbums = new ItemQuery();
                artistAlbums.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                artistAlbums.setUserId(TvApp.getApplication().getCurrentUser().getId());
                artistAlbums.setArtistIds(new String[]{mBaseItem.getId()});
                artistAlbums.setRecursive(true);
                artistAlbums.setIncludeItemTypes(new String[]{"MusicAlbum"});
                ItemRowAdapter artistAlbumsAdapter = new ItemRowAdapter(artistAlbums, 100, false, new CardPresenter(), adapter);
                addItemRow(adapter, artistAlbumsAdapter, 0, mApplication.getString(R.string.lbl_albums));

                break;
            case "Series":
                NextUpQuery nextUpQuery = new NextUpQuery();
                nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
                nextUpQuery.setSeriesId(mBaseItem.getId());
                nextUpQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                ItemRowAdapter nextUpAdapter = new ItemRowAdapter(nextUpQuery, false, new CardPresenter(), adapter);
                addItemRow(adapter, nextUpAdapter, 0, mApplication.getString(R.string.lbl_next_up));

                SeasonQuery seasons = new SeasonQuery();
                seasons.setSeriesId(mBaseItem.getId());
                seasons.setUserId(TvApp.getApplication().getCurrentUser().getId());
                seasons.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.DisplayPreferencesId});
                ItemRowAdapter seasonsAdapter = new ItemRowAdapter(seasons, new CardPresenter(), adapter);
                addItemRow(adapter, seasonsAdapter, 1, mActivity.getString(R.string.lbl_seasons));

                UpcomingEpisodesQuery upcoming = new UpcomingEpisodesQuery();
                upcoming.setUserId(TvApp.getApplication().getCurrentUser().getId());
                upcoming.setParentId(mBaseItem.getId());
                upcoming.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
                ItemRowAdapter upcomingAdapter = new ItemRowAdapter(upcoming, new CardPresenter(), adapter);
                addItemRow(adapter, upcomingAdapter, 2, mActivity.getString(R.string.lbl_upcoming));

                if (mBaseItem.getPeople() != null && mBaseItem.getPeople().length > 0) {
                    ItemRowAdapter seriesCastAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, seriesCastAdapter, 3, mApplication.getString(R.string.lbl_cast_crew));

                }

                SimilarItemsQuery similarSeries = new SimilarItemsQuery();
                similarSeries.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.DisplayPreferencesId});
                similarSeries.setUserId(TvApp.getApplication().getCurrentUser().getId());
                similarSeries.setId(mBaseItem.getId());
                similarSeries.setLimit(20);
                ItemRowAdapter similarAdapter = new ItemRowAdapter(similarSeries, QueryType.SimilarSeries, new CardPresenter(), adapter);
                addItemRow(adapter, similarAdapter, 4, mActivity.getString(R.string.lbl_similar_series));
                break;

            case "Episode":
                if (mBaseItem.getSeasonId() != null && mBaseItem.getIndexNumber() != null) {
                    StdItemQuery nextEpisodes = new StdItemQuery();
                    nextEpisodes.setParentId(mBaseItem.getSeasonId());
                    nextEpisodes.setIncludeItemTypes(new String[]{"Episode"});
                    nextEpisodes.setStartIndex(mBaseItem.getIndexNumber()); // query index is zero-based but episode no is not
                    nextEpisodes.setLimit(20);
                    ItemRowAdapter nextAdapter = new ItemRowAdapter(nextEpisodes, 0 , false, true, new CardPresenter(true, 240), adapter);
                    addItemRow(adapter, nextAdapter, 5, "Next Episodes");
                }
                addInfoRows(adapter);
                break;

            case "SeriesTimer":
                TimerQuery scheduled = new TimerQuery();
                scheduled.setSeriesTimerId(mSeriesTimerInfo.getId());
                TvManager.getScheduleRowsAsync(scheduled, new CardPresenter(true), adapter, new Response<Integer>());
                break;
        }


    }

    private void addInfoRows(ArrayObjectAdapter adapter) {
        if (TvApp.getApplication().getPrefs().getBoolean("pref_enable_debug", false) && mBaseItem.getMediaSources() != null) {
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
        if (buttonTypeList.contains(item.getType())) addButtons(BUTTON_SIZE);
//        updatePlayedDate();
//
        updateBackground(Utils.getBackdropImageUrl(item, TvApp.getApplication().getApiClient(), true));

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

    private String getRunTime() {
        Long runtime = Utils.NullCoalesce(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
        return runtime != null && runtime > 0 ? runtime / 600000000 + getString(R.string.lbl_min) : "";
    }

    private String getEndTime() {
        if (mBaseItem != null) {
            Long runtime = Utils.NullCoalesce(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
            if (runtime != null && runtime > 0) {
                long endTimeTicks = "Program".equals(mBaseItem.getType()) && mBaseItem.getEndDate() != null ? Utils.convertToLocalDate(mBaseItem.getEndDate()).getTime() : System.currentTimeMillis() + runtime / 10000;
                if (mBaseItem.getCanResume()) {
                    endTimeTicks = System.currentTimeMillis() + ((runtime - mBaseItem.getUserData().getPlaybackPositionTicks()) / 10000);
                    return android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks));
                } else {
                    return android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks));
                }
            }

        }
        return "";
    }

    private void addItemToQueue() {
        if ("Audio".equals(mBaseItem.getType())) {
            MediaManager.addToAudioQueue(Arrays.asList(mBaseItem));

        } else {
            MediaManager.addToVideoQueue(mBaseItem);
        }
    }

    private void toggleFavorite() {
        UserItemDataDto data = mBaseItem.getUserData();
        mApplication.getApiClient().UpdateFavoriteStatusAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                favButton.setImageResource(response.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart);
                TvApp.getApplication().setLastFavoriteUpdate(System.currentTimeMillis());
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
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final DelayedMessage msg = new DelayedMessage(mActivity, 150);
                        TvApp.getApplication().getApiClient().DeleteItem(mBaseItem.getId(), new EmptyResponse() {
                            @Override
                            public void onResponse() {
                                msg.Cancel();
                                Utils.showToast(mActivity, mBaseItem.getName() + " Deleted");
                                TvApp.getApplication().setLastDeletedItemId(mBaseItem.getId());
                                finish();
                            }

                            @Override
                            public void onError(Exception ex) {
                                msg.Cancel();
                                Utils.showToast(mActivity, ex.getLocalizedMessage());
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(mActivity, "Item NOT Deleted");
                    }
                })
                .show();


    }

    private TextUnderButton favButton = null;
    private TextUnderButton goToSeriesButton = null;
    private TextUnderButton queueButton = null;
    private TextUnderButton deleteButton = null;
    private TextUnderButton moreButton;

    private void addButtons(int buttonSize) {
        mResumeButton = new TextUnderButton(this, R.drawable.resume, buttonSize, "Series".equals(mBaseItem.getType()) ? getString(R.string.lbl_play_next_up) : getString(R.string.lbl_resume), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("Series".equals(mBaseItem.getType())) {
                    //play next up
                    NextUpQuery nextUpQuery = new NextUpQuery();
                    nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
                    nextUpQuery.setSeriesId(mBaseItem.getId());
                    TvApp.getApplication().getApiClient().GetNextUpEpisodesAsync(nextUpQuery, new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getItems().length > 0) {
                                play(response.getItems()[0], 0 , false);
                            } else {
                                Utils.showToast(TvApp.getApplication(), "Unable to find next up episode");
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            TvApp.getApplication().getLogger().ErrorException("Error playing next up episode", exception);
                            Utils.showToast(TvApp.getApplication(), getString(R.string.msg_video_playback_error));
                        }
                    });
                } else {
                    //resume
                    Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
                    play(mBaseItem, pos.intValue(), false);

                }
            }
        });

        if (Utils.CanPlay(mBaseItem)) {
            mDetailsOverviewRow.addAction(mResumeButton);
            mResumeButton.setVisibility(("Series".equals(mBaseItem.getType()) && ! mBaseItem.getUserData().getPlayed()) || (mBaseItem.getCanResume() ) ? View.VISIBLE : View.GONE);

            TextUnderButton play = new TextUnderButton(this, R.drawable.play, buttonSize, getString(Utils.isLiveTv(mBaseItem) ? R.string.lbl_tune_to_channel : mBaseItem.getIsFolder() ? R.string.lbl_play_all : R.string.lbl_play), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    play(mBaseItem, 0, false);
                }
            });
            mDetailsOverviewRow.addAction(play);

            if (!mBaseItem.getIsFolder() && !Utils.isLiveTv(mBaseItem)) {
                queueButton = new TextUnderButton(this, R.drawable.addtoqueue, buttonSize, getString(R.string.lbl_add_to_queue), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addItemToQueue();
                    }
                });
                mDetailsOverviewRow.addAction(queueButton);
            }

            if (mBaseItem.getIsFolder()) {
                TextUnderButton shuffle = new TextUnderButton(this, R.drawable.shuffle, buttonSize, getString(R.string.lbl_shuffle_all), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        play(mBaseItem, 0, true);
                    }
                });
                mDetailsOverviewRow.addAction(shuffle);
            }

            if ("MusicArtist".equals(mBaseItem.getType())) {
                TextUnderButton imix = new TextUnderButton(this, R.drawable.mix, buttonSize, getString(R.string.lbl_instant_mix), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Utils.Beep();
                        Utils.playInstantMix(mBaseItem.getId());
                    }
                });
                mDetailsOverviewRow.addAction(imix);
            }

        }

        if (mBaseItem.getLocalTrailerCount() != null && mBaseItem.getLocalTrailerCount() > 0) {
            TextUnderButton trailer = new TextUnderButton(this, R.drawable.trailer, buttonSize, getString(R.string.lbl_play_trailers), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TvApp.getApplication().getApiClient().GetLocalTrailersAsync(TvApp.getApplication().getCurrentUser().getId(), mBaseItem.getId(), new Response<BaseItemDto[]>() {
                        @Override
                        public void onResponse(BaseItemDto[] response) {
                            play(response, 0, false);
                        }

                        @Override
                        public void onError(Exception exception) {
                            TvApp.getApplication().getLogger().ErrorException("Error retrieving trailers for playback", exception);
                            Utils.showToast(mActivity, R.string.msg_video_playback_error);
                        }
                    });

                }
            });

            mDetailsOverviewRow.addAction(trailer);
        }

        if (mProgramInfo != null && mApplication.canManageRecordings()) {
            if (Utils.convertToLocalDate(mBaseItem.getEndDate()).getTime() > System.currentTimeMillis()) {
                //Record button
                mRecordButton = new TextUnderButton(this, mProgramInfo.getTimerId() != null ? R.drawable.rec : R.drawable.recwhite, buttonSize, getString(R.string.lbl_record), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getTimerId() == null) {
                            //Create one-off recording with defaults
                            mApplication.getApiClient().GetDefaultLiveTvTimerInfo(mProgramInfo.getId(), new Response<SeriesTimerInfoDto>() {
                                @Override
                                public void onResponse(SeriesTimerInfoDto response) {
                                    mApplication.getApiClient().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            // we have to re-retrieve the program to get the timer id
                                            TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramInfo.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
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
                                            mApplication.getLogger().ErrorException("Error creating recording", ex);
                                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception exception) {
                                    mApplication.getLogger().ErrorException("Error creating recording", exception);
                                    Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                }
                            });
                        } else {
                            TvApp.getApplication().getApiClient().CancelLiveTvTimerAsync(mProgramInfo.getTimerId(), new EmptyResponse() {
                                @Override
                                public void onResponse() {
                                    setRecTimer(null);
                                    TvApp.getApplication().setLastDeletedItemId(mProgramInfo.getId());
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

                mDetailsOverviewRow.addAction(mRecordButton);
            }

            if (mProgramInfo.getIsSeries() != null && mProgramInfo.getIsSeries()) {
                mRecSeriesButton= new TextUnderButton(this, mProgramInfo.getSeriesTimerId() != null ? R.drawable.recseries : R.drawable.recserieswhite, buttonSize, getString(R.string.lbl_record_series), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getSeriesTimerId() == null) {
                            //Create series recording with default options
                            mApplication.getApiClient().GetDefaultLiveTvTimerInfo(mProgramInfo.getId(), new Response<SeriesTimerInfoDto>() {
                                @Override
                                public void onResponse(SeriesTimerInfoDto response) {
                                    mApplication.getApiClient().CreateLiveTvSeriesTimerAsync(response, new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            // we have to re-retrieve the program to get the timer id
                                            TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramInfo.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
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
                                            mApplication.getLogger().ErrorException("Error creating recording", ex);
                                            Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception exception) {
                                    mApplication.getLogger().ErrorException("Error creating recording", exception);
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
                                            TvApp.getApplication().getApiClient().CancelLiveTvSeriesTimerAsync(mProgramInfo.getSeriesTimerId(), new EmptyResponse() {
                                                @Override
                                                public void onResponse() {
                                                    setRecSeriesTimer(null);
                                                    setRecTimer(null);
                                                    TvApp.getApplication().setLastDeletedItemId(mProgramInfo.getId());
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

                mDetailsOverviewRow.addAction(mRecSeriesButton);

                mSeriesSettingsButton = new TextUnderButton(this, R.drawable.cog, buttonSize, getString(R.string.lbl_series_settings), new View.OnClickListener() {
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
            if (!"MusicArtist".equals(mBaseItem.getType()) && !"Person".equals(mBaseItem.getType())) {
                mWatchedToggleButton = new TextUnderButton(this, userData.getPlayed() ? R.drawable.redcheck : R.drawable.whitecheck, buttonSize, getString(R.string.lbl_toggle_watched), markWatchedListener);
                mDetailsOverviewRow.addAction(mWatchedToggleButton);
            }

            //Favorite
            favButton = new TextUnderButton(this, userData.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart, buttonSize, getString(R.string.lbl_toggle_favorite), new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    toggleFavorite();
                }
            });
            mDetailsOverviewRow.addAction(favButton);
        }

        if ("Episode".equals(mBaseItem.getType()) && mBaseItem.getSeriesId() != null) {
            //add the prev button first so it will be there in proper position - we'll show it later if needed
            mPrevButton = new TextUnderButton(this, R.drawable.prev, buttonSize, "Previous Episode", new View.OnClickListener() {
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
            adjacent.setUserId(TvApp.getApplication().getCurrentUser().getId());
            adjacent.setSeriesId(mBaseItem.getSeriesId());
            adjacent.setAdjacentTo(mBaseItem.getId());
            TvApp.getApplication().getApiClient().GetEpisodesAsync(adjacent, new Response<ItemsResult>() {
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
                }
            });

            goToSeriesButton = new TextUnderButton(this, R.drawable.tvicon, buttonSize, getString(R.string.lbl_goto_series), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotoSeries();
                }
            });
            mDetailsOverviewRow.addAction(goToSeriesButton);
        }

        if (("Recording".equals(mBaseItem.getType()) && TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvManagement() && mBaseItem.getCanDelete()) ||
                (("Movie".equals(mBaseItem.getType()) || "Episode".equals(mBaseItem.getType()) || "Video".equals(mBaseItem.getType())) && TvApp.getApplication().getCurrentUser().getPolicy().getEnableContentDeletion())) {
            final Activity activity = this;
            deleteButton = new TextUnderButton(this, R.drawable.trash, buttonSize, getString(R.string.lbl_delete), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteItem();
                }
            });
            mDetailsOverviewRow.addAction(deleteButton);
        }

        if (mSeriesTimerInfo != null && "SeriesTimer".equals(mBaseItem.getType())) {
            //Settings
            mDetailsOverviewRow.addAction(new TextUnderButton(this, R.drawable.cog, buttonSize, getString(R.string.lbl_series_settings), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //show recording options
                    showRecordingOptions(mSeriesTimerInfo.getId(), mBaseItem, true);
                }
            }));

            //Delete
            final Activity activity = this;
            TextUnderButton del = new TextUnderButton(this, R.drawable.trash, buttonSize, getString(R.string.lbl_cancel_series), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.lbl_delete)
                            .setMessage(getString(R.string.msg_cancel_entire_series))
                            .setPositiveButton(R.string.lbl_cancel_series, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    final DelayedMessage msg = new DelayedMessage(activity, 150);
                                    TvApp.getApplication().getApiClient().CancelLiveTvSeriesTimerAsync(mSeriesTimerInfo.getId(), new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            msg.Cancel();
                                            Utils.showToast(activity, mSeriesTimerInfo.getName() + " Canceled");
                                            TvApp.getApplication().setLastDeletedItemId(mSeriesTimerInfo.getId());
                                            finish();
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            msg.Cancel();
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
        moreButton = new TextUnderButton(this, R.drawable.lb_ic_more, buttonSize, "More Options", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show popup
                PopupMenu more = new PopupMenu(mActivity, v);
                more.inflate(R.menu.menu_details_more);
                more.setOnMenuItemClickListener(moreMenuListener);
                if (favButton != null) {
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

                if (queueButton == null) {
                    more.getMenu().getItem(2).setVisible(false);
                }

                if (goToSeriesButton == null) {
                    more.getMenu().getItem(3).setVisible(false);
                }

                if (deleteButton == null) {
                    more.getMenu().getItem(4).setVisible(false);
                }

                more.show();
            }
        });

        moreButton.setVisibility(View.GONE);
        mDetailsOverviewRow.addAction(moreButton);
        showMoreButtonIfNeeded();

    }

    private void showMoreButtonIfNeeded() {
        if (mDetailsOverviewRow.getVisibleActions() > 5) {

            if (favButton != null) favButton.setVisibility(View.GONE);
            if (queueButton != null) queueButton.setVisibility(View.GONE);
            if (goToSeriesButton != null) goToSeriesButton.setVisibility(View.GONE);
            if (deleteButton != null) deleteButton.setVisibility(View.GONE);

            moreButton.setVisibility(View.VISIBLE);
        } else {
            if (favButton != null) favButton.setVisibility(View.VISIBLE);
            if (queueButton != null) queueButton.setVisibility(View.VISIBLE);
            if (goToSeriesButton != null) goToSeriesButton.setVisibility(View.VISIBLE);
            if (deleteButton != null) deleteButton.setVisibility(View.VISIBLE);

            moreButton.setVisibility(View.GONE);

        }
    }

    PopupMenu.OnMenuItemClickListener moreMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.addFav:
                case R.id.remFav:
                    toggleFavorite();
                    return true;
                case R.id.addQueue:
                    addItemToQueue();
                    return true;
                case R.id.gotoSeries:
                    gotoSeries();
                    return true;
                case R.id.delete:
                    deleteItem();
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
            mRecordPopup = new RecordPopup(this, mRowsFragment.getView(), (size.x/2) - (width/2), mRowsFragment.getView().getTop(), width);
        }
        TvApp.getApplication().getApiClient().GetLiveTvSeriesTimerAsync(id, new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                if (recordSeries || Utils.isTrue(program.getIsSports())){
                    mRecordPopup.setContent(program, response, mActivity, recordSeries);
                    mRecordPopup.show();
                } else {
                    //just record with defaults
                    TvApp.getApplication().getApiClient().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                        @Override
                        public void onResponse() {
                            // we have to re-retrieve the program to get the timer id
                            TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramInfo.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
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
            if (mBaseItem.getIsFolder()) {
                new AlertDialog.Builder(mActivity)
                        .setTitle(getString(data.getPlayed() ? R.string.lbl_mark_unplayed : R.string.lbl_mark_played))
                        .setMessage(getString(data.getPlayed() ? R.string.lbl_confirm_mark_unwatched : R.string.lbl_confirm_mark_watched))
                        .setNegativeButton(getString(R.string.lbl_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).setPositiveButton(getString(R.string.lbl_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            if (data.getPlayed())  markUnPlayed(); else markPlayed();
                    }
                }).show();

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
        mApplication.getApiClient().MarkPlayedAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), null, new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                mWatchedToggleButton.setImageResource(R.drawable.redcheck);
                //adjust resume
                if (mResumeButton != null && !mBaseItem.getCanResume())
                    mResumeButton.setVisibility(View.GONE);
                //force lists to re-fetch
                TvApp.getApplication().setLastPlayback(Calendar.getInstance());
                showMoreButtonIfNeeded();
            }
        });

    }

    private void markUnPlayed() {
        mApplication.getApiClient().MarkUnplayedAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                mWatchedToggleButton.setImageResource(R.drawable.whitecheck);
                //adjust resume
                if (mResumeButton != null && !mBaseItem.getCanResume())
                    mResumeButton.setVisibility(View.GONE);
                //force lists to re-fetch
                TvApp.getApplication().setLastPlayback(Calendar.getInstance());
                showMoreButtonIfNeeded();
            }
        });

    }

    protected void play(final BaseItemDto item, final int pos, final boolean shuffle) {
        final Activity activity = this;
        Utils.getItemsToPlay(item, pos == 0 && item.getType().equals("Movie"), shuffle, new Response<List<BaseItemDto>>() {
            @Override
            public void onResponse(List<BaseItemDto> response) {
                if ("MusicArtist".equals(item.getType())) {
                    MediaManager.playNow(response);
                } else {
                    Intent intent = new Intent(activity, mApplication.getPlaybackActivityClass(item.getType()));
                    MediaManager.setCurrentVideoQueue(response);
                    intent.putExtra("Position", pos);
                    startActivity(intent);
                }
            }
        });

    }

    protected void play(final BaseItemDto[] items, final int pos, final boolean shuffle) {
        List<BaseItemDto> itemsToPlay = Arrays.asList(items);
        Intent intent = new Intent(this, mApplication.getPlaybackActivityClass(items[0].getType()));
        if (shuffle) Collections.shuffle(itemsToPlay);
        MediaManager.setCurrentVideoQueue(itemsToPlay);
        intent.putExtra("Position", pos);
        startActivity(intent);

    }

    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true));
                mLoopHandler.postDelayed(this, BACKDROP_ROTATION_INTERVAL);
            }
        };

        mLoopHandler.postDelayed(mBackdropLoop, BACKDROP_ROTATION_INTERVAL);
    }

    private void stopRotate() {
        if (mLoopHandler != null && mBackdropLoop != null) {
            mLoopHandler.removeCallbacks(mBackdropLoop);
        }
    }

    protected void updateBackground(String url) {
        if (url == null) {
            BackgroundManager.getInstance(this).setDrawable(mDefaultBackground);
        } else {
            Picasso.with(this)
                    .load(url)
                    .skipMemoryCache()
                    .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                    .centerCrop()
                    .error(mDefaultBackground)
                    .into(mBackgroundTarget);
        }
    }

}
