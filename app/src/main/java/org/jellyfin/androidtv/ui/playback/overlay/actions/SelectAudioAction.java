package org.jellyfin.androidtv.ui.playback.overlay.actions;

import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.entities.MediaStream;

import java.util.List;

public class SelectAudioAction extends CustomAction {

    public SelectAudioAction(Context context, CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue) {
        super(context, customPlaybackTransportControlGlue);
        initializeWithIcon(R.drawable.ic_select_audio);
    }

    @Override
    public void handleClickAction(PlaybackController playbackController, LeanbackOverlayFragment leanbackOverlayFragment, Context context, View view) {

        List<MediaStream> audioTracks = TvApp.getApplication().getPlaybackManager().getInPlaybackSelectableAudioStreams(playbackController.getCurrentStreamInfo());
        Integer currentAudioIndex = playbackController.getAudioStreamIndex();
        if (!playbackController.isNativeMode() && currentAudioIndex > audioTracks.size()) {
            //VLC has translated this to an ID - we need to translate back to our index positionally
            currentAudioIndex = playbackController.translateVlcAudioId(currentAudioIndex);
        }

        PopupMenu audioMenu = Utils.createPopupMenu(context, view, Gravity.END);
        for (MediaStream audio : audioTracks) {
            MenuItem item = audioMenu.getMenu().add(0, audio.getIndex(), audio.getIndex(), audio.getDisplayTitle());
            if (currentAudioIndex != null && currentAudioIndex == audio.getIndex())
                item.setChecked(true);
        }
        audioMenu.getMenu().setGroupCheckable(0, true, false);
        audioMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                leanbackOverlayFragment.setFading(true);
            }
        });
        audioMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                playbackController.switchAudioStream(item.getItemId());
                return true;
            }
        });
        audioMenu.show();
    }
}
