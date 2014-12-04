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
package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.VideoView;

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
public class PlaybackOverlayActivity extends Activity implements
        PlaybackOverlayFragment.OnPlayPauseClickedListener {
    private static final String TAG = "PlaybackOverlayActivity";

    private static final double MEDIA_HEIGHT = 0.95;
    private static final double MEDIA_WIDTH = 0.95;
    private static final double MEDIA_TOP_MARGIN = 0.025;
    private static final double MEDIA_RIGHT_MARGIN = 0.025;
    private static final double MEDIA_BOTTOM_MARGIN = 0.025;
    private static final double MEDIA_LEFT_MARGIN = 0.025;


    private VideoView mVideoView;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playback_controls);
        loadViews();
        //setupCallbacks();
        overScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVideoView.suspend();
    }

    /**
     * Implementation of OnPlayPauseClickedListener
     */
    public void onFragmentPlayPause(Movie movie, int position, Boolean playPause) {
        mVideoView.setVideoPath(movie.getVideoUrl());

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

    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.videoView);
    }

    private void overScan() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int w = (int) (metrics.widthPixels * MEDIA_WIDTH);
        int h = (int) (metrics.heightPixels * MEDIA_HEIGHT);
        int marginLeft = (int) (metrics.widthPixels * MEDIA_LEFT_MARGIN);
        int marginTop = (int) (metrics.heightPixels * MEDIA_TOP_MARGIN);
        int marginRight = (int) (metrics.widthPixels * MEDIA_RIGHT_MARGIN);
        int marginBottom = (int) (metrics.heightPixels * MEDIA_BOTTOM_MARGIN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        mVideoView.setLayoutParams(lp);
    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                String msg = "";
                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                    msg = getString(R.string.video_error_media_load_timeout);
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    msg = getString(R.string.video_error_server_inaccessible);
                } else {
                    msg = getString(R.string.video_error_unknown_error);
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
            }
        });

    }

    /*
     * List of various states that we can be in
     */
    public static enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE;
    }

}
