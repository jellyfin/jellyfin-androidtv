package tv.emby.embyatv.playback;

import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;

/**
 * Created by Eric on 10/22/2015.
 */
public class MediaManager {

    private static ItemRowAdapter currentMediaAdapter;
    private static int currentMediaPosition = -1;

    public static ItemRowAdapter getCurrentMediaAdapter() {
        return currentMediaAdapter;
    }

    public static void setCurrentMediaAdapter(ItemRowAdapter currentMediaAdapter) {
        MediaManager.currentMediaAdapter = currentMediaAdapter;
    }

    public static int getCurrentMediaPosition() {
        return currentMediaPosition;
    }

    public static void setCurrentMediaPosition(int currentMediaPosition) {
        MediaManager.currentMediaPosition = currentMediaPosition;
    }

    public static BaseRowItem getMediaItem(int pos) {
        return currentMediaAdapter != null && currentMediaAdapter.size() > pos ? (BaseRowItem) currentMediaAdapter.get(pos) : null;
    }

    public static BaseRowItem getCurrentMediaItem() { return getMediaItem(currentMediaPosition); }
    public static BaseRowItem next() {
        if (hasNextMediaItem()) {
            currentMediaPosition++;
            currentMediaAdapter.loadMoreItemsIfNeeded(currentMediaPosition);
        }

        return getCurrentMediaItem();
    }

    public static BaseRowItem prev() {
        if (hasPrevMediaItem()) {
            currentMediaPosition--;
        }

        return getCurrentMediaItem();
    }

    public static BaseRowItem peekNextMediaItem() {
        return hasNextMediaItem() ? getMediaItem(currentMediaPosition+1) : null;
    }

    public static BaseRowItem peekPrevMediaItem() {
        return hasPrevMediaItem() ? getMediaItem(currentMediaPosition-1) : null;
    }

    public static boolean hasNextMediaItem() { return currentMediaAdapter.size() > currentMediaPosition+1; }
    public static boolean hasPrevMediaItem() { return currentMediaPosition > 0; }
}
