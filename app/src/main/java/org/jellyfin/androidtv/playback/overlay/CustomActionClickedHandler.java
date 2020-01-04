package org.jellyfin.androidtv.playback.overlay;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.entities.MediaStream;

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
}
