package org.jellyfin.androidtv.playback.overlay.actions;

import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.compat.SubtitleStreamInfo;
import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.util.Utils;

import java.util.List;

import timber.log.Timber;

public class ClosedCaptionsAction extends CustomAction {

    public ClosedCaptionsAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.ic_select_subtitle);
    }

    @Override
    public void handleClickAction(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment, Context context, View view) {
        if (playbackController.getCurrentStreamInfo() == null) {
            Timber.w("StreamInfo null trying to obtain subtitles");
            Utils.showToast(TvApp.getApplication(), "Unable to obtain subtitle info");
            return;
        }
        List<SubtitleStreamInfo> subtitles = playbackController.getSubtitleStreams();
        PopupMenu subMenu = Utils.createPopupMenu(context, view, Gravity.END);
        MenuItem none = subMenu.getMenu().add(0, -1, 0, context.getString(R.string.lbl_none));
        int currentSubIndex = playbackController.getSubtitleStreamIndex();
        if (currentSubIndex < 0) none.setChecked(true);
        for (SubtitleStreamInfo sub : subtitles) {
            MenuItem item = subMenu.getMenu().add(0, sub.getIndex(), sub.getIndex(), sub.getDisplayTitle());
            if (currentSubIndex == sub.getIndex()) item.setChecked(true);
        }
        subMenu.getMenu().setGroupCheckable(0, true, false);
        subMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                leanbackOverlayFragment.setFading(true);
            }
        });
        subMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                playbackController.switchSubtitleStream(item.getItemId());
                return true;
            }
        });
        subMenu.show();
    }
}
