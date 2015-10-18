package tv.emby.embyatv;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.DisplayPreferences;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.registration.RegistrationInfo;
import mediabrowser.model.system.SystemInfo;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.playback.PlaybackController;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.startup.LogonCredentials;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.validation.AppValidator;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.sender.HttpSender;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;

/**
 * Created by Eric on 11/24/2014.
 */


@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "https://embi.smileupps.com/acra-androidtv/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "atvreporter",
        formUriBasicAuthPassword = "bumblebee+")

public class TvApp extends Application {

    public static String FEATURE_CODE = "androidtv";
    public static final int LIVE_TV_GUIDE_OPTION_ID = 1000;
    public static final int LIVE_TV_RECORDINGS_OPTION_ID = 2000;

    private ILogger logger;
    private IConnectionManager connectionManager;
    private PlaybackManager playbackManager;
    private GsonJsonSerializer serializer;
    private static TvApp app;
    private UserDto currentUser;
    private SystemInfo currentSystemInfo;
    private BaseItemDto currentPlayingItem;
    private PlaybackController playbackController;
    private ApiClient loginApiClient;
    private AudioManager audioManager;
    private VolleyHttpClient httpClient;

    private int autoBitrate;
    private String directItemId;
    private Typeface roboto;

    private HashMap<String, DisplayPreferences> displayPrefsCache = new HashMap<>();

    private boolean isConnectLogin = false;
    private String lastDeletedItemId = "";

    private boolean isPaid = false;
    private RegistrationInfo registrationInfo;

    private Calendar lastPlayback = Calendar.getInstance();
    private Calendar lastMoviePlayback = Calendar.getInstance();
    private Calendar lastTvPlayback = Calendar.getInstance();
    private Calendar lastLibraryChange = Calendar.getInstance();
    private long lastUserInteraction = System.currentTimeMillis();

    private boolean audioMuted;

    private BaseActivity currentActivity;

    private LogonCredentials configuredAutoCredentials;

    @Override
    public void onCreate() {
        super.onCreate();
        logger = new ConsoleLogger();
        app = (TvApp)getApplicationContext();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        logger.Info("Application object created");

        ACRA.init(this);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                if (!getApiClient().getServerInfo().getName().equals("Dev Server")) {
                    Utils.PutCustomAcraData();
                    ACRA.getErrorReporter().handleException(ex, false);
                } else {
                    Log.e("MediaBrowserTv", "Uncaught exception is: ", ex);
                    ex.printStackTrace();

                }
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
                      });

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
        return currentUser;
    }

    public void setCurrentUser(UserDto currentUser) {
        this.currentUser = currentUser;
    }

    public GsonJsonSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(GsonJsonSerializer serializer) {
        this.serializer = serializer;
    }

    public ApiClient getApiClient() {
        return connectionManager.GetApiClient(currentUser);
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

    public void setAudioMuted(boolean value) {
        audioMuted = value;
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, audioMuted);
    }

    public boolean isAudioMuted() { return audioMuted; }

    public PlaybackController getPlaybackController() {
        return playbackController;
    }

    public void setPlaybackController(PlaybackController playbackController) {
        this.playbackController = playbackController;
    }

    public SystemInfo getCurrentSystemInfo() { return currentSystemInfo; }

    public void loadSystemInfo() {
        if (getApiClient() != null) {
            getApiClient().GetSystemInfoAsync(new Response<SystemInfo>() {
                @Override
                public void onResponse(SystemInfo response) {
                    currentSystemInfo = response;
                    logger.Info("Current server is "+response.getServerName()+" (ver "+response.getVersion()+") running on "+response.getOperatingSystemDisplayName());
                }

                @Override
                public void onError(Exception exception) {
                    logger.ErrorException("Unable to obtain system info.",exception);
                }
            });
        }
    }

    public void showMessage(String title, String msg) {
        if (currentActivity != null) {
            currentActivity.showMessage(title, msg);
        }
    }

    public long getLastNagTime() { return getSystemPrefs().getLong("lastNagTime",0); }

    public void setLastNagTime(long time) { getSystemPrefs().edit().putLong("lastNagTime", System.currentTimeMillis()).commit(); }

    public LogonCredentials getConfiguredAutoCredentials() {
        return configuredAutoCredentials;
    }

    public void setConfiguredAutoCredentials(LogonCredentials configuredAutoCredentials) {
        this.configuredAutoCredentials = configuredAutoCredentials;
    }

    public SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    public SharedPreferences getSystemPrefs() {
        return getSharedPreferences("systemprefs", MODE_PRIVATE);
    }

    public String getConfigVersion() {
        return getSystemPrefs().getString("sys_pref_config_version", "2");
    }

    public boolean getIsAutoLoginConfigured() {
        return getPrefs().getString("pref_login_behavior", "0").equals("1");
    }

    public Calendar getLastMoviePlayback() {
        return lastMoviePlayback.after(lastPlayback) ? lastMoviePlayback : lastPlayback;
    }

    public void setLastMoviePlayback(Calendar lastMoviePlayback) {
        this.lastMoviePlayback = lastMoviePlayback;
        this.lastPlayback = lastMoviePlayback;
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

    public long getLastUserInteraction() {
        return lastUserInteraction;
    }

    public void setLastUserInteraction(long lastUserInteraction) {
        this.lastUserInteraction = lastUserInteraction;
    }

    public boolean checkPaidCache() {
        isPaid = getSystemPrefs().getString("kv","").equals(getApiClient().getDeviceId());
        logger.Info("Paid cache check: " + isPaid);
        return isPaid;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean isPaid) {
        this.isPaid = isPaid;
        getSystemPrefs().edit().putString("kv", isPaid ? getApiClient().getDeviceId() : "").commit();
    }

    public RegistrationInfo getRegistrationInfo() {
        return registrationInfo;
    }

    public void setRegistrationInfo(RegistrationInfo registrationInfo) {
        this.registrationInfo = registrationInfo;
    }

    public boolean isValid() {
        return isPaid || (registrationInfo != null && (registrationInfo.getIsRegistered() || registrationInfo.getIsTrial()));
    }

    public boolean isRegistered() {
        return registrationInfo != null && registrationInfo.getIsRegistered();
    }

    public boolean isTrial() {
        return registrationInfo != null && registrationInfo.getIsTrial() && !isPaid;
    }

    public void validate() {
        new AppValidator().validate();
    }

    public String getRegistrationString() {
        return isTrial() ? "In Trial. Expires " + DateUtils.getRelativeTimeSpanString(Utils.convertToLocalDate(registrationInfo.getExpirationDate()).getTime()).toString() :
                isValid() ? "Registered" : "Expired";
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

    public boolean isConnectLogin() {
        return isConnectLogin;
    }

    public void setConnectLogin(boolean isConnectLogin) {
        this.isConnectLogin = isConnectLogin;
    }

    public void stopPlayback() {
        if (playbackController != null && currentActivity != null && currentActivity instanceof PlaybackOverlayActivity) {
            currentActivity.finish();
        }
    }

    public void pausePlayback() {
        if (playbackController != null) {
            playbackController.playPause();
        }
    }
    public void unPausePlayback() {
        if (playbackController != null) {
            playbackController.playPause();
        }
    }

    public void playbackNext() {
        if (playbackController != null) {
            playbackController.next();
        }
    }

    public void playbackPrev() {
        if (playbackController != null) {
            playbackController.prev();
        }
    }

    public void playbackSeek(int pos) {
        if (playbackController != null) {
            playbackController.seek(pos);
        }
    }

    public void playbackJump() {
        if (playbackController != null) {
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
        displayPrefsCache.put(preferences.getId(), preferences);
        getApiClient().UpdateDisplayPreferencesAsync(preferences, getCurrentUser().getId(), "ATV", new EmptyResponse());
        logger.Debug("Display prefs updated isFavorite: "+preferences.getCustomPrefs().get("FavoriteOnly"));
    }

    public void getDisplayPrefsAsync(final String key, final Response<DisplayPreferences> outerResponse) {
        if (displayPrefsCache.containsKey(key)) {
            logger.Debug("Display prefs loaded from cache "+key);
            outerResponse.onResponse(displayPrefsCache.get(key));
        } else {
            getApiClient().GetDisplayPreferencesAsync(key, getCurrentUser().getId(), "ATV", new Response<DisplayPreferences>(){
                @Override
                public void onResponse(DisplayPreferences response) {
                    if (response.getSortBy() == null) response.setSortBy("SortName");
                    if (response.getCustomPrefs() == null) response.setCustomPrefs(new HashMap<String, String>());
                    displayPrefsCache.put(key, response);
                    logger.Debug("Display prefs loaded and saved in cache " + response.getPrimaryImageHeight());
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
                logger.Info("Auto bitrate set to: "+autoBitrate);
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
}
