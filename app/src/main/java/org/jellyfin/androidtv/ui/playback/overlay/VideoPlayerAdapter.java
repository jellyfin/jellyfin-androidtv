package org.jellyfin.androidtv.ui.playback.overlay;

import androidx.leanback.media.PlayerAdapter;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.ChapterInfoDto;

import java.util.List;

public class VideoPlayerAdapter extends PlayerAdapter {

    private final PlaybackController playbackController;
    private CustomPlaybackOverlayFragment customPlaybackOverlayFragment;
    private LeanbackOverlayFragment leanbackOverlayFragment;

    VideoPlayerAdapter(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment) {
        this.playbackController = playbackController;
        this.leanbackOverlayFragment = leanbackOverlayFragment;
    }

    @Override
    public void play() {
        playbackController.play(playbackController.getCurrentPosition());
    }

    @Override
    public void pause() {
        playbackController.pause();
    }

    @Override
    public void rewind() {
        playbackController.rewind();
        updateCurrentPosition();
    }

    @Override
    public void fastForward() {
        playbackController.fastForward();
        updateCurrentPosition();
    }

    @Override
    public void seekTo(long positionInMs) {
        playbackController.seek(positionInMs);
        updateCurrentPosition();
    }

    @Override
    public void next() {
        playbackController.next();
    }

    @Override
    public long getDuration() {
        return getCurrentlyPlayingItem() != null && getCurrentlyPlayingItem().getRunTimeTicks() != null ?
                getCurrentlyPlayingItem().getRunTimeTicks() / 10000 : -1;
    }

    @Override
    public long getCurrentPosition() {
        return playbackController.getCurrentPosition();
    }

    @Override
    public boolean isPlaying() {
        return playbackController.isPlaying();
    }

    @Override
    public long getBufferedPosition() {
        return getDuration();
    }

    void updateCurrentPosition() {
        getCallback().onCurrentPositionChanged(this);
    }

    void updatePlayState() {
        getCallback().onPlayStateChanged(this);
    }

    void updateDuration() {
        getCallback().onDurationChanged(this);
    }

    boolean hasSubs() {
        return StreamHelper.getSubtitleStreams(playbackController.getCurrentMediaSource()).size() > 0;
    }

    boolean hasMultiAudio() {
        return StreamHelper.getAudioStreams(playbackController.getCurrentMediaSource()).size() > 1;
    }

    boolean hasNextItem() {
        return playbackController.hasNextItem();
    }

    boolean isNativeMode() {
        return playbackController.isNativeMode();
    }

    boolean canSeek() {
        return playbackController.canSeek();
    }

    boolean isLiveTv() {
        return playbackController.isLiveTv();
    }

    void setMasterOverlayFragment(CustomPlaybackOverlayFragment customPlaybackOverlayFragment) {
        this.customPlaybackOverlayFragment = customPlaybackOverlayFragment;
    }

    CustomPlaybackOverlayFragment getMasterOverlayFragment() {
        return customPlaybackOverlayFragment;
    }

    LeanbackOverlayFragment getLeanbackOverlayFragment() {
        return leanbackOverlayFragment;
    }

    @Override
    public void onDetachedFromHost() {
        customPlaybackOverlayFragment = null;
        leanbackOverlayFragment = null;
    }

    boolean canRecordLiveTv() {
        BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        return currentlyPlayingItem.getCurrentProgram() != null
                && TvApp.getApplication().canManageRecordings();
    }

    void toggleRecording() {
        BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        getMasterOverlayFragment().toggleRecording(currentlyPlayingItem);
    }

    boolean isRecording() {
        BaseItemDto currentProgram = getCurrentlyPlayingItem().getCurrentProgram();
        if (currentProgram == null) {
            return false;
        } else {
            return currentProgram.getTimerId() != null;
        }
    }

    BaseItemDto getCurrentlyPlayingItem() {
        return playbackController.getCurrentlyPlayingItem();
    }

    boolean hasChapters() {
        BaseItemDto item = getCurrentlyPlayingItem();
        List<ChapterInfoDto> chapters = item.getChapters();
        return chapters != null && chapters.size() > 0;
    }
}
