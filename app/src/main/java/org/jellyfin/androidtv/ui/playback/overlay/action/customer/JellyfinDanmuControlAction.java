package org.jellyfin.androidtv.ui.playback.overlay.action.customer;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.leanback.widget.Action;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.customer.CustomerUserPreferences;
import org.jellyfin.androidtv.customer.jellyfin.DanmuPlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter;
import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;
import org.koin.java.KoinJavaComponent;

public class JellyfinDanmuControlAction extends CustomAction {
    private final DanmuPlaybackController danmuPlaybackController;
    private final CustomerUserPreferences customerUserPreferences;
    private final Consumer<Action> buttonRefresher;

    public JellyfinDanmuControlAction(@NonNull Context context, @NonNull CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue, DanmuPlaybackController danmuPlaybackController, Consumer<Action> buttonRefresher) {
        super(context, customPlaybackTransportControlGlue);
        this.danmuPlaybackController = danmuPlaybackController;
        this.buttonRefresher = buttonRefresher;
        customerUserPreferences = KoinJavaComponent.get(CustomerUserPreferences.class);

        changeDanmuIcon(customerUserPreferences.isDanmuController());
    }

    private void changeDanmuIcon(boolean open) {
        initializeWithIcon(open ? R.drawable.ic_danmu_open : R.drawable.ic_danmu_close);
    }

    @Override
    public void handleClickAction(@NonNull PlaybackController playbackController, @NonNull VideoPlayerAdapter videoPlayerAdapter, @NonNull Context context, @NonNull View view) {
        // 当前不展示 => 变成展示
        boolean toOpen = !danmuPlaybackController.isShowDanmu();
        customerUserPreferences.setDanmuController(toOpen);
        danmuPlaybackController.changeDanmuShow(toOpen);

        initializeWithIcon(toOpen? R.drawable.ic_danmu_open : R.drawable.ic_danmu_close);
        buttonRefresher.accept(this);
    }
}
