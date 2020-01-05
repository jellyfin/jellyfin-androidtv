package org.jellyfin.androidtv.playback.overlay;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.model.compat.SubtitleStreamInfo;
import org.jellyfin.androidtv.playback.CustomPlaybackOverlayFragment;
import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.playback.VideoManager;
import org.jellyfin.androidtv.ui.AudioDelayPopup;
import org.jellyfin.androidtv.ui.ValueChangedListener;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.entities.MediaStream;

import java.util.ArrayList;
import java.util.List;

public class CustomActionClickedHandler {

    private final PlaybackController mPlaybackController;
    private final Context context;

    public CustomActionClickedHandler(PlaybackController playbackController, Context context) {
        this.mPlaybackController = playbackController;
        this.context = context;
    }

    public void handleAudioSelection(View view) {
        List<MediaStream> audioTracks = TvApp.getApplication().getPlaybackManager().getInPlaybackSelectableAudioStreams(mPlaybackController.getCurrentStreamInfo());
        Integer currentAudioIndex = mPlaybackController.getAudioStreamIndex();
        if (!mPlaybackController.isNativeMode() && currentAudioIndex > audioTracks.size()) {
            //VLC has translated this to an ID - we need to translate back to our index positionally
            currentAudioIndex = mPlaybackController.translateVlcAudioId(currentAudioIndex);
        }

        PopupMenu audioMenu = Utils.createPopupMenu(context, view, Gravity.END);
        for (MediaStream audio : audioTracks) {
            MenuItem item = audioMenu.getMenu().add(0, audio.getIndex(), audio.getIndex(), audio.getDisplayTitle());
            if (currentAudioIndex != null && currentAudioIndex == audio.getIndex()) item.setChecked(true);
        }
        audioMenu.getMenu().setGroupCheckable(0, true, false);
        audioMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                //setFadingEnabled(true);
            }
        });
        audioMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mPlaybackController.switchAudioStream(item.getItemId());
                return true;
            }
        });
        audioMenu.show();
    }

    void handleClosedCaptionsSelection(View view) {
        if (mPlaybackController.getCurrentStreamInfo() == null) {
            TvApp.getApplication().getLogger().Warn("StreamInfo null trying to obtain subtitles");
            Utils.showToast(TvApp.getApplication(), "Unable to obtain subtitle info");
            return;
        }
        List<SubtitleStreamInfo> subtitles = mPlaybackController.getSubtitleStreams();
        PopupMenu subMenu = Utils.createPopupMenu(context, view, Gravity.END);
        MenuItem none = subMenu.getMenu().add(0, -1, 0, context.getString(R.string.lbl_none));
        int currentSubIndex = mPlaybackController.getSubtitleStreamIndex();
        if (currentSubIndex < 0) none.setChecked(true);
        for (SubtitleStreamInfo sub : subtitles) {
            MenuItem item = subMenu.getMenu().add(0, sub.getIndex(), sub.getIndex(), sub.getDisplayTitle());
            if (currentSubIndex == sub.getIndex()) item.setChecked(true);
        }
        subMenu.getMenu().setGroupCheckable(0, true, false);
        subMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                //setFadingEnabled(true);
            }
        });
        subMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mPlaybackController.switchSubtitleStream(item.getItemId());
                return true;
            }
        });
        subMenu.show();
    }

    void handleAudioDelaySelection(View view) {
        new AudioDelayPopup(context, view, new ValueChangedListener<Long>() {
            @Override
            public void onValueChanged(Long value) {
                mPlaybackController.setAudioDelay(value);
            }
        }).show(mPlaybackController.getAudioDelay());
    }

    void handleZoomSelection(View view) {
        PopupMenu zoomMenu = Utils.createPopupMenu(context, view, Gravity.RIGHT);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_NORMAL, VideoManager.ZOOM_NORMAL, context.getString(R.string.lbl_normal)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_NORMAL);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_VERTICAL, VideoManager.ZOOM_VERTICAL, context.getString(R.string.lbl_vertical_stretch)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_VERTICAL);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_HORIZONTAL, VideoManager.ZOOM_HORIZONTAL, context.getString(R.string.lbl_horizontal_stretch)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_HORIZONTAL);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_FULL, VideoManager.ZOOM_FULL, context.getString(R.string.lbl_zoom)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_FULL);

        zoomMenu.getMenu().setGroupCheckable(0, true, false);
        zoomMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                //setFadingEnabled(true);
            }
        });
        zoomMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mPlaybackController.setZoom(item.getItemId());
                return true;
            }
        });

        //setFadingEnabled(false);
        zoomMenu.show();
    }

    void handlePreviousLiveTvChannelSelection(CustomPlaybackOverlayFragment customPlaybackOverlayFragment) {
        customPlaybackOverlayFragment.switchChannel(TvManager.getPrevLiveTvChannel());
    }

    void handleChannelBarSelection(CustomPlaybackOverlayFragment customPlaybackOverlayFragment) {
        customPlaybackOverlayFragment.showQuickChannelChanger();
    }

    void handleGuideSelection(CustomPlaybackOverlayFragment customPlaybackOverlayFragment) {
        customPlaybackOverlayFragment.showGuide();
    }
}
