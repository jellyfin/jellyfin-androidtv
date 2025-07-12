package org.jellyfin.androidtv.ui.playback.overlay.action.customer;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.customer.CustomerUserPreferences;
import org.jellyfin.androidtv.customer.action.BingeWatchingComponent;
import org.jellyfin.androidtv.danmu.model.AutoSkipModel;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue;
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter;
import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.koin.java.KoinJavaComponent;

public class JellyfinBingeWatchingAction extends CustomAction implements BingeWatchingComponent {
    private final PlaybackController playbackController;
    private final CustomerUserPreferences customerUserPreferences;

    public JellyfinBingeWatchingAction(@NonNull Context context, @NonNull CustomPlaybackTransportControlGlue customPlaybackTransportControlGlue, @NonNull PlaybackController playbackController) {
        super(context, customPlaybackTransportControlGlue);
        this.playbackController = playbackController;
        this.customerUserPreferences = KoinJavaComponent.get(CustomerUserPreferences.class);

        initializeWithIcon(R.drawable.ic_movie);
    }

    @Override
    public void updateCurrentAutoSkipModel(AutoSkipModel autoSkipModel) {
    }

    @Override
    public void handleClickAction(@NonNull PlaybackController playbackController, @NonNull VideoPlayerAdapter videoPlayerAdapter, @NonNull Context context, @NonNull View view) {
        doClick(context, view);
    }

    @Override
    public BaseItemDto getCurrentlyPlayingItem() {
        return playbackController.getCurrentlyPlayingItem();
    }

    @Override
    public CustomerUserPreferences getCustomerUserPreferences() {
        return customerUserPreferences;
    }
}
