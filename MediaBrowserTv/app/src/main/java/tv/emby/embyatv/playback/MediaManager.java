package tv.emby.embyatv.playback;

import android.support.v17.leanback.widget.ArrayObjectAdapter;

import tv.emby.embyatv.itemhandling.ItemRowAdapter;

/**
 * Created by Eric on 10/22/2015.
 */
public class MediaManager {

    private static ItemRowAdapter currentQueue;

    public static ItemRowAdapter getCurrentQueue() {
        return currentQueue;
    }

    public static void setCurrentQueue(ItemRowAdapter currentQueue) {
        MediaManager.currentQueue = currentQueue;
    }
}
