package org.jellyfin.androidtv;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraHttpSender;
import org.acra.annotation.AcraLimiter;
import org.acra.sender.HttpSender;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.model.LogonCredentials;
import org.jellyfin.androidtv.di.AppModuleKt;
import org.jellyfin.androidtv.di.PreferenceModuleKt;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer;
import org.jellyfin.androidtv.ui.shared.AppThemeCallbacks;
import org.jellyfin.androidtv.ui.shared.AuthenticatedUserCallbacks;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackManager;
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity;
import org.jellyfin.apiclient.interaction.AndroidDevice;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.logging.AndroidLogger;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.entities.DisplayPreferences;
import org.koin.android.java.KoinAndroidApplication;
import org.koin.core.KoinApplication;
import org.koin.core.context.GlobalContext;

import java.util.HashMap;

import timber.log.Timber;

import static org.koin.core.context.ContextFunctionsKt.startKoin;
import static org.koin.java.KoinJavaComponent.get;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraHttpSender(
        uri = "https://collector.tracepot.com/a2eda9d9",
        httpMethod = HttpSender.Method.POST
)
@AcraDialog(
        resTitle = R.string.acra_dialog_title,
        resText = R.string.acra_dialog_text,
        resTheme = R.style.Theme_Jellyfin
)
@AcraLimiter
public class TvApp extends Application {
    public static final String CREDENTIALS_PATH = "org.jellyfin.androidtv.login.json";

    public static final int LIVE_TV_GUIDE_OPTION_ID = 1000;
    public static final int LIVE_TV_RECORDINGS_OPTION_ID = 2000;
    public static final int VIDEO_QUEUE_OPTION_ID = 3000;
    public static final int LIVE_TV_SCHEDULE_OPTION_ID = 4000;
    public static final int LIVE_TV_SERIES_OPTION_ID = 5000;

    private PlaybackManager playbackManager;
    private static TvApp app;
    private UserDto currentUser;
    private BaseItemDto lastPlayedItem;
    private PlaybackController playbackController;

    private int autoBitrate;
    private String directItemId;

    private HashMap<String, DisplayPreferences> displayPrefsCache = new HashMap<>();

    public final DataRefreshService dataRefreshService = new DataRefreshService();

    private BaseActivity currentActivity;

    private LogonCredentials configuredAutoCredentials;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Start Koin
        KoinApplication koin = KoinAndroidApplication.create(this)
                .modules(
                        AppModuleKt.getAppModule(),
                        PreferenceModuleKt.getPreferenceModule()
                );
        startKoin(new GlobalContext(), koin);

        app = (TvApp) getApplicationContext();

        playbackManager = new PlaybackManager(AndroidDevice.fromContext(this), new AndroidLogger("PlaybackManager"));

        registerActivityLifecycleCallbacks(new AuthenticatedUserCallbacks());
        registerActivityLifecycleCallbacks(new AppThemeCallbacks());

        // Initialize the logging library
        Timber.plant(new Timber.DebugTree());
        Timber.i("Application object created");
    }

    public static TvApp getApplication() {
        return app;
    }

    public UserDto getCurrentUser() {
        if (currentUser == null) {
            Timber.e("Called getCurrentUser() but value was null.");
        }
        return currentUser;
    }

    public void setCurrentUser(UserDto currentUser) {
        this.currentUser = currentUser;
        TvManager.clearCache();
        this.displayPrefsCache = new HashMap<>();
    }

    /**
     * @deprecated This function is causing a **lot** of issues because not all activities will set their self as "currentactivity". Try to receive a Context instance instead.
     */
    @Deprecated()
    public BaseActivity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(BaseActivity activity) {
        currentActivity = activity;
    }

    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    public void setPlaybackController(PlaybackController playbackController) {
        this.playbackController = playbackController;
    }

    public LogonCredentials getConfiguredAutoCredentials() {
        return configuredAutoCredentials;
    }

    public void setConfiguredAutoCredentials(LogonCredentials configuredAutoCredentials) {
        this.configuredAutoCredentials = configuredAutoCredentials;
    }

    public boolean useExternalPlayer(BaseItemType itemType) {
        switch (itemType) {
            case Movie:
            case Episode:
            case Video:
            case Series:
            case Recording:
                return get(UserPreferences.class).get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.EXTERNAL;
            case TvChannel:
            case Program:
                return get(UserPreferences.class).get(UserPreferences.Companion.getLiveTvVideoPlayer()) == PreferredVideoPlayer.EXTERNAL;
            default:
                return false;
        }
    }

    public Class<? extends Activity> getPlaybackActivityClass(BaseItemType itemType) {
        return useExternalPlayer(itemType) ? ExternalPlayerActivity.class : PlaybackOverlayActivity.class;
    }

    /**
     * @deprecated Use `getUserPreferences().getResumePreroll()`
     */
    @Deprecated
    public int getResumePreroll() {
        try {
            return Integer.parseInt(get(UserPreferences.class).get(UserPreferences.Companion.getResumeSubtractDuration())) * 1000;
        } catch (Exception e) {
            Timber.e(e, "Unable to parse resume preroll");
            return 0;
        }
    }

    public boolean canManageRecordings() {
        return currentUser != null && currentUser.getPolicy().getEnableLiveTvManagement();
    }

    public PlaybackManager getPlaybackManager() {
        return playbackManager;
    }

    public Drawable getDrawableCompat(int id) {
//        if (Build.VERSION.SDK_INT >= 21) {
//            return getDrawable(id);
//        }

        return getResources().getDrawable(id);
    }

    public DisplayPreferences getCachedDisplayPrefs(String key) {
        return displayPrefsCache.containsKey(key) ? displayPrefsCache.get(key) : new DisplayPreferences();
    }

    public void updateDisplayPrefs(DisplayPreferences preferences) {
        updateDisplayPrefs("ATV", preferences);
    }

    public void updateDisplayPrefs(String app, DisplayPreferences preferences) {
        displayPrefsCache.put(preferences.getId(), preferences);
        get(ApiClient.class).UpdateDisplayPreferencesAsync(preferences, getCurrentUser().getId(), app, new EmptyResponse());
        Timber.d("Display prefs updated for %s isFavorite: %s", preferences.getId(), preferences.getCustomPrefs().get("FavoriteOnly"));
    }

    public void getDisplayPrefsAsync(String key, Response<DisplayPreferences> response) {
        getDisplayPrefsAsync(key, "ATV", response);
    }

    public void getDisplayPrefsAsync(final String key, String app, final Response<DisplayPreferences> outerResponse) {
        if (displayPrefsCache.containsKey(key)) {
            Timber.d("Display prefs loaded from cache %s", key);
            outerResponse.onResponse(displayPrefsCache.get(key));
        } else {
            get(ApiClient.class).GetDisplayPreferencesAsync(key, getCurrentUser().getId(), app, new Response<DisplayPreferences>(){
                @Override
                public void onResponse(DisplayPreferences response) {
                    if (response.getSortBy() == null) response.setSortBy("SortName");
                    if (response.getCustomPrefs() == null) response.setCustomPrefs(new HashMap<String, String>());
                    displayPrefsCache.put(key, response);
                    Timber.d("Display prefs loaded and saved in cache %s", key);
                    outerResponse.onResponse(response);
                }

                @Override
                public void onError(Exception exception) {
                    //Continue with defaults
                    Timber.e(exception, "Unable to load display prefs ");
                    DisplayPreferences prefs = new DisplayPreferences();
                    prefs.setId(key);
                    prefs.setSortBy("SortName");
                    prefs.setCustomPrefs(new HashMap<String, String>());
                    outerResponse.onResponse(prefs);
                }
            });
        }
    }


    public int getAutoBitrate() {
        return autoBitrate;
    }

    public void determineAutoBitrate() {
        get(ApiClient.class).detectBitrate(new Response<Long>() {
            @Override
            public void onResponse(Long response) {
                autoBitrate = response.intValue();
                Timber.i("Auto bitrate set to: %d", autoBitrate);
            }
        });
    }

    public String getDirectItemId() {
        return directItemId;
    }

    public void setDirectItemId(String directItemId) {
        this.directItemId = directItemId;
    }

    public BaseItemDto getLastPlayedItem() {
        return lastPlayedItem;
    }

    public void setLastPlayedItem(BaseItemDto lastPlayedItem) {
        this.lastPlayedItem = lastPlayedItem;
    }
}
