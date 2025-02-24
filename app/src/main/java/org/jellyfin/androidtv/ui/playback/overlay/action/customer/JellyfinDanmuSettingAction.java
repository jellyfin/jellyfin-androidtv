package org.jellyfin.androidtv.ui.playback.overlay.action.customer;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.customer.CustomerUserPreferences;
import org.jellyfin.androidtv.customer.action.DanmuSettingActionComponent;
import org.jellyfin.androidtv.customer.jellyfin.DanmuPlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter;
import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.koin.java.KoinJavaComponent;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;

public class JellyfinDanmuSettingAction extends CustomAction implements DanmuSettingActionComponent {
    private final DanmuPlaybackController danmuPlaybackController;
    private final CustomerUserPreferences customerUserPreferences;

    public JellyfinDanmuSettingAction(@NonNull Context context, @NonNull CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue, DanmuPlaybackController danmuPlaybackController) {
        super(context, customPlaybackTransportControlGlue);
        this.danmuPlaybackController = danmuPlaybackController;
        this.customerUserPreferences = KoinJavaComponent.get(CustomerUserPreferences.class);

        initializeWithIcon(R.drawable.danmu_setting);
    }

    @Override
    public void handleClickAction(@NonNull PlaybackController playbackController, @NonNull VideoPlayerAdapter videoPlayerAdapter, @NonNull Context context, @NonNull View view) {
        doClick(context, view);
    }


    @Override
    public CustomerUserPreferences getCustomerUserPreferences() {
        return customerUserPreferences;
    }

    @Override
    public BaseItemDto getCurrentlyPlayingItem() {
        return danmuPlaybackController.getCurrentlyPlayingItem();
    }

    @Override
    public DanmakuContext getDanmakuContext() {
        return danmuPlaybackController.getDanmakuContext();
    }

    @Override
    public IDanmakuView getDanmakuView() {
        return danmuPlaybackController.getDanmakuView();
    }
}
