package tv.emby.embyatv.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.playback.AudioEventListener;
import tv.emby.embyatv.playback.AudioNowPlayingActivity;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackController;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 7/22/2015.
 */
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
            Typeface font = TvApp.getApplication().getDefaultFont();
            npIcon = (ImageView)v.findViewById(R.id.npIcon);
            npDesc = ((TextView) v.findViewById(R.id.npDesc));
            npDesc.setTypeface(font);
            npStatus = ((TextView) v.findViewById(R.id.npStatus));
            npStatus.setTypeface(font);
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
            this.setBackgroundResource(R.color.lb_default_brand_color);
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
            if (isShown()) npStatus.setText(Utils.formatMillis(pos) + "/" + currentDuration);
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            if (hasQueue) {
                // may have just added one so update display
                setInfo(MediaManager.getCurrentAudioItem());
                npStatus.setText(Utils.formatMillis(MediaManager.getCurrentAudioPosition()) + "/" + currentDuration);
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
                npStatus.setText(Utils.formatMillis(MediaManager.getCurrentAudioPosition()) + "/" + currentDuration);
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

        Picasso.with(context).load(Utils.getPrimaryImageUrl(item, TvApp.getApplication().getApiClient())).error(R.drawable.audioicon).resize(35,35).centerInside().into(npIcon);
        currentDuration = Utils.formatMillis(item.getRunTimeTicks() != null ? item.getRunTimeTicks() / 10000 : 0);
        npDesc.setText(item.getAlbumArtist() != null ? item.getAlbumArtist() : item.getName());

    }

    public boolean manageVisibility() {
        this.setVisibility(MediaManager.hasAudioQueueItems() ? VISIBLE : GONE);
        return MediaManager.hasAudioQueueItems();
    }

    public void showDescription(boolean show) {
        npDesc.setVisibility(show ? VISIBLE : GONE);
    }
}
