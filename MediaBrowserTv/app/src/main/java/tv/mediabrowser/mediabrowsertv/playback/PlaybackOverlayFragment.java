/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package tv.mediabrowser.mediabrowsertv.playback;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.MediaStream;
import tv.mediabrowser.mediabrowsertv.integration.RecommendationManager;
import tv.mediabrowser.mediabrowsertv.itemhandling.BaseRowItem;
import tv.mediabrowser.mediabrowsertv.model.ChapterItemInfo;
import tv.mediabrowser.mediabrowsertv.itemhandling.ItemRowAdapter;
import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.presentation.CardPresenter;
import tv.mediabrowser.mediabrowsertv.util.Utils;

/*
 * Class for video playback with media control
 */
public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment {
    private static final String TAG = "PlaybackControlsFragment";

    private static PlaybackOverlayActivity sContext;

    private static final boolean SHOW_DETAIL = true;
    private static final int PRIMARY_CONTROLS = 6;
    private static final boolean SHOW_LOGO = true;
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private ArrayObjectAdapter mCurrentQueue;
    private PlayPauseAction mPlayPauseAction;
    private RepeatAction mRepeatAction;
    private ThumbsUpAction mThumbsUpAction;
    private ThumbsDownAction mThumbsDownAction;
    private Action mSubtitleAction;
    private Action mAudioAction;
    private FastForwardAction mFastForwardAction;
    private Action mRewindAction;
    private SkipNextAction mSkipNextAction;
    private SkipPreviousAction mSkipPreviousAction;

    private int mInitialStartPos;

    private ImageView mLogo;
    private TextView mClock;
    private Animation fadeIn;
    private Animation fadeOut;

    private Handler mLoopHandler = new Handler();
    private Runnable mClockLoop;

    public PlaybackControlsRow getPlaybackControlsRow() {
        return mPlaybackControlsRow;
    }

    private PlaybackControlsRow mPlaybackControlsRow;
    private TvApp mApplication;
    private PlaybackController mPlaybackController;
    private List<BaseItemDto> mItemsToPlay = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        sContext = (PlaybackOverlayActivity) getActivity();
        mApplication = TvApp.getApplication();

        Intent intent = getActivity().getIntent();
        GsonJsonSerializer serializer = mApplication.getSerializer();

        String[] passedItems = intent.getStringArrayExtra("Items");
        if (passedItems != null) {
            for (String json : passedItems) {
                mItemsToPlay.add((BaseItemDto) serializer.DeserializeFromString(json, BaseItemDto.class));
            }
        }

        mInitialStartPos = intent.getIntExtra("Position", 0);

        mApplication.setPlaybackController(new PlaybackController(mItemsToPlay, this));
        mPlaybackController = mApplication.getPlaybackController();

        setupFade();
        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);

        setupRows();

        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
            }
        });
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemClicked: " + item + " row " + row);
                if (item instanceof BaseRowItem) {
                    BaseRowItem rowItem = (BaseRowItem)item;

                    switch (rowItem.getItemType()) {

                        case BaseItem:
                            break;
                        case Person:
                            break;
                        case Server:
                            break;
                        case User:
                            break;
                        case Chapter:
                            Long start = rowItem.getChapterInfo().getStartPositionTicks() / 10000;
                            mPlaybackController.seek(start.intValue());

                            break;
                        case SearchHint:
                            break;
                    }
                }
            }
        });

        setFadeCompleteListener(new OnFadeCompleteListener() {
            @Override
            public void onFadeInComplete() {
                mLogo.startAnimation(fadeIn);
                mClock.startAnimation(fadeIn);
                super.onFadeInComplete();
            }

            @Override
            public void onFadeOutComplete() {
                mLogo.startAnimation(fadeOut);
                mClock.startAnimation(fadeOut);
                super.onFadeOutComplete();
            }
        });

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLogo = (ImageView) getActivity().findViewById(R.id.npLogoImage);
        String logoImageUrl = Utils.getLogoImageUrl(mPlaybackController.getCurrentlyPlayingItem(), mApplication.getApiClient());
        if (logoImageUrl != null) Picasso.with(getActivity()).load(logoImageUrl).into(mLogo);
        mClock = (TextView) getActivity().findViewById(R.id.pbClock);
        mClock.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf"));

    }

    @Override
    public void onResume() {
        super.onResume();
        startClock();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RecommendationManager.getInstance().recommend(mPlaybackController.getCurrentlyPlayingItem().getId());
        mPlaybackController.stop();
        stopClock();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPlaybackController.stop();
        stopClock();
    }

    private void updateClock() {
        mClock.setText(Utils.getCurrentFormattedTime());
    }

    public void finish() {
        getActivity().finish();
    }

    private void startClock() {
        updateClock();
        mClockLoop = new Runnable() {
            @Override
            public void run() {
                updateClock();
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

    private void setupFade() {
        fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mLogo.setVisibility(View.VISIBLE);
                mClock.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mLogo.setVisibility(View.INVISIBLE);
                mClock.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    private void setupRows() {

        ClassPresenterSelector ps = new ClassPresenterSelector();

        final PlaybackControlsRowPresenter playbackControlsRowPresenter;
        if (SHOW_DETAIL) {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter(
                    new DescriptionPresenter());
        } else {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter();
        }
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == mPlayPauseAction.getId()) {
                    if (mPlayPauseAction.getIndex() == PlayPauseAction.PLAY) {
                        mPlaybackController.play(mPlaybackControlsRow.getCurrentTime());
                    } else {
                        mPlaybackController.pause();
                    }
                } else if (action.getId() == mSkipNextAction.getId()) {
                    mPlaybackController.next();
                } else if (action.getId() == mFastForwardAction.getId()) {
                    mPlaybackController.skip(30000);
                } else if (action.getId() == mRewindAction.getId()) {
                    mPlaybackController.skip(-11000);
                } else if (action.getId() == mThumbsUpAction.getId()) {
                    //toggle likes
                    updateLikes(mPlaybackController.getCurrentlyPlayingItem().getUserData().getLikes() == null ? true : null);
                    mThumbsDownAction.setIndex(mThumbsDownAction.OUTLINE);
                } else if (action.getId() == mThumbsDownAction.getId()) {
                    //toggle dislikes
                    updateLikes(mPlaybackController.getCurrentlyPlayingItem().getUserData().getLikes());
                    mThumbsUpAction.setIndex(mThumbsUpAction.OUTLINE);
                } else if (action.getId() == mSubtitleAction.getId()) {
                    setFadingEnabled(false);
                    List<MediaStream> subtitles = Utils.GetSubtitleStreams(mPlaybackController.getCurrentMediaSource());
                    PopupMenu subMenu = new PopupMenu(getActivity(), getActivity().findViewById(R.id.playback_progress), Gravity.RIGHT);
                    MenuItem none = subMenu.getMenu().add(0, -1, 0, "None");
                    int currentSubIndex = Utils.NullCoalesce(mPlaybackController.getCurrentStreamInfo().getSubtitleStreamIndex(), -1);
                    if (currentSubIndex < 0) none.setChecked(true);
                    for (MediaStream sub : subtitles) {
                        MenuItem item = subMenu.getMenu().add(0, sub.getIndex(), sub.getIndex(), sub.getLanguage() + (sub.getIsExternal() ? " (external)" : " (internal)") + (sub.getIsForced() ? " (forced)" : ""));
                        if (currentSubIndex == sub.getIndex()) item.setChecked(true);
                    }
                    subMenu.getMenu().setGroupCheckable(0, true, false);
                    subMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            setFadingEnabled(true);
                        }
                    });
                    subMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mApplication.getLogger().Debug("Selected subtitle " + item.getTitle());
                            mPlaybackController.switchSubtitleStream(item.getItemId());
                            return true;
                        }
                    });
                    subMenu.show();

                } else if (action.getId() == mAudioAction.getId()) {
                    setFadingEnabled(false);
                    List<MediaStream> audioTracks = Utils.GetAudioStreams(mPlaybackController.getCurrentMediaSource());
                    int currentAudioIndex = Utils.NullCoalesce(mPlaybackController.getCurrentStreamInfo().getAudioStreamIndex(), 0);
                    PopupMenu audioMenu = new PopupMenu(getActivity(), getActivity().findViewById(R.id.playback_progress), Gravity.RIGHT);
                    for (MediaStream audio : audioTracks) {
                        MenuItem item = audioMenu.getMenu().add(0, audio.getIndex(), audio.getIndex(), Utils.SafeToUpper(audio.getLanguage()) + " " + Utils.SafeToUpper(audio.getCodec()) + " (" + audio.getChannelLayout() + ")");
                        if (currentAudioIndex == audio.getIndex()) item.setChecked(true);
                    }
                    audioMenu.getMenu().setGroupCheckable(0, true, false);
                    audioMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            setFadingEnabled(true);
                        }
                    });
                    audioMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mApplication.getLogger().Debug("Selected stream " + item.getTitle());
                            mPlaybackController.switchAudioStream(item.getItemId());
                            return true;
                        }
                    });
                    audioMenu.show();

                }
                if (action instanceof PlaybackControlsRow.MultiAction) {
                    ((PlaybackControlsRow.MultiAction) action).nextIndex();
                    notifyChanged(action);
                }
            }
        });
        playbackControlsRowPresenter.setSecondaryActionsHidden(false);

        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        updatePlaybackControls();
        if (mItemsToPlay.size() > 1) addQueueRow(); // only show queue if more than one item

        setAdapter(mRowsAdapter);
    }

    public void updatePlaybackControls() {
        addPlaybackControlsRow();
        if (mPlaybackController.getCurrentlyPlayingItem().getChapters() != null && mPlaybackController.getCurrentlyPlayingItem().getChapters().size() > 0) addChapterRow();

    }

    private void updateLikes(Boolean likes) {
        if (likes == null) {
            mApplication.getApiClient().ClearUserItemRatingAsync(mPlaybackController.getCurrentlyPlayingItem().getId(), mApplication.getCurrentUser().getId(), new Response<UserItemDataDto>() {
                @Override
                public void onResponse(UserItemDataDto response) {
                    mPlaybackController.getCurrentlyPlayingItem().setUserData(response);
                }

                @Override
                public void onError(Exception exception) {
                    exception.printStackTrace();
                }
            });

        } else {
            mApplication.getApiClient().UpdateUserItemRatingAsync(mPlaybackController.getCurrentlyPlayingItem().getId(), mApplication.getCurrentUser().getId(), likes, new Response<UserItemDataDto>() {
                @Override
                public void onResponse(UserItemDataDto response) {
                    mPlaybackController.getCurrentlyPlayingItem().setUserData(response);
                }

                @Override
                public void onError(Exception exception) {
                    exception.printStackTrace();
                }
            });
        }

    }

    private int getDuration() {
        BaseItemDto movie = mPlaybackController.getCurrentlyPlayingItem();

        if (movie != null) {
            Long mbRuntime = movie.getRunTimeTicks();
            Long andDuration = mbRuntime != null ? mbRuntime / 10000: 0;
            return andDuration.intValue();
        }
        return 0;
    }

    public void addPlaybackControlsRow() {
        if (mPlaybackControlsRow != null) mRowsAdapter.remove(mPlaybackControlsRow);
        if (SHOW_DETAIL) {
            mPlaybackControlsRow = new PlaybackControlsRow(mPlaybackController.getCurrentlyPlayingItem());
        } else {
            mPlaybackControlsRow = new PlaybackControlsRow();
        }
        mRowsAdapter.add(0, mPlaybackControlsRow);

        updatePlaybackRow();

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);

        mPlayPauseAction = new PlayPauseAction(sContext);
        mPlayPauseAction.setIndex(PlayPauseAction.PAUSE);
        mRepeatAction = new RepeatAction(sContext);
        mThumbsUpAction = new ThumbsUpAction(sContext);
        mThumbsDownAction = new ThumbsDownAction(sContext);
        BaseItemDto firstItem = mPlaybackController.getCurrentlyPlayingItem();
        UserItemDataDto userData = firstItem.getUserData();
        Boolean hasSubs = Utils.GetSubtitleStreams(mPlaybackController.getCurrentMediaSource()).size() > 0;
        Boolean hasMultiAudio = Utils.GetAudioStreams(mPlaybackController.getCurrentMediaSource()).size() > 1;

        mThumbsDownAction.setIndex(ThumbsDownAction.OUTLINE);
        mThumbsUpAction.setIndex(ThumbsUpAction.OUTLINE);
        if (userData != null && userData.getLikes() != null) {
            if (userData.getLikes()) {
                mThumbsUpAction.setIndex(ThumbsUpAction.SOLID);
                mThumbsDownAction.setIndex(ThumbsDownAction.OUTLINE);
            } else {
                mThumbsDownAction.setIndex(ThumbsDownAction.SOLID);
                mThumbsUpAction.setIndex(ThumbsUpAction.OUTLINE);
            }
        }

        boolean hlsEnabled = PreferenceManager.getDefaultSharedPreferences(mApplication).getBoolean("pref_enable_hls",true);

        mSubtitleAction = new Action(999, null, null, getActivity().getResources().getDrawable(R.drawable.subt));
        mAudioAction = new Action(998, null, null, getActivity().getResources().getDrawable(R.drawable.audiosel));
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(sContext);
        mRewindAction = new Action(997, null, null, getActivity().getResources().getDrawable(R.drawable.loopback));
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(sContext);

        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsUpAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsUpAction);
        }
        //mPrimaryActionsAdapter.add(mSkipPreviousAction);
        if (hlsEnabled && mPlaybackController.canSeek()) {
            mPrimaryActionsAdapter.add(mRewindAction);
        }
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        if (hlsEnabled && mPlaybackController.canSeek()) {
            mPrimaryActionsAdapter.add(new PlaybackControlsRow.FastForwardAction(sContext));
        }
        if (mItemsToPlay.size() > 1) {
            mPrimaryActionsAdapter.add(mSkipNextAction);
        }

        //mSecondaryActionsAdapter.add(mRepeatAction);

        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsDownAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsDownAction);
        }

        if (hasSubs && hasMultiAudio) {
            mSecondaryActionsAdapter.add(mAudioAction);
            mSecondaryActionsAdapter.add(mSubtitleAction);
        } else {
            if (hasSubs) mPrimaryActionsAdapter.add(mSubtitleAction);
            else if (hasMultiAudio) mPrimaryActionsAdapter.add(mAudioAction);
        }
    }

    private void notifyChanged(Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = mSecondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
    }

    private void updatePlaybackRow() {
        if (SHOW_LOGO) {
            sContext.setLogo(Utils.getLogoImageUrl(mPlaybackController.getCurrentlyPlayingItem(), mApplication.getApiClient()));
        }
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
        mPlaybackControlsRow.setTotalTime(getDuration());
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(0);
    }

    private void addChapterRow() {
        BaseItemDto baseItem = mPlaybackController.getCurrentlyPlayingItem();
        if (baseItem.getChapters() != null && baseItem.getChapters().size() > 0) {
            List<ChapterItemInfo> chapters = new ArrayList<>();
            ImageOptions options = new ImageOptions();
            options.setImageType(ImageType.Chapter);
            int i = 0;
            for (ChapterInfoDto dto : baseItem.getChapters()) {
                ChapterItemInfo chapter = new ChapterItemInfo();
                chapter.setItemId(baseItem.getId());
                chapter.setName(dto.getName());
                chapter.setStartPositionTicks(dto.getStartPositionTicks());
                if (dto.getHasImage()) {
                    options.setTag(dto.getImageTag());
                    options.setImageIndex(i);
                    chapter.setImagePath(TvApp.getApplication().getApiClient().GetImageUrl(baseItem.getId(), options));
                }
                chapters.add(chapter);
                i++;
            }

            ItemRowAdapter chapterAdapter = new ItemRowAdapter(chapters, new CardPresenter(), mRowsAdapter);
            chapterAdapter.Retrieve();
            HeaderItem header = new HeaderItem(0, getString(R.string.chapters), null);
            mRowsAdapter.add(new ListRow(header, chapterAdapter));

        }
    }

    private void addQueueRow() {
        mCurrentQueue = new ArrayObjectAdapter(new CardPresenter());
        int i = 0;
        for (BaseItemDto item : mItemsToPlay) {
            mCurrentQueue.add(new BaseRowItem(i++,item));
        }
        HeaderItem header = new HeaderItem(0, getString(R.string.current_queue), null);
        mRowsAdapter.add(new ListRow(header, mCurrentQueue));

    }

    public void removeQueueItem(int pos) {
        if (mCurrentQueue != null) {
            mCurrentQueue.removeItems(pos, 1);
        }
    }

    @Override
    public void onStop() {
        mPlaybackController.stopProgressAutomation();
        super.onStop();
    }

    static class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            BaseItemDto movie = (BaseItemDto) item;
            viewHolder.getTitle().setText(movie.getName());
            viewHolder.getSubtitle().setText(Utils.getInfoRow(movie));
        }
    }

}
