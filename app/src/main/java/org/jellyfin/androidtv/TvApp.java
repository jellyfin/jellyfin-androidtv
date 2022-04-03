package org.jellyfin.androidtv;

import android.app.Application;

import androidx.annotation.Nullable;

import org.jellyfin.androidtv.auth.UserRepository;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.sdk.model.api.UserDto;
import org.koin.java.KoinJavaComponent;

import kotlin.Deprecated;
import kotlin.ReplaceWith;

public class TvApp extends Application {
    private static TvApp app;
    private BaseItemDto lastPlayedItem;
    private PlaybackController playbackController;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;
    }

    @Nullable
    public static TvApp getApplication() {
        return app;
    }

    @Deprecated(message = "Use UserRepository", replaceWith = @ReplaceWith(expression = "KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue()", imports = {}))
    @Nullable
    public UserDto getCurrentUser() {
        return KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue();
    }

    @Nullable
    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    public void setPlaybackController(PlaybackController playbackController) {
        this.playbackController = playbackController;
    }

    @Nullable
    public BaseItemDto getLastPlayedItem() {
        return lastPlayedItem;
    }

    public void setLastPlayedItem(BaseItemDto lastPlayedItem) {
        this.lastPlayedItem = lastPlayedItem;
    }
}
