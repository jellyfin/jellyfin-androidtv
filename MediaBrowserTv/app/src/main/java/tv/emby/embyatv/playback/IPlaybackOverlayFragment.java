package tv.emby.embyatv.playback;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.mediainfo.SubtitleTrackInfo;

/**
 * Created by Eric on 4/28/2015.
 */
public interface IPlaybackOverlayFragment {
    void setCurrentTime(long time);
    void setFadingEnabled(boolean value);
    void setPlayPauseActionState(int state);
    void updateDisplay();
    void updateEndTime(long timeLeft);
    void nextItemThresholdHit(BaseItemDto nextItem);
    void finish();
    void addManualSubtitles(SubtitleTrackInfo info);
    void updateSubtitles(long posMs);
    void showSubLoadingMsg(boolean show);
}
