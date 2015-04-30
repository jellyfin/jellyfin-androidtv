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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.integration.RecommendationManager;
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
    TextClock mClock;
    View mTopPanel;
    View mBottomPanel;
    ImageButton mPlayPauseBtn;
    LinearLayout mInfoRow;
    ProgressBar mCurrentProgress;
    PlaybackController mPlaybackController;
    private List<BaseItemDto> mItemsToPlay = new ArrayList<>();

    Animation fadeOut;
    Animation slideUp;
    Animation slideDown;
    Handler mHandler = new Handler();
    Runnable mHideTask;

    TvApp mApplication;
    PlaybackOverlayActivity mActivity;
    private AudioManager mAudioManager;

    boolean mFadeEnabled = false;
    boolean mIsVisible = true;

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
            Utils.showToast(mActivity, mApplication.getString(R.string.msg_no_playable_items));
            return;
        }

        mApplication.setPlaybackController(new PlaybackController(mItemsToPlay, this));
        mPlaybackController = mApplication.getPlaybackController();

        //set up fade task
        mHideTask = new Runnable() {
            @Override
            public void run() {
                hide();
            }
        };


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.custom_player_interface, container);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPoster = (ImageView) mActivity.findViewById(R.id.poster);
        mStudioImage = (ImageView) mActivity.findViewById(R.id.studioImg);
        mTopPanel = mActivity.findViewById(R.id.topPanel);
        mBottomPanel = mActivity.findViewById(R.id.bottomPanel);
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
        mTitle = (TextView) mActivity.findViewById(R.id.title);
        Typeface font = Typeface.createFromAsset(mActivity.getAssets(), "fonts/Roboto-Light.ttf");
        mTitle.setTypeface(font);
        mEndTime = (TextView) mActivity.findViewById(R.id.endTime);
        mCurrentPos = (TextView) mActivity.findViewById(R.id.currentPos);
        mRemainingTime = (TextView) mActivity.findViewById(R.id.remainingTime);
        mClock = (TextClock) mActivity.findViewById(R.id.textClock);
        mClock.setTypeface(font);
        mCurrentProgress = (ProgressBar) mActivity.findViewById(R.id.playerProgress);

        //pre-load animations
        fadeOut = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        fadeOut.setAnimationListener(hideAnimationListener);
        slideDown = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_top);
        slideUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        slideUp.setAnimationListener(showAnimationListener);

        updateDisplay();

        Intent intent = mActivity.getIntent();
        //start playing
        int startPos = intent.getIntExtra("Position", 0);
        mPlaybackController.play(startPos);

        mPlayPauseBtn.requestFocus();

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

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_BUTTON_B) {
                //if we're not visible, show us
                if (!mIsVisible) show();

                //and then manage our fade timer
                if (mFadeEnabled) startFadeTimer();
            }

            return false;
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

    }

    @Override
    public void onDestroy() {
        RecommendationManager.getInstance().recommend(mPlaybackController.getCurrentlyPlayingItem().getId());
        super.onDestroy();
    }

    @Override
    public void onPause() {
        mPlaybackController.stop();
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
        mBottomPanel.startAnimation(fadeOut);
        mTopPanel.startAnimation(fadeOut);
        mIsVisible = false;
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

    private void updatePoster(BaseItemDto item) {
        int height = Utils.convertDpToPixel(getActivity(), 300);
        int width = Utils.convertDpToPixel(getActivity(), 150);
        String posterImageUrl = Utils.getPrimaryImageUrl(item, mApplication.getApiClient(), false, false, false, true, height);
        if (posterImageUrl != null) Picasso.with(getActivity()).load(posterImageUrl).skipMemoryCache().resize(width, height).centerInside().into(mPoster);

    }

    private void updateStudio(BaseItemDto item) {
        if (item.getStudios() != null && item.getStudios().length > 0 && item.getStudios()[0].getHasPrimaryImage()) {
            int height = Utils.convertDpToPixel(mActivity, 45);
            int width = Utils.convertDpToPixel(mActivity, 70);
            String studioImageUrl = Utils.getPrimaryImageUrl(item.getStudios()[0], mApplication.getApiClient(), height);
            if (studioImageUrl != null) Picasso.with(mActivity).load(studioImageUrl).resize(width, height).into(mStudioImage);
        } else {
            mStudioImage.setImageResource(R.drawable.blank30x30);
        }

    }

    public void updateEndTime(int timeLeft) {
        mEndTime.setText( timeLeft > 0 ?
                mApplication.getString(R.string.lbl_ends) + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(System.currentTimeMillis() + timeLeft)
                : ""
        );

    }

    private void updateCurrentDuration(BaseItemDto item) {
        Long mbRuntime = item.getRunTimeTicks();
        Long andDuration = mbRuntime != null ? mbRuntime / 10000: 0;
        mCurrentDuration = andDuration.intValue();
    }

    private int getDuration() {
        return mCurrentDuration;
    }

    @Override
    public void setCurrentTime(int time) {
        mCurrentProgress.setProgress(time);
        mCurrentPos.setText(Utils.formatMillis(time));
        mRemainingTime.setText("-"+Utils.formatMillis(mCurrentDuration - time));
    }

    @Override
    public void setFadingEnabled(boolean value) {
        mFadeEnabled = value;
        if (mFadeEnabled) {
            if (mIsVisible) startFadeTimer();
        } else {
            mHandler.removeCallbacks(mHideTask);
            if (!mIsVisible) show();
        }
    }

    @Override
    public void setPlayPauseActionState(int state) {
        mPlayPauseBtn.setState(state);
    }

    @Override
    public void updateDisplay() {
        BaseItemDto current = mPlaybackController.getCurrentlyPlayingItem();
        if (current != null) {
            updateCurrentDuration(current);
            //set progress to match duration
            mCurrentProgress.setMax(mCurrentDuration);
            //set other information
            mTitle.setText(current.getName());
            mInfoRow.removeAllViews();
            updatePoster(current);
            updateStudio(current);
            InfoLayoutHelper.addInfoRow(mActivity, current, mInfoRow, true);
        }
    }

    @Override
    public void removeQueueItem(int pos) {

    }

    @Override
    public void finish() {
        getActivity().finish();
    }
}
