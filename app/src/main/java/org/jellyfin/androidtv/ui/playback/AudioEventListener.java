package org.jellyfin.androidtv.ui.playback;

import org.jellyfin.apiclient.model.dto.BaseItemDto;

public interface AudioEventListener {
    public default void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem) {
    }

    public default void onProgress(long pos) {
    }

    public default void onQueueStatusChanged(boolean hasQueue) {
    }

    public default void onQueueReplaced() {
    }
}
