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
import org.jellyfin.androidtv.util.Utils;

public class ZoomAction extends CustomAction {

    public ZoomAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.ic_aspect_ratio);
    }

    @Override
    public void handleClickAction(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment, Context context, View view) {
        PopupMenu zoomMenu = Utils.createPopupMenu(context, view, Gravity.RIGHT);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_NORMAL, VideoManager.ZOOM_NORMAL, context.getString(R.string.lbl_normal)).setChecked(playbackController.getZoomMode() == VideoManager.ZOOM_NORMAL);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_VERTICAL, VideoManager.ZOOM_VERTICAL, context.getString(R.string.lbl_vertical_stretch)).setChecked(playbackController.getZoomMode() == VideoManager.ZOOM_VERTICAL);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_HORIZONTAL, VideoManager.ZOOM_HORIZONTAL, context.getString(R.string.lbl_horizontal_stretch)).setChecked(playbackController.getZoomMode() == VideoManager.ZOOM_HORIZONTAL);
        zoomMenu.getMenu().add(0, VideoManager.ZOOM_FULL, VideoManager.ZOOM_FULL, context.getString(R.string.lbl_zoom)).setChecked(playbackController.getZoomMode() == VideoManager.ZOOM_FULL);

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
