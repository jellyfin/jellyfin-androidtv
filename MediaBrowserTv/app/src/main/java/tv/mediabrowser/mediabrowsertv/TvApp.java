package tv.mediabrowser.mediabrowsertv;

import android.app.Application;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Eric on 11/24/2014.
 */
public class TvApp extends Application {

    private ILogger logger;
    private IConnectionManager connectionManager;
    private static TvApp app;
    private UserDto currentUser;

    @Override
    public void onCreate() {
        super.onCreate();
        logger = new ConsoleLogger();
        app = (TvApp)getApplicationContext();
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
}
