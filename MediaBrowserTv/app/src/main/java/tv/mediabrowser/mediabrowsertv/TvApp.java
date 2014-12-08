package tv.mediabrowser.mediabrowsertv;

import android.app.Application;
import android.util.Log;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;

/**
 * Created by Eric on 11/24/2014.
 */
public class TvApp extends Application {

    private ILogger logger;
    private IConnectionManager connectionManager;
    private GsonJsonSerializer serializer;
    private static TvApp app;
    private UserDto currentUser;
    private BaseItemDto currentPlayingItem;

    @Override
    public void onCreate() {
        super.onCreate();
        logger = new ConsoleLogger();
        app = (TvApp)getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e("MediaBrowserTv", "Uncaught exception is: ", ex);
                ex.printStackTrace();
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

    public void setCurrentPlayingItem(BaseItemDto currentPlayingItem) {
        this.currentPlayingItem = currentPlayingItem;
    }
}
