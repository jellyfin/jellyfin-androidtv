package tv.emby.embyatv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackController;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.search.SearchActivity;
import tv.emby.embyatv.startup.LogonCredentials;
import tv.emby.embyatv.util.LogReporter;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.validation.AppValidator;

import java.util.Calendar;
import java.util.HashMap;
import android.Manifest;

/**
 * Created by Eric on 11/24/2014.
 */


public class TvApp extends Application implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static String FEATURE_CODE = "androidtv";
    public static final int LIVE_TV_GUIDE_OPTION_ID = 1000;
    public static final int LIVE_TV_RECORDINGS_OPTION_ID = 2000;
    public static final int VIDEO_QUEUE_OPTION_ID = 3000;

    private static final int SEARCH_PERMISSION = 0;

    private ILogger logger;
    private IConnectionManager connectionManager;
    private PlaybackManager playbackManager;
    private GsonJsonSerializer serializer;
    private static TvApp app;
    private UserDto currentUser;
    private SystemInfo currentSystemInfo;
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

    private boolean isPaid = false;
    private RegistrationInfo registrationInfo;

    private Calendar lastPlayback = Calendar.getInstance();
    private Calendar lastMoviePlayback = Calendar.getInstance();
    private Calendar lastTvPlayback = Calendar.getInstance();
    private Calendar lastLibraryChange = Calendar.getInstance();
    private long lastVideoQueueChange = System.currentTimeMillis();
    private long lastFavoriteUpdate = System.currentTimeMillis();
    private long lastMusicPlayback = System.currentTimeMillis();
    private long lastUserInteraction = System.currentTimeMillis();

    private boolean searchAllowed = Build.VERSION.SDK_INT < 23;

    private boolean audioMuted;
    private boolean playingIntros;

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

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                if (!getApiClient().getServerInfo().getName().equals("Dev Server")) {
                    ex.printStackTrace();
                    new LogReporter().sendReport("Exception", new EmptyResponse() {
                        @Override
                        public void onResponse() {

                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(10);
                        }
                    });
                } else {
                    Log.e("MediaBrowserTv", "Uncaught exception is: ", ex);
                    ex.printStackTrace();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);

                }
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

    public void setAudioMuted(boolean value) {
        audioMuted = value;
        getLogger().Info("Setting mute state to: "+audioMuted);
        if (Utils.is60()) {
            audioManager.adjustVolume(audioMuted ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);

        } else {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, audioMuted);
        }
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
                    logger.Info("Current server is " + response.getServerName() + " (ver " + response.getVersion() + ") running on " + response.getOperatingSystemDisplayName());
                    //Server compat warning
                    if (getCurrentActivity() != null && !Utils.versionGreaterThanOrEqual(currentSystemInfo.getVersion(), "3.0.5882.0")) {
                        new AlertDialog.Builder(getCurrentActivity())
                                .setTitle("Incompatible Server Version")
                                .setMessage("Please update your Emby Server to avoid potential seeking problems during playback.")
                                .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .setCancelable(false)
                                .show();
                    }

                }

                @Override
                public void onError(Exception exception) {
                    logger.ErrorException("Unable to obtain system info.", exception);
                }
            });
        }
    }

    public void showSearch(final Activity activity, boolean musicOnly) {
        if (!searchAllowed && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            //request necessary permission
            logger.Info("Requesting search permission...");
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)) {
                //show explanation
                logger.Info("Show rationale for permission");
                new AlertDialog.Builder(activity)
                        .setTitle("Search Permission")
                        .setMessage("Search requires permission to record audio in order to use the microphone for voice search")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.RECORD_AUDIO}, SEARCH_PERMISSION);
                            }
                        }).show();
            } else {
                ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.RECORD_AUDIO}, SEARCH_PERMISSION);
            }
        } else {
            showSearchInternal(activity, musicOnly);
        }
    }

    private void showSearchInternal(Context activity, boolean musicOnly) {
        Intent intent = new Intent(activity, SearchActivity.class);
        if (musicOnly) intent.putExtra("MusicOnly", true);
        activity.startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case SEARCH_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    searchAllowed = true;
                    showSearchInternal(getCurrentActivity(), false);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Utils.showToast(this, "Search not allowed");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
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
    private long getLastNagTime() { return getSystemPrefs().getLong("lastNagTime",0); }

    private void setLastNagTime(long time) { getSystemPrefs().edit().putLong("lastNagTime", System.currentTimeMillis()).commit(); }

    public void premiereNag() {
        if (!isRegistered() && System.currentTimeMillis() - (86400000 * 7) > getLastNagTime()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentActivity != null && !currentActivity.isFinishing()) {
                        currentActivity.showMessage(getString(R.string.msg_premiere_nag_title), getString(R.string.msg_premiere_nag_msg), 10000);
                        setLastNagTime(System.currentTimeMillis());
                    }

                }
            },2500);
        }
    }

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
        return getPrefs().getString("pref_login_behavior", "0").equals("1") && getConfiguredAutoCredentials().getServerInfo().getId() != null;
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

    public boolean directStreamLiveTv() { return getPrefs().getBoolean("pref_live_direct", true); }

    public void setDirectStreamLiveTv(boolean value) { getPrefs().edit().putBoolean("pref_live_direct", value).commit(); }

    public boolean useVlcForLiveTv() { return getPrefs().getBoolean("pref_enable_vlc_livetv", true); }

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
        return getSystemPrefs().getBoolean("sys_pref_connect_login", false);
    }

    public void setConnectLogin(boolean value) {
        TvApp.getApplication().getSystemPrefs().edit().putBoolean("sys_pref_connect_login", value).commit();
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
        displayPrefsCache.put(preferences.getId(), preferences);
        getApiClient().UpdateDisplayPreferencesAsync(preferences, getCurrentUser().getId(), "ATV", new EmptyResponse());
        logger.Debug("Display prefs updated for "+preferences.getId()+" isFavorite: "+preferences.getCustomPrefs().get("FavoriteOnly"));
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
                    logger.Debug("Display prefs loaded and saved in cache " + key);
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

    public boolean isSearchAllowed() {
        return searchAllowed;
    }

    public void setSearchAllowed(boolean searchAllowed) {
        this.searchAllowed = searchAllowed;
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
}
