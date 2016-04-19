package tv.emby.embyatv.util;

import tv.emby.embyatv.BuildConfig;

/**
 * Created by Eric on 2/28/2016.
 */
public class LogReport {
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getOsVersionInt() {
        return osVersionInt;
    }

    public void setOsVersionInt(int osVersionInt) {
        this.osVersionInt = osVersionInt;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getLogLines() {
        return logLines;
    }

    public void setLogLines(String logLines) {
        this.logLines = logLines;
    }

    private String appName = "AndroidTV";
    private String appVersion = BuildConfig.VERSION_NAME;
    private String serverName;
    private String userName;
    private String cause;
    private int osVersionInt;
    private String osVersionString;
    private String deviceInfo;
    private String logLines;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getOsVersionString() {
        return osVersionString;
    }

    public void setOsVersionString(String osVersionString) {
        this.osVersionString = osVersionString;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
