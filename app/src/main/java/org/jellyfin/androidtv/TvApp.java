package org.jellyfin.androidtv;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;

import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraHttpSender;
import org.acra.annotation.AcraLimiter;
import org.acra.sender.HttpSender;
import org.jellyfin.androidtv.base.AuthenticatedUserCallbacks;
import org.jellyfin.androidtv.base.BaseActivity;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.model.DisplayPriorityType;
import org.jellyfin.androidtv.model.LogonCredentials;
import org.jellyfin.androidtv.playback.ExternalPlayerActivity;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.playback.PlaybackManager;
import org.jellyfin.androidtv.playback.PlaybackOverlayActivity;
import org.jellyfin.androidtv.preferences.SystemPreferences;
import org.jellyfin.androidtv.preferences.UserPreferences;
import org.jellyfin.androidtv.preferences.enums.LoginBehavior;
import org.jellyfin.androidtv.preferences.enums.PreferredVideoPlayer;
import org.jellyfin.androidtv.search.SearchActivity;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.IConnectionManager;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.interaction.VolleyHttpClient;
import org.jellyfin.apiclient.logging.AndroidLogger;
import org.jellyfin.apiclient.model.configuration.ServerConfiguration;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.entities.DisplayPreferences;
import org.jellyfin.apiclient.model.logging.ILogger;
import org.jellyfin.apiclient.model.serialization.GsonJsonSerializer;

import java.util.Calendar;
import java.util.HashMap;

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
    // The minimum supported server version. Trying to connect to an older server will display an error.
    public static final String MINIMUM_SERVER_VERSION = "10.3.0";
    public static final String CREDENTIALS_PATH = "org.jellyfin.androidtv.login.json";

    public static final int LIVE_TV_GUIDE_OPTION_ID = 1000;
    public static final int LIVE_TV_RECORDINGS_OPTION_ID = 2000;
    public static final int VIDEO_QUEUE_OPTION_ID = 3000;
    public static final int LIVE_TV_SCHEDULE_OPTION_ID = 4000;
    public static final int LIVE_TV_SERIES_OPTION_ID = 5000;

    private static final String TAG = "Jellyfin-AndroidTV";

    private ILogger logger;
    private IConnectionManager connectionManager;
    private PlaybackManager playbackManager;
    private GsonJsonSerializer serializer;
    private static TvApp app;
    private UserDto currentUser;
    private BaseItemDto currentPlayingItem;
    private BaseItemDto lastPlayedItem;
    private PlaybackController playbackController;
    private ApiClient loginApiClient;
    private AudioManager audioManager;
    private VolleyHttpClient httpClient;

    private int autoBitrate;
    private String directItemId;
    private Typeface roboto;

    private HashMap<String, DisplayPreferences> displayPrefsCache = new HashMap<>();

    private String lastDeletedItemId = "";

    private ServerConfiguration serverConfiguration;

    private int maxRemoteBitrate = -1;

    private Calendar lastPlayback = Calendar.getInstance();
    private Calendar lastMoviePlayback = Calendar.getInstance();
    private Calendar lastTvPlayback = Calendar.getInstance();
    private Calendar lastLibraryChange = Calendar.getInstance();
    private long lastVideoQueueChange = System.currentTimeMillis();
    private long lastFavoriteUpdate = System.currentTimeMillis();
    private long lastMusicPlayback = System.currentTimeMillis();

    private GradientDrawable currentBackgroundGradient;

    private boolean playingIntros;
    private DisplayPriorityType displayPriority = DisplayPriorityType.Movies;

    private BaseActivity currentActivity;

    private LogonCredentials configuredAutoCredentials;
    private UserPreferences userPreferences;
    private SystemPreferences systemPreferences;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logger = new AndroidLogger(TAG);
        app = (TvApp) getApplicationContext();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        setCurrentBackgroundGradient(new int[] {ContextCompat.getColor(this, R.color.lb_default_brand_color_dark), ContextCompat.getColor(this, R.color.lb_default_brand_color)});

        registerActivityLifecycleCallbacks(new AuthenticatedUserCallbacks());

        logger.Info("Application object created");
    }

    public static TvApp getApplication() {
        return app;
    }

    public ILogger getLogger() {
        return logger;
    }

    public void setLogger(ILogger value) {
        logger = value;
    }

    public Typeface getDefaultFont() { return roboto; }

    public IConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(IConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public UserDto getCurrentUser() {
        if (currentUser == null)
            logger.Error("Called getCurrentUser() but value was null.");

        return currentUser;
    }

    public void setCurrentUser(UserDto currentUser) {
        this.currentUser = currentUser;
        TvManager.clearCache();
        this.displayPrefsCache = new HashMap<>();
    }

    public GsonJsonSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(GsonJsonSerializer serializer) {
        this.serializer = serializer;
    }

    public ApiClient getApiClient() {
        return currentUser != null ? connectionManager.GetApiClient(currentUser) : null;
    }

    public BaseItemDto getCurrentPlayingItem() {
        return currentPlayingItem;
    }

    public BaseActivity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(BaseActivity activity) {
        currentActivity = activity;
    }

    public void setCurrentPlayingItem(BaseItemDto currentPlayingItem) {
        this.currentPlayingItem = currentPlayingItem;
    }

    public ApiClient getLoginApiClient() {
        return loginApiClient;
    }

    public void setLoginApiClient(ApiClient loginApiClient) {
        this.loginApiClient = loginApiClient;
    }

    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    public void setPlaybackController(PlaybackController playbackController) {
        this.playbackController = playbackController;
    }

    public void showSearch(final Activity activity, boolean musicOnly) {
        Intent intent = new Intent(activity, SearchActivity.class);
        intent.putExtra("MusicOnly", musicOnly);

        activity.startActivity(intent);
    }

    public void showMessage(String title, String msg) {
        if (currentActivity != null) {
            currentActivity.showMessage(title, msg);
        }
    }

    public void showMessage(String title, String msg, int timeout, int iconResource) {
        if (currentActivity != null) {
            currentActivity.showMessage(title, msg, timeout, iconResource, null);
        }
    }

    public LogonCredentials getConfiguredAutoCredentials() {
        return configuredAutoCredentials;
    }

    public void setConfiguredAutoCredentials(LogonCredentials configuredAutoCredentials) {
        this.configuredAutoCredentials = configuredAutoCredentials;
    }

    public UserPreferences getUserPreferences() {
        if (this.userPreferences == null) this.userPreferences = new UserPreferences(this);
        return this.userPreferences;
    }

    public SystemPreferences getSystemPreferences() {
        if (this.systemPreferences == null) this.systemPreferences = new SystemPreferences(this);
        return this.systemPreferences;
    }

    public boolean getIsAutoLoginConfigured() {
        return getUserPreferences().getLoginBehavior() == LoginBehavior.AUTO_LOGIN && getConfiguredAutoCredentials().getServerInfo().getId() != null;
    }

    public boolean useExternalPlayer(BaseItemType itemType) {
        switch (itemType) {
            case Movie:
            case Episode:
            case Video:
            case Series:
            case Recording:
                return getUserPreferences().getVideoPlayer() == PreferredVideoPlayer.EXTERNAL;
            case TvChannel:
            case Program:
                return getUserPreferences().getLiveTvVideoPlayer() == PreferredVideoPlayer.EXTERNAL;
            default:
                return false;
        }
    }

    public Class<? extends Activity> getPlaybackActivityClass(BaseItemType itemType) {
        return useExternalPlayer(itemType) ? ExternalPlayerActivity.class : PlaybackOverlayActivity.class;
    }

    public Calendar getLastMoviePlayback() {
        return lastMoviePlayback.after(lastPlayback) ? lastMoviePlayback : lastPlayback;
    }

    public void setLastMoviePlayback(Calendar lastMoviePlayback) {
        this.lastMoviePlayback = lastMoviePlayback;
        this.lastPlayback = lastMoviePlayback;
    }

    public void setLastFavoriteUpdate(long time) { lastFavoriteUpdate = time; }
    public long getLastFavoriteUpdate() { return lastFavoriteUpdate; }

    public void setLastMusicPlayback(long time) { lastMusicPlayback = time; }
    public long getLastMusicPlayback() { return lastMusicPlayback; }

    /**
     * @deprecated Use `getUserPreferences().getResumePreroll()`
     */
    @Deprecated
    public int getResumePreroll() {
        try {
            return Integer.parseInt(getUserPreferences().getResumeSubtractDuration()) * 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    public Calendar getLastTvPlayback() {
        return lastTvPlayback.after(lastPlayback) ? lastTvPlayback : lastPlayback;
    }

    public void setLastTvPlayback(Calendar lastTvPlayback) {
        this.lastTvPlayback = lastTvPlayback;
        this.lastPlayback = lastTvPlayback;
    }

    public Calendar getLastLibraryChange() {
        return lastLibraryChange;
    }

    public void setLastLibraryChange(Calendar lastLibraryChange) {
        this.lastLibraryChange = lastLibraryChange;
    }

    public Calendar getLastPlayback() {
        return lastPlayback;
    }

    public void setLastPlayback(Calendar lastPlayback) {
        this.lastPlayback = lastPlayback;
    }

    public boolean canManageRecordings() {
        return currentUser != null && currentUser.getPolicy().getEnableLiveTvManagement();
    }

    public PlaybackManager getPlaybackManager() {
        return playbackManager;
    }

    public void setPlaybackManager(PlaybackManager playbackManager) {
        this.playbackManager = playbackManager;
    }

    public Drawable getDrawableCompat(int id) {
//        if (Build.VERSION.SDK_INT >= 21) {
//            return getDrawable(id);
//        }

        return getResources().getDrawable(id);
    }

    public boolean isPlayingVideo() {
        return playbackController != null && currentActivity != null && currentActivity instanceof PlaybackOverlayActivity;
    }

    public void stopPlayback() {
        if (isPlayingVideo()) {
            currentActivity.finish();
        } else if (MediaManager.isPlayingAudio()) {
            MediaManager.stopAudio();
        }
    }

    public void pausePlayback() {
        if (MediaManager.isPlayingAudio()) {
            MediaManager.pauseAudio();
        } else if (isPlayingVideo()) {
            playbackController.playPause();
        }
    }
    public void unPausePlayback() {
        if (isPlayingVideo()) {
            playbackController.playPause();
        } else if (MediaManager.hasAudioQueueItems()) {
            MediaManager.resumeAudio();
        }
    }

    public void playbackNext() {
        if (isPlayingVideo()) {
            playbackController.next();
        } else if (MediaManager.hasAudioQueueItems()) {
            MediaManager.nextAudioItem();
        }
    }

    public void playbackPrev() {
        if (isPlayingVideo()) {
            playbackController.prev();
        } else if (MediaManager.hasAudioQueueItems()) {
            MediaManager.prevAudioItem();
        }
    }

    public void playbackSeek(int pos) {
        if (isPlayingVideo()) {
            playbackController.seek(pos);
        }
    }

    public void playbackJump() {
        if (isPlayingVideo()) {
            playbackController.skip(30000);
        }
    }

    public void playbackJumpBack() {
        if (playbackController != null) {
            playbackController.skip(-11000);
        }
    }

    public DisplayPreferences getCachedDisplayPrefs(String key) {
        return displayPrefsCache.containsKey(key) ? displayPrefsCache.get(key) : new DisplayPreferences();
    }

    public void updateDisplayPrefs(DisplayPreferences preferences) {
        updateDisplayPrefs("ATV", preferences);
    }

    public void updateDisplayPrefs(String app, DisplayPreferences preferences) {
        displayPrefsCache.put(preferences.getId(), preferences);
        getApiClient().UpdateDisplayPreferencesAsync(preferences, getCurrentUser().getId(), app, new EmptyResponse());
        logger.Debug("Display prefs updated for %s isFavorite: %s", preferences.getId(), preferences.getCustomPrefs().get("FavoriteOnly"));
    }

    public void getDisplayPrefsAsync(String key, Response<DisplayPreferences> response) {
        getDisplayPrefsAsync(key, "ATV", response);
    }

    public void getDisplayPrefsAsync(final String key, String app, final Response<DisplayPreferences> outerResponse) {
        if (displayPrefsCache.containsKey(key)) {
            logger.Debug("Display prefs loaded from cache %s", key);
            outerResponse.onResponse(displayPrefsCache.get(key));
        } else {
            getApiClient().GetDisplayPreferencesAsync(key, getCurrentUser().getId(), app, new Response<DisplayPreferences>(){
                @Override
                public void onResponse(DisplayPreferences response) {
                    if (response.getSortBy() == null) response.setSortBy("SortName");
                    if (response.getCustomPrefs() == null) response.setCustomPrefs(new HashMap<String, String>());
                    displayPrefsCache.put(key, response);
                    logger.Debug("Display prefs loaded and saved in cache %s", key);
                    outerResponse.onResponse(response);
                }

                @Override
                public void onError(Exception exception) {
                    //Continue with defaults
                    logger.ErrorException("Unable to load display prefs ", exception);
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
        if (getApiClient() == null) return;
        getApiClient().detectBitrate(new Response<Long>() {
            @Override
            public void onResponse(Long response) {
                autoBitrate = response.intValue();
                logger.Info("Auto bitrate set to: %d", autoBitrate);
            }
        });
    }


    public String getDirectItemId() {
        return directItemId;
    }

    public void setDirectItemId(String directItemId) {
        this.directItemId = directItemId;
    }

    public String getLastDeletedItemId() {
        return lastDeletedItemId;
    }

    public void setLastDeletedItemId(String lastDeletedItemId) {
        this.lastDeletedItemId = lastDeletedItemId;
    }

    public VolleyHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(VolleyHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public BaseItemDto getLastPlayedItem() {
        return lastPlayedItem;
    }

    public void setLastPlayedItem(BaseItemDto lastPlayedItem) {
        this.lastPlayedItem = lastPlayedItem;
    }

    public long getLastVideoQueueChange() {
        return lastVideoQueueChange;
    }

    public void setLastVideoQueueChange(long lastVideoQueueChange) {
        this.lastVideoQueueChange = lastVideoQueueChange;
    }

    public boolean isPlayingIntros() {
        return playingIntros;
    }

    public void setPlayingIntros(boolean playingIntros) {
        this.playingIntros = playingIntros;
    }

    public ServerConfiguration getServerConfiguration() {
        return serverConfiguration;
    }

    public int getServerBitrateLimit() { return maxRemoteBitrate > 0 ? maxRemoteBitrate : 100000000; }

    public DisplayPriorityType getDisplayPriority() {
        return displayPriority;
    }

    public void setDisplayPriority(DisplayPriorityType displayPriority) {
        this.displayPriority = displayPriority;
    }

    public GradientDrawable getCurrentBackgroundGradient() {
        return currentBackgroundGradient;
    }

    public void setCurrentBackground(Bitmap currentBackground) {
        int[] colors = new int[2];
        colors[0] = Utils.darker(Palette.from(currentBackground).generate().getMutedColor(ContextCompat.getColor(this, R.color.black_transparent)), .6f);
        colors[1] = Utils.darker(colors[0], .1f);
        setCurrentBackgroundGradient(colors);
    }

    private void setCurrentBackgroundGradient(int[] colors) {
        currentBackgroundGradient = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, colors);
        currentBackgroundGradient.setCornerRadius(0f);
        currentBackgroundGradient.setGradientCenter(.6f, .5f);
        currentBackgroundGradient.setAlpha(200);
    }
}
