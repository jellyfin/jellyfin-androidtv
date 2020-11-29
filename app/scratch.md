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

ISSUES
- Adding a server does not refresh the server list (need to restart activity)
- Profile pictures not showing
- Profile pictures not cached
- Add old credential migration and remove as sources in ServersRepo
- Use consistent naming for:
  - user/account
  - login/authenticate
  etc
