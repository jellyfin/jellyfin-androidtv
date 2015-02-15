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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.VideoView;

import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.TvApp;

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
public class PlaybackOverlayActivity extends Activity {
    private static final String TAG = "PlaybackOverlayActivity";

    private static final double MEDIA_HEIGHT = 0.95;
    private static final double MEDIA_WIDTH = 0.95;
    private static final double MEDIA_TOP_MARGIN = 0.025;
    private static final double MEDIA_RIGHT_MARGIN = 0.025;
    private static final double MEDIA_BOTTOM_MARGIN = 0.025;
    private static final double MEDIA_LEFT_MARGIN = 0.025;

    private VideoView mVideoView;
    private TvApp mApplication;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playback_controls);
        mApplication = TvApp.getApplication();
        loadViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                mApplication.getPlaybackController().play(0);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                mApplication.getPlaybackController().pause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                PlaybackController controller = mApplication.getPlaybackController();
                if (controller.isPlaying()) {
                    controller.pause();
                } else {
                    controller.play(0);
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.videoView);
        mApplication.getPlaybackController().init(mVideoView, findViewById(R.id.bufferingProgress));
        Intent intent = getIntent();
        //start playing
        mApplication.getPlaybackController().play(intent.getIntExtra("Position", 0));
    }

    public void setLogo(String url) {
        if (url != null) {
            ImageView logo = (ImageView) findViewById(R.id.npLogoImage);
            if (logo != null) {
                logo.setImageURI(Uri.parse(url));
            }
        }
    }

}
