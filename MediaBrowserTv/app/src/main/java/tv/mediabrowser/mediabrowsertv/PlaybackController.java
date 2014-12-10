package tv.mediabrowser.mediabrowsertv;

import android.media.MediaPlayer;
import android.os.Handler;
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

    private Runnable mReportLoop;
    private Handler mHandler;
    private static int REPORT_INTERVAL = 3000;


    public PlaybackController(List<BaseItemDto> items) {
        mItems = items;
        mApplication = TvApp.getApplication();
        mHandler = new Handler();

    }

    public void init(VideoView view) {
        mVideoView = view;
        setupCallbacks();
    }

    public BaseItemDto getCurrentlyPlayingItem() {
        return mItems.get(mCurrentIndex);
    }

    public boolean isPlaying() {
        return mPlaybackState == PlaybackState.PLAYING;
    }

    public void play(int position) {
        switch (mPlaybackState) {
            case PLAYING:
                // do nothing
                break;
            case PAUSED:
                // just resume
                mVideoView.start();
                break;
            case BUFFERING:
                // onPrepared should take care of it
                break;
            case IDLE:
                // start new playback
                Utils.Play(getCurrentlyPlayingItem(), position, mVideoView);
                mPlaybackState = PlaybackState.BUFFERING;
                break;
        }
    }

    public void pause() {
        mPlaybackState = PlaybackState.PAUSED;
        stopReportLoop();
        mVideoView.pause();

    }

    public void stop() {
        mPlaybackState = PlaybackState.IDLE;
        stopReportLoop();
        Utils.Stop(getCurrentlyPlayingItem(), mCurrentPosition * 10000);
        mVideoView.stopPlayback();
    }

    public void next() {

    }

    public void prev() {

    }

    private void startReportLoop() {
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    Utils.ReportProgress(getCurrentlyPlayingItem(), mVideoView.getCurrentPosition() * 10000);
                }
                mHandler.postDelayed(this, REPORT_INTERVAL);
            }
        };
        mHandler.postDelayed(mReportLoop, REPORT_INTERVAL);
    }

    private void stopReportLoop() {
        if (mHandler != null && mReportLoop != null) {
            mHandler.removeCallbacks(mReportLoop);
        }

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
                if (mPlaybackState == PlaybackState.BUFFERING) {
                    mVideoView.start();
                    mPlaybackState = PlaybackState.PLAYING;
                    startReportLoop();
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
