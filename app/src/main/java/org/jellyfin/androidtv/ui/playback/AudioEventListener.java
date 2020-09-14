package org.jellyfin.androidtv.ui.playback;

import org.jellyfin.apiclient.model.dto.BaseItemDto;

public class AudioEventListener {
    public void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem) {}
    public void onProgress(long pos) {}
    public void onQueueStatusChanged(boolean hasQueue) {}
    public void onQueueReplaced(){}
}
