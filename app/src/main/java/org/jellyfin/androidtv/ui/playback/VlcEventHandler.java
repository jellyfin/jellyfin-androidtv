package org.jellyfin.androidtv.ui.playback;

import org.videolan.libvlc.MediaPlayer.Event;

public class VlcEventHandler implements org.videolan.libvlc.MediaPlayer.EventListener {

    private PlaybackListener onCompletionListener;
    private PlaybackListener onErrorListener;
    private PlaybackListener onPreparedListener;
    private PlaybackListener onProgressListener;

    public void setOnCompletionListener(PlaybackListener listener) {
        onCompletionListener = listener;
    }

    public void setOnErrorListener(PlaybackListener listener) {
        onErrorListener = listener;
    }

    public void setOnPreparedListener(PlaybackListener listener) {
        onPreparedListener = listener;
    }

    public void setOnProgressListener(PlaybackListener listener) {
        onProgressListener = listener;
    }

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
            case Event.EncounteredError:
                if (onErrorListener != null) onErrorListener.onEvent();
            case Event.Paused:
            case Event.Stopped:
            default:
                break;

        }
    }
}
