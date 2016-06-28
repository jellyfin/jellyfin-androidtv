package tv.emby.embyatv.details;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.InfoCardPresenter;
import tv.emby.embyatv.presentation.MyDetailsOverviewRowPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.SpecialsQuery;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.querying.TrailersQuery;
import tv.emby.embyatv.ui.GenreButton;
import tv.emby.embyatv.ui.IRecordingIndicatorView;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.ui.RecordPopup;
import tv.emby.embyatv.util.DelayedMessage;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 2/19/2015.
 */
public class FullDetailsActivity extends BaseActivity implements IRecordingIndicatorView {

    private int BUTTON_SIZE;

    private LinearLayout mGenreRow;
    private ImageButton mResumeButton;
    private ImageButton mPrevButton;
    private ImageButton mRecordButton;
    private ImageButton mRecSeriesButton;
    private ImageButton mWatchedToggleButton;

    private Target mBackgroundTarget;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    protected BaseItemDto mProgramInfo;
    protected String mItemId;
    protected String mChannelId;
    protected BaseRowItem mCurrentItem;
    private Calendar mLastUpdated;
    private String mPrevItemId;

    private TextView mTitle;
    private RowsFragment mRowsFragment;
    private ArrayObjectAdapter mRowsAdapter;

    private MyDetailsOverviewRowPresenter mDorPresenter;
    private MyDetailsOverviewRow mDetailsOverviewRow;

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

        BUTTON_SIZE = Utils.convertDpToPixel(this, 35);
        mApplication = TvApp.getApplication();
        mActivity = this;
        roboto = mApplication.getDefaultFont();

        mTitle = (TextView) findViewById(R.id.fdTitle);
        mTitle.setTypeface(roboto);
        mTitle.setShadowLayer(5, 5, 5, Color.BLACK);
        mGenreRow = (LinearLayout) findViewById(R.id.fdGenreRow);
        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        backgroundManager.attach(getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);
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
                                    }
                                    updatePlayedDate();
                                    updateWatched();
                                    updatePoster();
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

    private static String[] playableTypes = new String[] {"Episode","Movie","Series","Season","Folder","Video","Recording","Program","ChannelVideoItem","Trailer","MusicArtist","Person","MusicVideo"};
    private static List<String> playableTypeList = Arrays.asList(playableTypes);
    private static String[] directPlayableTypes = new String[] {"Episode","Movie","Video","Recording","Program"};
    private static List<String> directPlayableTypeList = Arrays.asList(directPlayableTypes);

    private void updatePoster() {
        if (isFinishing()) return;
        Picasso.with(mActivity)
                .load(Utils.getPrimaryImageUrl(mBaseItem, TvApp.getApplication().getApiClient(),false, false, posterHeight))
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
        final FullDetailsActivity us = this;
        if (mChannelId != null && mProgramInfo == null) {
            // if we are displaying a live tv channel - we want to get whatever is showing now on that channel
            mApplication.getApiClient().GetLiveTvChannelAsync(mChannelId, TvApp.getApplication().getCurrentUser().getId(), new Response<ChannelInfoDto>() {
                @Override
                public void onResponse(ChannelInfoDto response) {
                    mProgramInfo = response.getCurrentProgram();
                    mItemId = mProgramInfo.getId();
                    mApplication.getApiClient().GetItemAsync(mItemId, mApplication.getCurrentUser().getId(), new DetailItemLoadResponse(us));

                }
            });
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
        mProgramInfo.setSeriesTimerId(id);
        if (mRecSeriesButton != null) mRecSeriesButton.setImageResource(id == null ? R.drawable.recserieswhite : R.drawable.recseries);
    }

    private class BuildDorTask extends AsyncTask<BaseItemDto, Integer, MyDetailsOverviewRow> {

        @Override
        protected MyDetailsOverviewRow doInBackground(BaseItemDto... params) {
            BaseItemDto item = params[0];

            // Figure image size
            Double aspect = Utils.getImageAspectRatio(item, false);
            posterHeight = aspect > 1 ? Utils.convertDpToPixel(mActivity, 170) : Utils.convertDpToPixel(mActivity, 300);
            posterWidth = (int)((aspect) * posterHeight);
            if (posterHeight < 10) posterWidth = Utils.convertDpToPixel(mActivity, 150);  //Guard against zero size images causing picasso to barf

            String primaryImageUrl = Utils.getPrimaryImageUrl(mBaseItem, TvApp.getApplication().getApiClient(),false, false, posterHeight);
            mDetailsOverviewRow = new MyDetailsOverviewRow(item);

            mDetailsOverviewRow.setSummary(item.getOverview());
            switch (item.getType()) {
                case "Person":
                case "MusicArtist":
                    mDetailsOverviewRow.setSummarySubTitle("");
                    break;
                default:

                    BaseItemPerson director = Utils.GetFirstPerson(item, PersonType.Director);
                    if (director != null) {
                        mDetailsOverviewRow.setSummaryTitle(getString(R.string.lbl_directed_by)+director.getName());
                    }
                    mDetailsOverviewRow.setSummarySubTitle(getEndTime());
            }
            try {
                //Main image
                Bitmap poster = Picasso.with(mActivity)
                        .load(primaryImageUrl)
                        .skipMemoryCache()
                        .resize(posterWidth, posterHeight)
                        .centerInside()
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
            ps.addClassPresenter(ListRow.class, new ListRowPresenter());
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
                    ItemRowAdapter castAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(), adapter);
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
                    ItemRowAdapter chapterAdapter = new ItemRowAdapter(chapters, new CardPresenter(), adapter);
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
                    ItemRowAdapter castAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(), adapter);
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
                    ItemRowAdapter seriesCastAdapter = new ItemRowAdapter(mBaseItem.getPeople(), new CardPresenter(), adapter);
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
                    ItemRowAdapter nextAdapter = new ItemRowAdapter(nextEpisodes, 0 , false, true, new CardPresenter(), adapter);
                    addItemRow(adapter, nextAdapter, 5, "Next Episodes");
                }
                addInfoRows(adapter);
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
        setTitle(item.getName());
        if (item.getName().length() > 32) {
            // scale down the title so more will fit
            mTitle.setTextSize(32);
        }

        LinearLayout mainInfoRow = (LinearLayout)findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(this, item, mainInfoRow, false, false);
        addGenres(mGenreRow);
        if (playableTypeList.contains(item.getType())) addButtons(BUTTON_SIZE);
//        updatePlayedDate();
//
        updateBackground(Utils.getBackdropImageUrl(item, TvApp.getApplication().getApiClient(), true));

        mLastUpdated = Calendar.getInstance();

    }

    public void setTitle(String title) {
        mTitle.setText(title);
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

    private String getEndTime() {
        if (mBaseItem != null) {
            Long runtime = Utils.NullCoalesce(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
            if (runtime != null && runtime > 0) {
                long endTimeTicks = "Program".equals(mBaseItem.getType()) && mBaseItem.getEndDate() != null ? Utils.convertToLocalDate(mBaseItem.getEndDate()).getTime() : System.currentTimeMillis() + runtime / 10000;
                String text = getString(R.string.lbl_runs) + runtime / 600000000 + getString(R.string.lbl_min) + "  " + getString(R.string.lbl_ends) + android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks));
                if (mBaseItem.getCanResume()) {
                    endTimeTicks = System.currentTimeMillis() + ((runtime - mBaseItem.getUserData().getPlaybackPositionTicks()) / 10000);
                    text += " ("+android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks))+getString(R.string.lbl_if_resumed);
                }

                return text;
            }

        }
        return "";
    }

    private void addGenres(LinearLayout layout) {
        if (mBaseItem.getGenres() != null && mBaseItem.getGenres().size() > 0) {
            boolean first = true;
            for (String genre : mBaseItem.getGenres()) {
                if (!first) InfoLayoutHelper.addSpacer(this, layout, "  /  ", 14);
                first = false;
                layout.addView(new GenreButton(this, roboto, 16, genre, mBaseItem.getType()));
            }
        }
    }

    private void addButtons(int buttonSize) {
        mResumeButton = new ImageButton(this, R.drawable.resume, buttonSize, "Series".equals(mBaseItem.getType()) ? getString(R.string.lbl_play_next_up) : getString(R.string.lbl_resume), null, new View.OnClickListener() {
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
            mResumeButton.setVisibility(("Series".equals(mBaseItem.getType()) && ! mBaseItem.getUserData().getPlayed()) || mBaseItem.getCanResume() ? View.VISIBLE : View.GONE);

            ImageButton play = new ImageButton(this, R.drawable.play, buttonSize, getString(Utils.isLiveTv(mBaseItem) ? R.string.lbl_tune_to_channel : mBaseItem.getIsFolder() ? R.string.lbl_play_all : R.string.lbl_play), null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    play(mBaseItem, 0, false);
                }
            });
            mDetailsOverviewRow.addAction(play);

            if (!mBaseItem.getIsFolder() && !Utils.isLiveTv(mBaseItem)) {
                ImageButton queue = new ImageButton(this, R.drawable.addtoqueue, buttonSize, getString(R.string.lbl_add_to_queue), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ("Audio".equals(mBaseItem.getType())) {
                            MediaManager.addToAudioQueue(Arrays.asList(mBaseItem));

                        } else {
                            MediaManager.addToVideoQueue(mBaseItem);
                        }
                    }
                });
                mDetailsOverviewRow.addAction(queue);
            }

            if (mBaseItem.getIsFolder()) {
                ImageButton shuffle = new ImageButton(this, R.drawable.shuffle, buttonSize, getString(R.string.lbl_shuffle_all), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        play(mBaseItem, 0, true);
                    }
                });
                mDetailsOverviewRow.addAction(shuffle);
            }

            if ("MusicArtist".equals(mBaseItem.getType())) {
                ImageButton imix = new ImageButton(this, R.drawable.mix, buttonSize, getString(R.string.lbl_instant_mix), null, new View.OnClickListener() {
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
            ImageButton trailer = new ImageButton(this, R.drawable.trailer, buttonSize, getString(R.string.lbl_play_trailers), null, new View.OnClickListener() {
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

        if (mProgramInfo != null) {
            if (Utils.convertToLocalDate(mBaseItem.getEndDate()).getTime() > System.currentTimeMillis()) {
                //Record button
                mRecordButton = new ImageButton(this, mProgramInfo.getTimerId() != null ? R.drawable.rec : R.drawable.recwhite, buttonSize, getString(R.string.lbl_record), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getTimerId() == null) {
                            showRecordingOptions(false);
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
                mRecSeriesButton= new ImageButton(this, mProgramInfo.getSeriesTimerId() != null ? R.drawable.recseries : R.drawable.recserieswhite, buttonSize, getString(R.string.lbl_record_series), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mProgramInfo.getSeriesTimerId() == null) {
                            showRecordingOptions(true);
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
            }
        }

        UserItemDataDto userData = mBaseItem.getUserData();
        if (userData != null) {
            if (!"MusicArtist".equals(mBaseItem.getType()) && !"Person".equals(mBaseItem.getType())) {
                mWatchedToggleButton = new ImageButton(this, userData.getPlayed() ? R.drawable.redcheck : R.drawable.whitecheck, buttonSize, getString(R.string.lbl_toggle_watched), null, markWatchedListener);
                mDetailsOverviewRow.addAction(mWatchedToggleButton);
            }

            //Favorite
            ImageButton fav = new ImageButton(this, userData.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart, buttonSize, getString(R.string.lbl_toggle_favorite), null, new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    UserItemDataDto data = mBaseItem.getUserData();
                        mApplication.getApiClient().UpdateFavoriteStatusAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
                            @Override
                            public void onResponse(UserItemDataDto response) {
                                mBaseItem.setUserData(response);
                                ((ImageButton)v).setImageResource(response.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart);
                                TvApp.getApplication().setLastFavoriteUpdate(System.currentTimeMillis());
                            }
                        });
                }
            });
            mDetailsOverviewRow.addAction(fav);
        }

        if ("Episode".equals(mBaseItem.getType()) && mBaseItem.getSeriesId() != null) {
            //add the prev button first so it will be there in proper position - we'll show it later if needed
            mPrevButton = new ImageButton(this, R.drawable.prev, buttonSize, "Previous Episode", null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPrevItemId != null) {
                        Intent intent = new Intent(mActivity, FullDetailsActivity.class);
                        intent.putExtra("ItemId", mPrevItemId);
                        mActivity.startActivity(intent);
                    }
                }
            });

            mPrevButton.setVisibility(View.GONE);
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
                        }
                    }
                }
            });


            ImageButton series = new ImageButton(this, R.drawable.tvicon, buttonSize, getString(R.string.lbl_goto_series), null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mActivity, FullDetailsActivity.class);
                    intent.putExtra("ItemId", mBaseItem.getSeriesId());
                    mActivity.startActivity(intent);
                }
            });
            mDetailsOverviewRow.addAction(series);
        }

        if (("Recording".equals(mBaseItem.getType()) && TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvManagement() && mBaseItem.getCanDelete()) ||
                (("Movie".equals(mBaseItem.getType()) || "Episode".equals(mBaseItem.getType()) || "Video".equals(mBaseItem.getType())) && TvApp.getApplication().getCurrentUser().getPolicy().getEnableContentDeletion())) {
            final Activity activity = this;
            ImageButton del = new ImageButton(this, R.drawable.trash, buttonSize, getString(R.string.lbl_delete), null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.lbl_delete)
                            .setMessage("This will PERMANENTLY DELETE " + mBaseItem.getName() + " from your library.  Are you VERY sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    final DelayedMessage msg = new DelayedMessage(activity, 150);
                                    TvApp.getApplication().getApiClient().DeleteItem(mBaseItem.getId(), new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            msg.Cancel();
                                            Utils.showToast(activity, mBaseItem.getName() + " Deleted");
                                            TvApp.getApplication().setLastDeletedItemId(mBaseItem.getId());
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
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utils.showToast(activity, "Item NOT Deleted");
                                }
                            })
                            .show();

                }
            });
            mDetailsOverviewRow.addAction(del);
        }
    }

    RecordPopup mRecordPopup;
    public void showRecordingOptions(final boolean recordSeries) {
        if (mRecordPopup == null) {
            int width = Utils.convertDpToPixel(this, 600);
            Point size = new Point();
            getWindowManager().getDefaultDisplay().getSize(size);
            mRecordPopup = new RecordPopup(this, mTitle, (size.x/2) - (width/2), mTitle.getTop(), width);
        }
        TvApp.getApplication().getApiClient().GetDefaultLiveTvTimerInfo(mProgramInfo.getId(), new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                if (recordSeries || Utils.isTrue(mProgramInfo.getIsSports())){
                    mRecordPopup.setContent(mProgramInfo, response, mActivity, recordSeries);
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
                            if (data.getPlayed())  markUnPlayed(v); else markPlayed(v);
                    }
                }).show();

            } else {
                if (data.getPlayed()) {
                    markUnPlayed(v);
                } else {
                    markPlayed(v);
                }
            }

        }
    };

    private void markPlayed(final View v) {
        mApplication.getApiClient().MarkPlayedAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), null, new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                ((ImageButton) v).setImageResource(R.drawable.redcheck);
                //adjust resume
                if (mResumeButton != null && !mBaseItem.getCanResume())
                    mResumeButton.setVisibility(View.GONE);
                //force lists to re-fetch
                TvApp.getApplication().setLastPlayback(Calendar.getInstance());
            }
        });

    }

    private void markUnPlayed(final View v) {
        mApplication.getApiClient().MarkUnplayedAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), new Response<UserItemDataDto>() {
            @Override
            public void onResponse(UserItemDataDto response) {
                mBaseItem.setUserData(response);
                ((ImageButton) v).setImageResource(R.drawable.whitecheck);
                //adjust resume
                if (mResumeButton != null && !mBaseItem.getCanResume())
                    mResumeButton.setVisibility(View.GONE);
                //force lists to re-fetch
                TvApp.getApplication().setLastPlayback(Calendar.getInstance());
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
                    Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                    MediaManager.setCurrentVideoQueue(response);
                    intent.putExtra("Position", pos);
                    startActivity(intent);
                }
            }
        });

    }

    protected void play(final BaseItemDto[] items, final int pos, final boolean shuffle) {
        List<BaseItemDto> itemsToPlay = Arrays.asList(items);
        Intent intent = new Intent(this, PlaybackOverlayActivity.class);
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
