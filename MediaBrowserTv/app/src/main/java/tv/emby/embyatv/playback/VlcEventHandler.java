package tv.emby.embyatv.playback;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.videolan.libvlc.EventHandler;

/**
 * Created by Eric on 6/13/2015.
 */
public class VlcEventHandler extends Handler {

    private MediaPlayer.OnCompletionListener onCompletionListener;
    private MediaPlayer.OnErrorListener onErrorListener;
    private MediaPlayer.OnPreparedListener onPreparedListener;

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) { onCompletionListener = listener; }
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) { onErrorListener = listener; }
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) { onPreparedListener = listener; }

    public void handleMessage(Message msg) {
        // Libvlc events
        Bundle b = msg.getData();
        switch (b.getInt("event")) {
            case EventHandler.MediaPlayerEndReached:
                if (onCompletionListener != null) onCompletionListener.onCompletion(null);
                break;
            case EventHandler.MediaPlayerPlaying:
                if (onPreparedListener != null) onPreparedListener.onPrepared(null);
                break;
            case EventHandler.MediaPlayerPositionChanged:

            case EventHandler.MediaPlayerPaused:
            case EventHandler.MediaPlayerStopped:
            default:
                break;
        }
    }
}
