package org.jellyfin.androidtv.util;

import static org.koin.java.KoinJavaComponent.inject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;

import kotlin.Lazy;
import timber.log.Timber;

public class RemoteControlReceiver extends BroadcastReceiver {
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        //Timber.d("****** In remote receiver. ");
        if ((TvApp.getApplication().getCurrentActivity() == null || !(TvApp.getApplication().getCurrentActivity() instanceof AudioNowPlayingActivity )) && mediaManager.getValue().isPlayingAudio()) {
            //Respond to media button presses
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Timber.d("****** In remote receiver.  Keycode: %d", event.getKeyCode());
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        //if the current activity is null then we must not be the foreground app - process play/pause here
                        if (TvApp.getApplication().getCurrentActivity() == null) {
                            mediaManager.getValue().pauseAudio();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        mediaManager.getValue().nextAudioItem();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        mediaManager.getValue().prevAudioItem();
                        break;
                }
                abortBroadcast(); // we handled it - don't pass it on
            }

        }
        //Otherwise don't do anything here
        //We trap the keypresses in the activities because our actions are contextual
        //We just need this to obtain focus for all media button presses
    }
}
