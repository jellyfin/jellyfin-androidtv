package org.jellyfin.androidtv.ui.playback;

import android.media.MediaPlayer;

public interface IVideoView {
    public int getDuration();
    public int getCurrentPosition();
    public boolean isPlaying();

    public void start();
    public void pause();
    public void stopPlayback();
    public void seekTo(int pos);
    public void setVideoPath(String path);

    public void onActivityCreated(PlaybackOverlayActivity activity);
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener);
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener);
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener);
    public void setOnSeekCompleteListener(MediaPlayer mp, MediaPlayer.OnSeekCompleteListener listener);

}
