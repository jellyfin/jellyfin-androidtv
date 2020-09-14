package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.playback.AudioEventListener;
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

public class NowPlayingBug extends FrameLayout {
    ImageView npIcon;
    TextView npDesc;
    TextView npStatus;
    String currentDuration;
    Context context;

    public NowPlayingBug(Context context) {
        super(context);
        init(context);
    }

    public NowPlayingBug(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.now_playing_bug, null, false);
        this.addView(v);
        if (!isInEditMode()) {
            npIcon = (ImageView)v.findViewById(R.id.npIcon);
            npDesc = ((TextView) v.findViewById(R.id.npDesc));
            npStatus = ((TextView) v.findViewById(R.id.npStatus));
            this.setFocusable(true);
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TvApp.getApplication().getCurrentActivity() != null) {
                        Intent np = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
                        TvApp.getApplication().getCurrentActivity().startActivity(np);
                    }
                }
            });
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            this.setBackgroundResource(R.drawable.btn_focus);
        } else {
            this.setBackground(null);
        }
    }

    AudioEventListener listener = new AudioEventListener() {
        @Override
        public void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem) {
            if (newState == PlaybackController.PlaybackState.PLAYING && currentItem != null) setInfo(currentItem);
        }

        @Override
        public void onProgress(long pos) {
            if (isShown()) setStatus(pos);
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            if (hasQueue) {
                // may have just added one so update display
                setInfo(MediaManager.getCurrentAudioItem());
                setStatus(MediaManager.getCurrentAudioPosition());
                setVisibility(VISIBLE);
            } else {
                setVisibility(GONE);
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            // hook our events
            MediaManager.addAudioEventListener(listener);
            if (manageVisibility()) {
                setInfo(MediaManager.getCurrentAudioItem());
                setStatus(MediaManager.getCurrentAudioPosition());
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            // unhook our events
            MediaManager.removeAudioEventListener(listener);
        }
    }

    private void setInfo(BaseItemDto item) {
        if (item == null) return;

        Glide.with(context).load(ImageUtils.getPrimaryImageUrl(item, TvApp.getApplication().getApiClient())).error(R.drawable.ic_album).override(35,35).centerInside().into(npIcon);
        currentDuration = TimeUtils.formatMillis(item.getRunTimeTicks() != null ? item.getRunTimeTicks() / 10000 : 0);
        npDesc.setText(item.getAlbumArtist() != null ? item.getAlbumArtist() : item.getName());
    }

    private void setStatus(long pos) {
        npStatus.setText(getResources().getString(R.string.lbl_status, TimeUtils.formatMillis(pos), currentDuration));
    }

    public boolean manageVisibility() {
        this.setVisibility(MediaManager.hasAudioQueueItems() ? VISIBLE : GONE);
        return MediaManager.hasAudioQueueItems();
    }

    public void showDescription(boolean show) {
        npDesc.setVisibility(show ? VISIBLE : GONE);
    }
}
