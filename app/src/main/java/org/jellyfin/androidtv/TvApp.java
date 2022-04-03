package org.jellyfin.androidtv;

import android.app.Application;

import androidx.annotation.Nullable;

import org.jellyfin.apiclient.model.dto.BaseItemDto;

public class TvApp extends Application {
    private static TvApp app;
    private BaseItemDto lastPlayedItem;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;
    }

    @Nullable
    public static TvApp getApplication() {
        return app;
    }

    @Nullable
    public BaseItemDto getLastPlayedItem() {
        return lastPlayedItem;
    }

    public void setLastPlayedItem(BaseItemDto lastPlayedItem) {
        this.lastPlayedItem = lastPlayedItem;
    }
}
