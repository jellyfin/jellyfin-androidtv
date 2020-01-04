package org.jellyfin.androidtv.playback.overlay;

import androidx.leanback.media.PlayerAdapter;

import org.jellyfin.androidtv.playback.PlaybackController;

public class VideoPlayerAdapter extends PlayerAdapter {

    private final PlaybackController playbackController;

    VideoPlayerAdapter(PlaybackController playbackController) {
        this.playbackController = playbackController;
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
        playbackController.skip(-30000);
        updateCurrentPosition();
    }

    @Override
    public void fastForward() {
        playbackController.skip(30000);
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
        return playbackController.getCurrentlyPlayingItem().getRunTimeTicks()!= null ?
                playbackController.getCurrentlyPlayingItem().getRunTimeTicks() / 10000 : -1;
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
}
