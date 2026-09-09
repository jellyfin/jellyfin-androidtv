# SyncPlay on Android TV

This implementation addresses [jellyfin/jellyfin-androidtv#538](https://github.com/jellyfin/jellyfin-androidtv/issues/538).
It uses Jellyfin's existing SyncPlay groups and server protocol, so the TV can watch with Jellyfin Web and other compatible clients.

## Using it

1. Open **SyncPlay** from the home toolbar or the integrated video player's controls.
2. Create a named group, or select an existing group to join.
3. Select a title. Group playback opens the integrated TV player automatically.
4. Play, pause, seek, and next/previous item controls are sent to the group.
5. Use **Leave group** to return to independent playback. Leaving the player or putting the app in the background also leaves the group,
   without sending a group-wide pause or stop.

The account needs permission to use SyncPlay and access to the group's media. External video players are not used for group playback.
The dialog shows participants, loading failures, and a **Correct playback drift** option. Ongoing speed/seek correction is off by default,
matching Jellyfin Web; scheduled group commands and clock synchronization always run.

## Implementation

- `SyncPlayService` consumes ordered WebSocket group updates and commands using Jellyfin Kotlin SDK models and API calls.
- SDK 1.8.12 stores raw socket frames in a conflating `StateFlow`, which can drop `GroupJoined` when `Stop` immediately follows.
  `ReliableApiClient` retains SDK HTTP behavior and replaces the session's socket with a bounded ordered transport using the SDK's
  decoder and authentication format. Overflow or a lost connection explicitly disconnects the group. All app subscribers share this
  one connection: a separate SyncPlay socket is insufficient because the server sends session events only to the most recently active
  connection, allowing the regular socket's keepalive to take over delivery.
- Group queue entries are prepared at the server's requested position without autoplay. The TV reports `Ready` with the **playlist entry ID**,
  then waits for the server's scheduled command. Library item IDs are used to fetch media, not to acknowledge playback.
- User commands are intercepted before changing the local backend. Server commands go directly to the backend and never echo as new group requests.
- Audio queue additions go to the shared playlist. Repeat and shuffle controls display the server's modes while local automatic queue
  advancement remains disabled, so either mode can be turned on and off without overriding the group.
- Server timestamps are translated using a clock estimate anchored to monotonic time. The estimate uses the best of eight latency samples.
- A continuous three-second buffering interval is reported to the group. Becoming ready clears that wait.
- Queue changes invalidate pending loads. Leaving, disconnecting, or switching accounts cancels scheduled playback and restores normal speed.
- The new player is used for grouped video. This branch targets upstream `master`, where the playback rewrite lives; it is a development build.
- Navigation follows the group's selected queue entry and media type. It does not open an old local audio item while joining or create another
  player screen for each queue update. A group transition from video to audio preserves membership.
- Closing a video screen or backgrounding the app resets local group playback before the best-effort leave request. This teardown survives
  fragment destruction and cannot send a group-wide pause/stop or later stop playback started after returning to the app.
- Session changes are serialized. A suspendable teardown hook runs before outgoing credentials are replaced or cleared, including current-user
  signout. Playback services are resolved on the main thread even when session maintenance runs in the background.

The SDK version pinned by this checkout exposes `timeSyncApi.getUtcTime()` and `userLibraryApi.getItem()`. Consult that version's source JAR
when changing the integration: the SDK's development branch has moved these calls to other API classes, so its current generated source
does not necessarily match the published dependency.

Protocol behavior and drift defaults are based on Jellyfin Web commit `b24b70d18dccf71a58633cd9795a2663983f93c0`, especially
[`PlaybackCore.js`](https://github.com/jellyfin/jellyfin-web/blob/b24b70d18dccf71a58633cd9795a2663983f93c0/src/plugins/syncPlay/core/PlaybackCore.js)
and [`TimeSync.js`](https://github.com/jellyfin/jellyfin-web/blob/b24b70d18dccf71a58633cd9795a2663983f93c0/src/plugins/syncPlay/core/timeSync/TimeSync.js).

## Building and verification

Use JDK 21, the Android SDK, and the repository's Gradle wrapper. The current checkout uses SDK platform `android-37.0` and also needs
build tools `36.0.0`. These versioned SDK package names differ from older Android SDK layouts.

```sh
./gradlew test
./gradlew detekt lint
./gradlew assembleDebug
```

The debug APK uses `org.jellyfin.androidtv.debug` and can be installed alongside the Google Play version. Building this branch does not
update the app published on Google Play.

### Verification performed

- The full Gradle `test` run passed 140 tests with no failures, errors, or skips.
- Detekt and Android lint reports were inspected because this repository configures both to allow findings without failing the build.
  Existing findings remain on untouched lines; none were reported in the new SyncPlay or network code or on changed lines.
- Runtime checks use an Android TV API 36 emulator, an isolated Jellyfin 12 server, the official Jellyfin Web app in Chromium, and a
  generated 90-second H.264/AAC test video. The Web client actually decodes media; it is not a protocol-only test double.
- Joining from TV opened the group's video paused at the same position as Web. Web play/pause advanced both to 30.418 seconds;
  Web seek moved the decoded TV frame to 45.000 seconds. TV-origin media controls resumed actual playback, with TV position samples
  advancing from 45.000 to 48.010 seconds before pausing at 50.371 seconds. TV fast-forward subsequently moved Web to 80.371 seconds.
- Back from the TV player returned home and removed only the TV participant; Web remained in its original group.
- Creating a named group from the TV and leaving it through the dialog both succeeded. Web retained its loaded player and working
  controls after the TV left the earlier shared group.
- A final D-pad-only play/pause sequence advanced both clients from 19.997 seconds to a shared paused position of 24.619 seconds.
  Reopening the controls after the hide timer succeeded. The player requests focus when controls appear and returns focus to the
  hidden overlay when they disappear; a disappearing button must not request focus again. The key handler precedes the focus target,
  and D-pad keys are explicitly accepted even when Android classifies the event as a system key.
- The player SyncPlay dialog retained D-pad focus after the underlying controls' hide timer, with both participants displayed.

Focused tests cover clock correction, drift thresholds, scheduled commands without feedback, readiness and buffering, join failures,
stale commands after leaving, stale queue loads after replacement or stop, account teardown, and shared audio queue controls.
Network tests replay serialized message bursts, exercise queue overflow and keepalive expiry, verify shared subscriptions and reconnects,
and reject buffered events from an earlier login even when the same credentials are reused.

When testing constructor-created player listeners with MockK, stub the actual service instance. `spyk(service)` copies the instance,
while a listener created in its constructor still captures the original; callbacks then inspect different membership state and can
produce misleading readiness failures. The service tests use the original instance and stub only inherited player dependencies.

Before a production release, test on the intended TV hardware with a second client: create/join, initial and mid-playback join, pause,
seek in both directions, next/previous, a buffering participant, leave, background/return, account changes, and lost/recovered connectivity.
Include the codecs and audio output used on that device; an emulator cannot establish hardware decoding or audio passthrough behavior.

The TV dialog should remain open while the underlying player controls hide. Check D-pad focus, the group-name keyboard and Done action,
Back/Close, refresh, participant updates, joining/leaving, and the correction checkbox.
