package tv.emby.embyatv.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Eric on 4/17/2015.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Don't do anything here
        //We trap the keypresses in the activities because our actions are contextual
        //We just need this to obtain focus for all media button presses
    }
}
