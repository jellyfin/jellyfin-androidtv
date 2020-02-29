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
package org.jellyfin.androidtv.playback;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
public class PlaybackOverlayActivity extends BaseActivity {
    private static final String TAG = "PlaybackOverlayActivity";

    private static final double MEDIA_HEIGHT = 0.95;
    private static final double MEDIA_WIDTH = 0.95;
    private static final double MEDIA_TOP_MARGIN = 0.025;
    private static final double MEDIA_RIGHT_MARGIN = 0.025;
    private static final double MEDIA_BOTTOM_MARGIN = 0.025;
    private static final double MEDIA_LEFT_MARGIN = 0.025;

    private VideoManager mVideoManager;
    private TvApp mApplication;

    private View.OnKeyListener mKeyListener;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Workaround for Sony Bravia devices that show a "grey" background on HDR videos
        getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_playback_overlay);
        mApplication = TvApp.getApplication();
        loadViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mVideoManager != null) {
            mVideoManager.destroy();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mKeyListener != null) if (mKeyListener.onKey(getCurrentFocus(), keyCode, event)) return true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                mApplication.getPlaybackController().play(0);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                mApplication.getPlaybackController().pause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                mApplication.getPlaybackController().playPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_R2:
                mApplication.getPlaybackController().skip(30000);
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_BUTTON_L1:
            case KeyEvent.KEYCODE_BUTTON_L2:
                mApplication.getPlaybackController().skip(-11000);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void loadViews() {
        if (mApplication.getPlaybackController() != null) {
            mVideoManager = new VideoManager(this, findViewById(android.R.id.content));
            mApplication.getPlaybackController().init(mVideoManager, findViewById(R.id.bufferingProgress));
        }
    }

    public void setKeyListener(View.OnKeyListener listener) {
        mKeyListener = listener;
    }
}
