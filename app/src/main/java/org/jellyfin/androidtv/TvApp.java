package org.jellyfin.androidtv;

import android.app.Application;

import androidx.annotation.Nullable;

public class TvApp extends Application {
    private static TvApp app;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;
    }

    @Nullable
    public static TvApp getApplication() {
        return app;
    }
}
