package org.jellyfin.androidtv.playback;

import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackInfo;

public interface IPlaybackOverlayFragment {
    void setCurrentTime(long time);
    void setSecondaryTime(long time);
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
