package org.jellyfin.androidtv.model;

import org.jellyfin.apiclient.model.apiclient.ServerInfo;
import org.jellyfin.apiclient.model.dto.UserDto;

public class LogonCredentials {
    private UserDto userDto;

    public LogonCredentials(ServerInfo server, UserDto user) {
        userDto = user;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
