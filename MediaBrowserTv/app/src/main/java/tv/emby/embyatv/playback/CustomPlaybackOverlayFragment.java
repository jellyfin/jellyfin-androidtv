package tv.emby.embyatv.playback;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.mediainfo.SubtitleTrackEvent;
import mediabrowser.model.mediainfo.SubtitleTrackInfo;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.integration.RecommendationManager;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.RemoteControlReceiver;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 4/28/2015.
 */
public class CustomPlaybackOverlayFragment extends Fragment implements IPlaybackOverlayFragment {

    ImageView mPoster;
    ImageView mStudioImage;
    TextView mTitle;
    TextView mEndTime;
    TextView mCurrentPos;
    TextView mRemainingTime;
    View mTopPanel;
    View mBottomPanel;
    ImageButton mPlayPauseBtn;
    LinearLayout mInfoRow;
    LinearLayout mButtonRow;
    FrameLayout mPopupArea;
    RowsFragment mPopupRowsFragment;
    ArrayObjectAdapter mPopupRowAdapter;
    ListRow mChapterRow;
    ProgressBar mCurrentProgress;

    View mNextUpPanel;
    Button mNextButton;
    Button mCancelButton;
    TextView mNextUpTitle;
    TextView mNextUpSummary;
    TextView mStartsIn;
    LinearLayout mNextUpInfoRow;
    ImageView mNextUpPoster;

    PlaybackController mPlaybackController;
    private List<BaseItemDto> mItemsToPlay = new ArrayList<>();

    Animation fadeOut;
    Animation slideUp;
    Animation slideDown;
    Animation showPopup;
    Animation hidePopup;
    Animation showNextUp;
    Animation hideNextUp;
    Handler mHandler = new Handler();
    Runnable mHideTask;

    TvApp mApplication;
    PlaybackOverlayActivity mActivity;
    private AudioManager mAudioManager;

    int mButtonSize;

    boolean mFadeEnabled = false;
    boolean mIsVisible = true;
    boolean mPopupPanelVisible = false;
    boolean mNextUpPanelVisible = false;

    int mCurrentDuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplication = TvApp.getApplication();
        mActivity = (PlaybackOverlayActivity) getActivity();
        mAudioManager = (AudioManager) mApplication.getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager == null) {
            mApplication.getLogger().Error("Unable to get audio manager");
            Utils.showToast(getActivity(), R.string.msg_cannot_play_time);
            return;
        }

        mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mActivity.setKeyListener(keyListener);

        Intent intent = mActivity.getIntent();
        GsonJsonSerializer serializer = mApplication.getSerializer();

        String[] passedItems = intent.getStringArrayExtra("Items");
        if (passedItems != null) {
            for (String json : passedItems) {
                mItemsToPlay.add((BaseItemDto) serializer.DeserializeFromString(json, BaseItemDto.class));
            }
        }

        if (mItemsToPlay.size() == 0) {
            Utils.showToast(mApplication, mApplication.getString(R.string.msg_no_playable_items));
            mActivity.finish();
            return;
        }

        mButtonSize = Utils.convertDpToPixel(mActivity, 28);

        mApplication.setPlaybackController(new PlaybackController(mItemsToPlay, this));
        mPlaybackController = mApplication.getPlaybackController();

        //set up fade task
        mHideTask = new Runnable() {
            @Override
            public void run() {
                if (mIsVisible) hide();
            }
        };

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.vlc_player_interface, container);

        // Inject the RowsFragment in the popup container
        if (getChildFragmentManager().findFragmentById(R.id.rows_area) == null) {
            mPopupRowsFragment = new RowsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.rows_area, mPopupRowsFragment).commit();
        } else {
            mPopupRowsFragment = (RowsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.rows_area);
        }

        mPopupRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mPopupRowsFragment.setAdapter(mPopupRowAdapter);
        mPopupRowsFragment.setOnItemViewClickedListener(itemViewClickedListener);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //if we're not visible, show us
                if (!mIsVisible) show();

                //and then manage our fade timer
                if (mFadeEnabled) startFadeTimer();

                TvApp.getApplication().getLogger().Debug("Got touch event.");
                return false;
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mItemsToPlay.size() == 0) return;

        mPoster = (ImageView) mActivity.findViewById(R.id.poster);
        mNextUpPoster = (ImageView) mActivity.findViewById(R.id.nextUpPoster);
        mStudioImage = (ImageView) mActivity.findViewById(R.id.studioImg);
        mTopPanel = mActivity.findViewById(R.id.topPanel);
        mBottomPanel = mActivity.findViewById(R.id.bottomPanel);
        mNextUpPanel = mActivity.findViewById(R.id.nextUpPanel);
        mPlayPauseBtn = (ImageButton) mActivity.findViewById(R.id.playPauseBtn);
        mPlayPauseBtn.setSecondaryImage(R.drawable.lb_ic_pause);
        mPlayPauseBtn.setPrimaryImage(R.drawable.play);
        mPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaybackController.playPause();
            }
        });
        mInfoRow = (LinearLayout) mActivity.findViewById(R.id.infoRow);
        mNextUpInfoRow = (LinearLayout) mActivity.findViewById(R.id.nextUpInfoRow);
        mButtonRow = (LinearLayout) mActivity.findViewById(R.id.buttonRow);
        mTitle = (TextView) mActivity.findViewById(R.id.title);
        mNextUpTitle = (TextView) mActivity.findViewById(R.id.nextUpTitle);
        mNextUpSummary = (TextView) mActivity.findViewById(R.id.nextUpSummary);
        Typeface font = Typeface.createFromAsset(mActivity.getAssets(), "fonts/Roboto-Light.ttf");
        mTitle.setTypeface(font);
        mNextUpTitle.setTypeface(font);
        mNextUpSummary.setTypeface(font);
        mEndTime = (TextView) mActivity.findViewById(R.id.endTime);
        mCurrentPos = (TextView) mActivity.findViewById(R.id.currentPos);
        mRemainingTime = (TextView) mActivity.findViewById(R.id.remainingTime);
        mCurrentProgress = (ProgressBar) mActivity.findViewById(R.id.playerProgress);
        mPopupArea = (FrameLayout) mActivity.findViewById(R.id.popupArea);
        mStartsIn = (TextView) mActivity.findViewById(R.id.startsIn);
        mNextButton = (Button) mActivity.findViewById(R.id.nextButton);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaybackController.next();
            }
        });
        mCancelButton = (Button) mActivity.findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //pre-load animations
        fadeOut = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        fadeOut.setAnimationListener(hideAnimationListener);
        slideDown = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_top);
        slideUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        slideUp.setAnimationListener(showAnimationListener);
        setupPopupAnimations();
        setupNextUpAnimations();

        Intent intent = mActivity.getIntent();
        //start playing
        int startPos = intent.getIntExtra("Position", 0);
        mPlaybackController.play(startPos);

        mPlayPauseBtn.requestFocus();

    }

    private void setupPopupAnimations() {
        showPopup = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        showPopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mPopupArea.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPopupArea.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        hidePopup = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        hidePopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPopupArea.setVisibility(View.GONE);
                mButtonRow.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


    }

    private void setupNextUpAnimations() {
        showNextUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        showNextUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mNextUpPanel.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mNextButton.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        hideNextUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        hideNextUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mNextUpPanel.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChanged = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    mPlaybackController.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    //We don't do anything here on purpose
                    // On the Nexus we get this notification erroneously when first starting up
                    // and in any instance that we navigate away from our page, we already handle
                    // stopping video and handing back audio focus
                    break;
            }
        }
    };

    private OnItemViewClickedListener itemViewClickedListener = new OnItemViewClickedListener() {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof BaseRowItem) {
                BaseRowItem rowItem = (BaseRowItem)item;

                switch (rowItem.getItemType()) {

                    case Chapter:
                        Long start = rowItem.getChapterInfo().getStartPositionTicks() / 10000;
                        mPlaybackController.seek(start);
                        hidePopupPanel();
                        break;
                }
            }
        }
    };

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            boolean ret = false;
            if (mPopupPanelVisible && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B)) {
                //back should just hide the popup panel
                hidePopupPanel();
                return true;
            }
            if (mNextUpPanelVisible) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                    //back should just hide the popup panel
                    hideNextUpPanel();
                    return true;
                }
                return false;
            }
            if (mIsVisible && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B)) {
                //back should just hide the panel
                hide();
                return true;
            }

            if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_BUTTON_B) {
                if (mPopupPanelVisible) {
                    // up or down should close panel
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        hidePopupPanel();
                        return true;
                    } else {
                        return false;
                    }
                }

                //if we're not visible, show us
                if (!mIsVisible) show();

                //and then manage our fade timer
                if (mFadeEnabled) startFadeTimer();

            }

            return ret;
        }
    };

    private void startFadeTimer() {
        mHandler.removeCallbacks(mHideTask);
        mHandler.postDelayed(mHideTask, 6000);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAudioManager.requestAudioFocus(mAudioFocusChanged, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mApplication.getLogger().Error("Unable to get audio focus");
            Utils.showToast(getActivity(), R.string.msg_cannot_play_time);
            return;
        }

        //Register a media button receiver so that all media button presses will come to us and not another app
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getActivity().getPackageName(), RemoteControlReceiver.class.getName()));
        //TODO implement conditional logic for api 21+

        if (!mIsVisible) show(); // in case we were paused during video playback

    }

    @Override
    public void onDestroy() {
        if (mPlaybackController != null && mPlaybackController.getCurrentlyPlayingItem() != null) RecommendationManager.getInstance().recommend(mPlaybackController.getCurrentlyPlayingItem().getId());
        super.onDestroy();
    }

    @Override
    public void onPause() {
        mPlaybackController.stop();
        setPlayPauseActionState(ImageButton.STATE_PRIMARY); // in case we come back

        //UnRegister the media button receiver
        mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(getActivity().getPackageName(), RemoteControlReceiver.class.getName()));
        //TODO implement conditional logic for api 21+

        //Give back audio focus
        mAudioManager.abandonAudioFocus(mAudioFocusChanged);

        super.onPause();
    }

    public void show() {
        mBottomPanel.startAnimation(slideUp);
        mTopPanel.startAnimation(slideDown);
        mIsVisible = true;
        mPlayPauseBtn.requestFocus();
    }

    public void hide() {
        mIsVisible = false;
        mBottomPanel.startAnimation(fadeOut);
        mTopPanel.startAnimation(fadeOut);
    }

    private void showPopupPanel(ListRow row) {
        setFadingEnabled(false);

        mPopupRowAdapter.clear();
        mPopupRowAdapter.add(row);
        mPopupArea.startAnimation(showPopup);
        mPopupPanelVisible = true;
    }

    private void hidePopupPanel(){
        setFadingEnabled(true);
        mPopupArea.startAnimation(hidePopup);
        mPopupPanelVisible = false;
    }

    private void showNextUpPanel() {
        if (mIsVisible) hide();
        if (mPopupPanelVisible) hidePopupPanel();

        mNextUpPanel.startAnimation(showNextUp);
        mNextUpPanelVisible = true;
    }

    private void hideNextUpPanel(){
        mNextUpPanel.startAnimation(hideNextUp);
        mNextUpPanelVisible = false;
    }

    private Animation.AnimationListener hideAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mTopPanel.setVisibility(View.GONE);
            mBottomPanel.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private Animation.AnimationListener showAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mTopPanel.setVisibility(View.VISIBLE);
            mBottomPanel.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private void updatePoster(BaseItemDto item, ImageView target, boolean preferSeries) {
        int height = Utils.convertDpToPixel(getActivity(), 300);
        int width = Utils.convertDpToPixel(getActivity(), 150);
        String posterImageUrl = Utils.getPrimaryImageUrl(item, mApplication.getApiClient(), false, false, false, preferSeries, height);
        if (posterImageUrl != null) Picasso.with(getActivity()).load(posterImageUrl).skipMemoryCache().resize(width, height).centerInside().into(target);

    }

    private void updateStudio(BaseItemDto item) {
        int height = Utils.convertDpToPixel(mActivity, 45);
        int width = Utils.convertDpToPixel(mActivity, 70);
        if (item.getStudios() != null && item.getStudios().length > 0 && item.getStudios()[0].getHasPrimaryImage()) {
            String studioImageUrl = Utils.getPrimaryImageUrl(item.getStudios()[0], mApplication.getApiClient(), height);
            if (studioImageUrl != null) Picasso.with(mActivity).load(studioImageUrl).resize(width, height).centerInside().into(mStudioImage);
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
                if (studioImageUrl != null) Picasso.with(mActivity).load(studioImageUrl).resize(width, height).centerInside().into(mStudioImage);

            } else {
                mStudioImage.setImageResource(R.drawable.blank30x30);

            }
        }

    }

    public void updateEndTime(final long timeLeft) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEndTime.setText(timeLeft > 0 ?
                                mApplication.getString(R.string.lbl_ends) + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(System.currentTimeMillis() + timeLeft)
                                : ""
                );
            }
        });

    }

    private void updateCurrentDuration(BaseItemDto item) {
        Long mbRuntime = item.getRunTimeTicks();
        Long andDuration = mbRuntime != null ? mbRuntime / 10000: 0;
        mCurrentDuration = andDuration.intValue();
    }

    private void addButtons(BaseItemDto item) {
        mButtonRow.removeAllViews();

        if (!Utils.isFireTv()) {
            // on-screen jump buttons for Nexus
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.repeat, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlaybackController.skip(-11000);
                }
            }));

            mButtonRow.addView(new ImageButton(mActivity, R.drawable.fastforward, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlaybackController.skip(30000);
                }
            }));

        }

        Boolean hasSubs = Utils.GetSubtitleStreams(mPlaybackController.getCurrentMediaSource()).size() > 0;
        Boolean hasMultiAudio = Utils.GetAudioStreams(mPlaybackController.getCurrentMediaSource()).size() > 1;

        if (hasMultiAudio) {
            mApplication.getLogger().Debug("Multiple Audio tracks found: "+Utils.GetAudioStreams(mPlaybackController.getCurrentMediaSource()).size());
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.audiosel, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlaybackController.getCurrentStreamInfo() == null) {
                        TvApp.getApplication().getLogger().Warn("StreamInfo null trying to obtain audio tracks");
                        Utils.showToast(TvApp.getApplication(), "Unable to obtain audio track info");
                        return;
                    }
                    setFadingEnabled(false);

                    List<MediaStream> audioTracks = TvApp.getApplication().getPlaybackManager().getInPlaybackSelectableAudioStreams(mPlaybackController.getCurrentStreamInfo());
                    Integer currentAudioIndex = mPlaybackController.getAudioStreamIndex();

                    PopupMenu audioMenu = Utils.createPopupMenu(getActivity(), v, Gravity.RIGHT);
                    for (MediaStream audio : audioTracks) {
                        MenuItem item = audioMenu.getMenu().add(0, audio.getIndex(), audio.getIndex(), Utils.SafeToUpper(audio.getLanguage()) + " " + Utils.SafeToUpper(audio.getCodec()) + " (" + audio.getChannelLayout() + ")");
                        if (currentAudioIndex != null && currentAudioIndex == audio.getIndex()) item.setChecked(true);
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
            }));
        } else {
            mApplication.getLogger().Debug("Only one audio track.");
        }

        if (hasSubs) {
            mApplication.getLogger().Debug("Subtitle tracks found: "+Utils.GetSubtitleStreams(mPlaybackController.getCurrentMediaSource()).size());
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.subt, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlaybackController.getCurrentStreamInfo() == null) {
                        TvApp.getApplication().getLogger().Warn("StreamInfo null trying to obtain subtitles");
                        Utils.showToast(TvApp.getApplication(), "Unable to obtain subtitle info");
                        return;
                    }
                    setFadingEnabled(false);
                    List<MediaStream> subtitles = TvApp.getApplication().getPlaybackManager().getInPlaybackSelectableSubtitleStreams(mPlaybackController.getCurrentStreamInfo());
                    PopupMenu subMenu = Utils.createPopupMenu(getActivity(), v, Gravity.RIGHT);
                    MenuItem none = subMenu.getMenu().add(0, -1, 0, mApplication.getString(R.string.lbl_none));
                    int currentSubIndex = mPlaybackController.getSubtitleStreamIndex();
                    if (currentSubIndex < 0) none.setChecked(true);
                    for (MediaStream sub : subtitles) {
                        MenuItem item = subMenu.getMenu().add(0, sub.getIndex(), sub.getIndex(), sub.getLanguage() + (sub.getIsExternal() ? mApplication.getString(R.string.lbl_parens_external) : mApplication.getString(R.string.lbl_parens_internal)) + (sub.getIsForced() ? mApplication.getString(R.string.lbl_parens_forced) : ""));
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

                }
            }));
        } else {
            mApplication.getLogger().Debug("No sub tracks found.");
        }

        List<ChapterInfoDto> chapters = item.getChapters();
        if (chapters != null && chapters.size() > 0) {
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.chapter, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopupPanel(mChapterRow);
                }
            }));

            //Create chapter row for later use
            ItemRowAdapter chapterAdapter = new ItemRowAdapter(Utils.buildChapterItems(item), new CardPresenter(), new ArrayObjectAdapter());
            chapterAdapter.Retrieve();
            mChapterRow = new ListRow(new HeaderItem(mActivity.getString(R.string.chapters), null), chapterAdapter);

        }

        if (mPlaybackController.hasNextItem()) {
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.lb_ic_skip_next, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlaybackController.next();
                }
            }));
        }

    }

    @Override
    public void setCurrentTime(long time) {
        if (mNextUpPanelVisible) {
            mStartsIn.setText(mCurrentDuration > 0 ? "Starts in " + Utils.formatMillis(mCurrentDuration - time) : "");
        } else {
            mCurrentProgress.setProgress(((Long)time).intValue());
            mCurrentPos.setText(Utils.formatMillis(time));
            mRemainingTime.setText(mCurrentDuration > 0 ? "-" + Utils.formatMillis(mCurrentDuration - time) : "");
        }
    }

    @Override
    public void setFadingEnabled(boolean value) {
        mFadeEnabled = value;
        if (mFadeEnabled) {
            if (mIsVisible) startFadeTimer();
        } else {
            mHandler.removeCallbacks(mHideTask);
            if (!mIsVisible) getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
        }
    }

    @Override
    public void setPlayPauseActionState(final int state) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlayPauseBtn.setState(state);
            }
        });
    }

    @Override
    public void updateDisplay() {
        BaseItemDto current = mPlaybackController.getCurrentlyPlayingItem();
        if (current != null) {
            if (mNextUpPanelVisible) hideNextUpPanel();
            updateCurrentDuration(current);
            //set progress to match duration
            mCurrentProgress.setMax(mCurrentDuration);
            //set other information
            mTitle.setText(current.getName());
            updatePoster(current, mPoster, true);
            updateStudio(current);
            addButtons(current);
            InfoLayoutHelper.addInfoRow(mActivity, current, mInfoRow, true);

            if (mPlaybackController.hasNextItem()) {
                //setup next up panel now - we need to retrieve full item for all info
                mApplication.getApiClient().GetItemAsync(mPlaybackController.getNextItem().getId(), mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        mNextUpTitle.setText("Up Next...  " + response.getName());
                        mNextUpSummary.setText(response.getOverview());
                        InfoLayoutHelper.addInfoRow(mActivity, response, mNextUpInfoRow, true);
                        updatePoster(response, mNextUpPoster, true);
                    }
                });
            }

            if (mIsVisible) mPlayPauseBtn.requestFocus();
        }
    }

    @Override
    public void removeQueueItem(int pos) {

    }

    @Override
    public void nextItemThresholdHit(BaseItemDto nextItem) {
        mApplication.getLogger().Debug("Next Item is " + nextItem.getName());
        showNextUpPanel();
    }

    @Override
    public void finish() {
        getActivity().finish();
    }

}
