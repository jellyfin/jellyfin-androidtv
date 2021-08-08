package org.jellyfin.androidtv.ui.playback.overlay.action;

import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.VideoManager;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;

public class ZoomAction extends CustomAction {

    public ZoomAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.ic_aspect_ratio);
    }

    @Override
    public void handleClickAction(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment, Context context, View view) {
        PopupMenu zoomMenu = new PopupMenu(context, view, Gravity.END);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_AUTO_CROP, VideoManager.ZOOM_AUTO_CROP, context.getString(R.string.lbl_auto_crop)).setChecked(playbackController.getZoomMode() == VideoManager.ZOOM_AUTO_CROP);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_FIT, VideoManager.ZOOM_FIT, context.getString(R.string.lbl_fit)).setChecked(playbackController.getZoomMode() == VideoManager.ZOOM_FIT);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_STRETCH, VideoManager.ZOOM_STRETCH, context.getString(R.string.lbl_stretch)).setChecked(playbackController.getZoomMode() == VideoManager.ZOOM_STRETCH);

        zoomMenu.getMenu().setGroupCheckable(0, true, false);
        zoomMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                leanbackOverlayFragment.setFading(true);
            }
        });
        zoomMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                playbackController.setZoom(item.getItemId());
                return true;
            }
        });

        zoomMenu.show();
    }
}
