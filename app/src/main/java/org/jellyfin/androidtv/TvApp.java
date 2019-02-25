package org.jellyfin.androidtv;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;

import org.jellyfin.androidtv.base.BaseActivity;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.model.DisplayPriorityType;
import org.jellyfin.androidtv.playback.ExternalPlayerActivity;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.playback.PlaybackController;
import org.jellyfin.androidtv.playback.PlaybackOverlayActivity;
import org.jellyfin.androidtv.search.SearchActivity;
import org.jellyfin.androidtv.startup.LogonCredentials;
import org.jellyfin.androidtv.util.Utils;

import java.util.Calendar;
import java.util.HashMap;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.configuration.ServerConfiguration;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.DisplayPreferences;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.net.EndPointInfo;
import mediabrowser.model.system.SystemInfo;

/**
 * Created by Eric on 11/24/2014.
 */


public class TvApp extends Application implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static String FEATURE_CODE = "androidtv";
    public static final int LIVE_TV_GUIDE_OPTION_ID = 1000;
    public static final int LIVE_TV_RECORDINGS_OPTION_ID = 2000;
    public static final int VIDEO_QUEUE_OPTION_ID = 3000;
    public static final int LIVE_TV_SCHEDULE_OPTION_ID = 4000;
    public static final int LIVE_TV_SERIES_OPTION_ID = 5000;

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

    private ServerConfiguration serverConfiguration;

    private int maxRemoteBitrate = -1;

    private Calendar lastPlayback = Calendar.getInstance();
    private Calendar lastMoviePlayback = Calendar.getInstance();
    private Calendar lastTvPlayback = Calendar.getInstance();
    private Calendar lastLibraryChange = Calendar.getInstance();
    private long lastVideoQueueChange = System.currentTimeMillis();
    private long lastFavoriteUpdate = System.currentTimeMillis();
    private long lastMusicPlayback = System.currentTimeMillis();
    private long lastUserInteraction = System.currentTimeMillis();

    private boolean searchAllowed = Build.VERSION.SDK_INT < 23;

    private GradientDrawable currentBackgroundGradient;

    private boolean audioMuted;
    private boolean playingIntros;
    private DisplayPriorityType displayPriority = DisplayPriorityType.Movies;

    private BaseActivity currentActivity;

    private LogonCredentials configuredAutoCredentials;

    @Override
    public void onCreate() {
        super.onCreate();
        logger = new ConsoleLogger();
        app = (TvApp)getApplicationContext();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        setCurrentBackgroundGradient(new int[] {ContextCompat.getColor(this, R.color.lb_default_brand_color_dark), ContextCompat.getColor(this, R.color.lb_default_brand_color)});

        logger.Info("Application object created");

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e("MediaBrowserTv", "Uncaught exception is: ", ex);
                ex.printStackTrace();
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
                                .setMessage("Please update your Jellyfin Server to avoid potential seeking problems during playback.")
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

            //Also get server configuration and fill in max remote bitrate if we are remote
            getApiClient().GetEndPointInfo(new Response<EndPointInfo>() {
                @Override
                public void onResponse(EndPointInfo response) {
                    if (!response.getIsInNetwork()) {
                        getApiClient().GetServerConfigurationAsync(new Response<ServerConfiguration>() {
                            @Override
                            public void onResponse(ServerConfiguration response) {
                                serverConfiguration = response;
                                maxRemoteBitrate = serverConfiguration.getRemoteClientBitrateLimit();
                                getLogger().Info("Server bitrate limit set to ", maxRemoteBitrate);
                            }

                            @Override
                            public void onError(Exception exception) {
                                getLogger().ErrorException("Unable to retrieve server configuration",exception);
                            }
                        });
                    } else {
                        getLogger().Info("** Local connection - no server bitrate limit");
                    }
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

    public boolean useExternalPlayer(String itemType) {
        switch (itemType) {
            case "Movie":
            case "Episode":
            case "Video":
            case "Series":
            case "Recording":
                return getPrefs().getBoolean("pref_video_use_external", false);
            case "TvChannel":
            case "Program":
                return getPrefs().getBoolean("pref_live_tv_use_external", false);
            default:
                return false;
        }
    }

    public Class getPlaybackActivityClass(String itemType) {
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

    public boolean directStreamLiveTv() { return getPrefs().getBoolean("pref_live_direct", true); }

    public void setDirectStreamLiveTv(boolean value) { getPrefs().edit().putBoolean("pref_live_direct", value).commit(); }

    public boolean useVlcForLiveTv() { return getPrefs().getBoolean("pref_enable_vlc_livetv", true); }

    public int getResumePreroll() {
        try {
            return Integer.parseInt(getPrefs().getString("pref_resume_preroll","0")) * 1000;
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

    public long getLastUserInteraction() {
        return lastUserInteraction;
    }

    public void setLastUserInteraction(long lastUserInteraction) {
        this.lastUserInteraction = lastUserInteraction;
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
        logger.Debug("Display prefs updated for "+preferences.getId()+" isFavorite: "+preferences.getCustomPrefs().get("FavoriteOnly"));
    }

    public void getDisplayPrefsAsync(String key, Response<DisplayPreferences> response) {
        getDisplayPrefsAsync(key, "ATV", response);
    }

    public void getDisplayPrefsAsync(final String key, String app, final Response<DisplayPreferences> outerResponse) {
        if (displayPrefsCache.containsKey(key)) {
            logger.Debug("Display prefs loaded from cache "+key);
            outerResponse.onResponse(displayPrefsCache.get(key));
        } else {
            getApiClient().GetDisplayPreferencesAsync(key, getCurrentUser().getId(), app, new Response<DisplayPreferences>(){
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
