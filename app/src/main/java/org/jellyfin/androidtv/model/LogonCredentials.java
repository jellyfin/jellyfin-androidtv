package org.jellyfin.androidtv.model;

import org.jellyfin.apiclient.model.apiclient.ServerInfo;
import org.jellyfin.apiclient.model.dto.UserDto;

public class LogonCredentials {
    private ServerInfo serverInfo;
    private UserDto userDto;

    public LogonCredentials(ServerInfo server, UserDto user) {
        serverInfo = server;
        userDto = user;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }
}
