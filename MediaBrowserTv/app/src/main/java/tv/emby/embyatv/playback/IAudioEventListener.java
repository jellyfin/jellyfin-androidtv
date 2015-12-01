package tv.emby.embyatv.playback;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 12/1/2015.
 */
public interface IAudioEventListener {
    void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem);
    void onProgress(long pos);
}
