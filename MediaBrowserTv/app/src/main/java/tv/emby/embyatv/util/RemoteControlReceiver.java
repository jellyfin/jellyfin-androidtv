package tv.emby.embyatv.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import tv.emby.embyatv.playback.MediaManager;

/**
 * Created by Eric on 4/17/2015.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (MediaManager.isPlayingAudio()) {
            //Respond to media button presses
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        MediaManager.pauseAudio();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        MediaManager.nextAudioItem();
                        break;
                }
            }

        }
        //Otherwise don't do anything here
        //We trap the keypresses in the activities because our actions are contextual
        //We just need this to obtain focus for all media button presses
    }
}
