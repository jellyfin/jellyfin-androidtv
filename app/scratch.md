.Activity
    .LoginViewModel
        state: LOADING, ERROR(err)
        servers(id, name, users(id, name, photo))
    .ServerRepository - Deals with server lists
        PRIV getDiscoveryServers()
        PRIV getStoredServers() (via authenticationrepo)
        PRIV getLegacyServer()
        PRIV getPublicUsersForServer(server)
        getServers(discovery = true, stored = true, legacy = true)
        getServersWithUsers(discovery = true, stored = true, legacy = true)
        removeServer(id)
        addServer(url)
    .AuthenticationRepository - Deals with authentication
        legacy credentials
        stored servers + users
