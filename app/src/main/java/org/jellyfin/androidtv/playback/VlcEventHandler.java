package org.jellyfin.androidtv.playback;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.videolan.libvlc.MediaPlayer.Event;

import org.jellyfin.androidtv.TvApp;

/**
 * Created by Eric on 6/13/2015.
 */
public class VlcEventHandler implements org.videolan.libvlc.MediaPlayer.EventListener {

    private PlaybackListener onCompletionListener;
    private PlaybackListener onErrorListener;
    private PlaybackListener onPreparedListener;
    private PlaybackListener onProgressListener;

    public void setOnCompletionListener(PlaybackListener listener) { onCompletionListener = listener; }
    public void setOnErrorListener(PlaybackListener listener) { onErrorListener = listener; }
    public void setOnPreparedListener(PlaybackListener listener) { onPreparedListener = listener; }
    public void setOnProgressListener(PlaybackListener listener) { onProgressListener = listener; }

    @Override
    public void onEvent(Event event) {
        switch (event.type) {
            case Event.EndReached:
                if (onCompletionListener != null) onCompletionListener.onEvent();
                break;
            case Event.Playing:
                if (onPreparedListener != null) onPreparedListener.onEvent();
                break;
            case Event.PositionChanged:
                if (onProgressListener != null) onProgressListener.onEvent();
                break;
            case Event.Paused:
            case Event.Stopped:
            default:
                break;

        }
    }
}
