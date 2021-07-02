package org.jellyfin.androidtv;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.UserDto;

import kotlin.Lazy;

public class TvApp extends Application {
    public static final int LIVE_TV_GUIDE_OPTION_ID = 1000;
    public static final int LIVE_TV_RECORDINGS_OPTION_ID = 2000;
    public static final int VIDEO_QUEUE_OPTION_ID = 3000;
    public static final int LIVE_TV_SCHEDULE_OPTION_ID = 4000;
    public static final int LIVE_TV_SERIES_OPTION_ID = 5000;

    private static TvApp app;
    private MediatorLiveData<UserDto> currentUser = new MediatorLiveData<UserDto>();
    private BaseItemDto lastPlayedItem;
    private PlaybackController playbackController;

    private Activity currentActivity;
    private Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);
    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;
    }

    @Nullable
    public static TvApp getApplication() {
        return app;
    }

    @Deprecated
    @Nullable
    public UserDto getCurrentUser() {
        return currentUser.getValue();
    }

    @Deprecated
    public LiveData<UserDto> getCurrentUserLiveData() {
        return currentUser;
    }

    @Deprecated
    public void setCurrentUser(UserDto currentUser) {
        this.currentUser.postValue(currentUser);
        TvManager.clearCache();
    }

    /**
     * @deprecated This function is causing a **lot** of issues because not all activities will set their self as "currentactivity". Try to receive a Context instance instead.
     */
    @Deprecated
    @Nullable
    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity activity) {
        currentActivity = activity;
    }

    @Nullable
    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    public void setPlaybackController(PlaybackController playbackController) {
        this.playbackController = playbackController;
    }

    @NonNull
    public boolean canManageRecordings() {
        UserDto currentUser = getCurrentUser();
        return currentUser != null && currentUser.getPolicy().getEnableLiveTvManagement();
    }

    @Nullable
    public BaseItemDto getLastPlayedItem() {
        return lastPlayedItem;
    }

    public void setLastPlayedItem(BaseItemDto lastPlayedItem) {
        this.lastPlayedItem = lastPlayedItem;
    }
}
