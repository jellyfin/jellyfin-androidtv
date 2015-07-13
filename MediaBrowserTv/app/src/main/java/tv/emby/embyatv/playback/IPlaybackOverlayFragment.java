package tv.emby.embyatv.playback;

import mediabrowser.model.dto.BaseItemDto;

/**
 * Created by Eric on 4/28/2015.
 */
public interface IPlaybackOverlayFragment {
    public void setCurrentTime(long time);
    public void setFadingEnabled(boolean value);
    public void setPlayPauseActionState(int state);
    public void updateDisplay();
    public void updateEndTime(long timeLeft);
    public void removeQueueItem(int pos);
    public void nextItemThresholdHit(BaseItemDto nextItem);
    public void finish();
}
