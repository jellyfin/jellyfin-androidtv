package tv.mediabrowser.mediabrowsertv;

import android.media.MediaPlayer;
import android.widget.VideoView;

import java.util.List;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 12/9/2014.
 */
public class PlaybackController {
    List<BaseItemDto> mItems;
    VideoView mVideoView;
    int mCurrentIndex = 0;
    private int mCurrentPosition = 0;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;
    private TvApp mApplication;


    public PlaybackController(List<BaseItemDto> items) {
        mItems = items;
        mApplication = TvApp.getApplication();

    }

    public void init(VideoView view) {
        mVideoView = view;
        setupCallbacks();
    }

    public BaseItemDto getCurrentlyPlayingItem() {
        return mItems.get(mCurrentIndex);
    }

    /**
     * Implementation of OnPlayPauseClickedListener
     */
    public void onFragmentPlayPause(int position, Boolean playPause) {
        Utils.Play(getCurrentlyPlayingItem(), mVideoView);

        if (position == 0 || mPlaybackState == PlaybackState.IDLE) {
            setupCallbacks();
            mPlaybackState = PlaybackState.IDLE;
        }

        if (playPause && mPlaybackState != PlaybackState.PLAYING) {
            mPlaybackState = PlaybackState.PLAYING;
            if (position > 0) {
                mVideoView.seekTo(position);
                mVideoView.start();
            }
        } else {
            mPlaybackState = PlaybackState.PAUSED;
            mVideoView.pause();
        }
    }

    public void play() {

    }

    public void next() {

    }

    public void prev() {

    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                String msg = "";
                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                    msg = mApplication.getString(R.string.video_error_media_load_timeout);
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    msg = mApplication.getString(R.string.video_error_server_inaccessible);
                } else {
                    msg = mApplication.getString(R.string.video_error_unknown_error);
                }
                mVideoView.stopPlayback();
                mPlaybackState = PlaybackState.IDLE;
                return false;
            }
        });


        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    mVideoView.start();
                }
            }
        });


        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlaybackState = PlaybackState.IDLE;
                Long mbPos = (long) mVideoView.getCurrentPosition() * 10000;
                Utils.Stop(TvApp.getApplication().getCurrentPlayingItem(), mbPos);
                mVideoView.suspend();
            }
        });

    }

    public int getmCurrentPosition() {
        return mCurrentPosition;
    }

    public void setmCurrentPosition(int mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
    }


    /*
 * List of various states that we can be in
 */
    public static enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE;
    }

}
