package tv.emby.embyatv.playback;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.videolan.libvlc.EventHandler;

import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 6/13/2015.
 */
public class VlcEventHandler extends Handler {

    private PlaybackListener onCompletionListener;
    private PlaybackListener onErrorListener;
    private PlaybackListener onPreparedListener;
    private PlaybackListener onProgressListener;

    public void setOnCompletionListener(PlaybackListener listener) { onCompletionListener = listener; }
    public void setOnErrorListener(PlaybackListener listener) { onErrorListener = listener; }
    public void setOnPreparedListener(PlaybackListener listener) { onPreparedListener = listener; }
    public void setOnProgressListener(PlaybackListener listener) { onProgressListener = listener; }

    public void handleMessage(Message msg) {
        // Libvlc events
        Bundle b = msg.getData();
        switch (b.getInt("event")) {
            case EventHandler.MediaPlayerEndReached:
                if (onCompletionListener != null) onCompletionListener.onEvent();
                break;
            case EventHandler.MediaPlayerPlaying:
                if (onPreparedListener != null) onPreparedListener.onEvent();
                break;
            case EventHandler.MediaPlayerPositionChanged:
                if (onProgressListener != null) onProgressListener.onEvent();
                break;
            case EventHandler.MediaPlayerPaused:
            case EventHandler.MediaPlayerStopped:
            default:
                break;
        }
    }
}
