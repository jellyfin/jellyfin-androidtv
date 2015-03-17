package tv.emby.embyatv.startup;

import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.UserDto;

/**
 * Created by Eric on 1/20/2015.
 */
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
