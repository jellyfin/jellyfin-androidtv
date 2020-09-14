package org.jellyfin.androidtv.ui.playback;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.widget.VideoView;

public class StdVideoView extends VideoView implements IVideoView {
    public StdVideoView(Context context) {
        super(context);
    }

    public StdVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StdVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onActivityCreated(PlaybackOverlayActivity activity) {
    }

    @Override
    public void setOnSeekCompleteListener(MediaPlayer mp, MediaPlayer.OnSeekCompleteListener listener) {
        mp.setOnSeekCompleteListener(listener);
    }
}
