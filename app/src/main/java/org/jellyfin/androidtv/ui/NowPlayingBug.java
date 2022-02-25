package org.jellyfin.androidtv.ui;

import static org.koin.java.KoinJavaComponent.inject;

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
import org.jellyfin.androidtv.databinding.NowPlayingBugBinding;
import org.jellyfin.androidtv.ui.playback.AudioEventListener;
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import kotlin.Lazy;

public class NowPlayingBug extends FrameLayout {
    ImageView npIcon;
    TextView npDesc;
    TextView npStatus;
    String currentDuration;
    Context context;
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);

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
        NowPlayingBugBinding binding = NowPlayingBugBinding.inflate(inflater, this, true);
        if (!isInEditMode()) {
            npIcon = binding.npIcon;
            npDesc = binding.npDesc;
            npStatus = binding.npStatus;
            this.setFocusable(true);
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent np = new Intent(context, AudioNowPlayingActivity.class);
                    context.startActivity(np);
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
            if (currentItem == null)
                return;

            if (newState == PlaybackController.PlaybackState.PLAYING) {
                setInfo(currentItem);
            } else if (newState == PlaybackController.PlaybackState.IDLE && isShown()) {
                setStatus(mediaManager.getValue().getCurrentAudioPosition());
            }
        }

        @Override
        public void onProgress(long pos) {
            if (isShown()) setStatus(pos);
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            if (hasQueue) {
                // may have just added one so update display
                setInfo(mediaManager.getValue().getCurrentAudioItem());
                setStatus(mediaManager.getValue().getCurrentAudioPosition());
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
            mediaManager.getValue().addAudioEventListener(listener);
            if (manageVisibility()) {
                setInfo(mediaManager.getValue().getCurrentAudioItem());
                setStatus(mediaManager.getValue().getCurrentAudioPosition());
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            // unhook our events
            mediaManager.getValue().removeAudioEventListener(listener);
        }
    }

    private void setInfo(BaseItemDto item) {
        if (item == null) return;

        Glide.with(context)
                .load(ImageUtils.getPrimaryImageUrl(item))
                .error(R.drawable.ic_album)
                .centerInside()
                .into(npIcon);
        currentDuration = TimeUtils.formatMillis(item.getRunTimeTicks() != null ? item.getRunTimeTicks() / 10000 : 0);
        npDesc.setText(item.getAlbumArtist() != null ? item.getAlbumArtist() : item.getName());
    }

    private void setStatus(long pos) {
        npStatus.setText(getResources().getString(R.string.lbl_status, TimeUtils.formatMillis(pos), currentDuration));
    }

    public boolean manageVisibility() {
        this.setVisibility(mediaManager.getValue().hasAudioQueueItems() ? VISIBLE : GONE);
        return mediaManager.getValue().hasAudioQueueItems();
    }

    public void showDescription(boolean show) {
        npDesc.setVisibility(show ? VISIBLE : GONE);
    }
}
