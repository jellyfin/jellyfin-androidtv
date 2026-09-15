# SyncPlay emulator test plan

Updated 2026-09-15 for branch `dev/syncplay`.

## Purpose

Validate the Android TV SyncPlay implementation against a real Jellyfin server, a real media pipeline, Android lifecycle behavior, and another Jellyfin SyncPlay client. Unit tests remain responsible for deterministic clock, scheduler, session-generation, and transport behavior. These device tests target failures that fakes cannot expose: WebSocket delivery, generated SDK serialization, playback preparation, Media3 timing, TV focus, buffering, process lifecycle, and interoperability.

## Required environment

- One ARM64 Android TV AVD on this Apple Silicon Mac, preferably a stable Google TV or Android TV image at API 35 or 36. The application supports API 23+, but the first emulator pass should use a current image.
- Android Emulator 36.5 or newer so multiple AVDs can share the emulator Wi-Fi network when a second AVD is useful.
- A test Jellyfin server reachable from the emulator. For a server bound to the Mac's loopback interface, use `http://10.0.2.2:<port>` in the emulator. A LAN or HTTPS URL is preferable when testing reconnects and certificate behavior.
- Two non-administrator Jellyfin test users, both allowed to play the test library and use SyncPlay. Keep one administrator available for access-policy tests.
- A second client signed in as the second user. Jellyfin Web in a desktop browser is the primary interoperability oracle. A second Android TV AVD is useful for Android-to-Android coverage but is not a substitute for the Web comparison.
- Three short test assets in the same library: a 2–3 minute H.264/AAC seekable file, a second episode for next-item behavior, and a deliberately high-bitrate or throttled asset for buffering. Include subtitles and multiple audio tracks on one item.
- Server logs, `adb logcat`, and screen recordings from both clients. Use synchronized host timestamps in filenames.

Do not put test credentials in Gradle files, shell history, screenshots, or repository files. Enter them in the client UI or provide them through an ignored local test harness.

## Initial setup and gate

1. Start the AVD with cold boot and verify it appears in `adb devices` as `device`.
2. Build and install the debug APK:

   ```sh
   JAVA_HOME=/Users/simon/Library/Java/JavaVirtualMachines/syncplay-jdk-21.jdk/Contents/Home \
     ./gradlew :app:installDebug
   ```

3. Launch the application with a TV remote/D-pad only. Sign in to the test server and play the baseline H.264 item outside SyncPlay.
4. Capture package identity, app version, emulator build/API, server version, and Web client version in the run report.
5. Run the existing automated gate before device testing:

   ```sh
   JAVA_HOME=/Users/simon/Library/Java/JavaVirtualMachines/syncplay-jdk-21.jdk/Contents/Home \
     ./gradlew detekt :app:testDebugUnitTest --tests '*SyncPlay*' \
       --tests '*PlaybackLauncherSyncPlayTests' \
       :playback:jellyfin:testDebugUnitTest :app:assembleDebug
   ```

Stop the run if ordinary solo playback, WebSocket connectivity, or the automated gate fails.

## Test matrix

Each case records pass/fail, both client states, approximate playback-position difference, relevant server/logcat excerpts, and screenshots or video where UI behavior matters.

### A. TV UI, discovery, and focus

| ID | Procedure | Pass criteria |
|---|---|---|
| A1 | Open SyncPlay from the home toolbar with only D-pad and Back. | Button is discoverable, label is readable at TV distance, initial focus is visible, Back returns focus to the invoking control, and no pointer/touch is required. |
| A2 | Open the dialog when no groups exist; refresh; create a group with the default name. | Empty/loading states are distinct, refresh does not duplicate requests or rows, the group is created once, and the UI changes to joined state. |
| A3 | Create several groups in Web, including long and non-ASCII names, then reopen/refresh the TV dialog. | Names and participant/state information render without clipping important actions; focus can reach every group and remains stable after refresh. |
| A4 | Open SyncPlay from both the legacy playback overlay and the Media3/rewrite OSD. | Both entry points are reachable, use the same active membership state, and return focus to playback controls. |
| A5 | Rotate/recreate the activity where supported, background/foreground the app, and close/reopen the dialog while joined. | UI recreation does not create another group, join twice, or discard valid session membership. |

### B. Membership and interoperability

| ID | Procedure | Pass criteria |
|---|---|---|
| B1 | Create a group on Android TV; discover and join it from Jellyfin Web. | Both clients show the same group and both participants exactly once. |
| B2 | Create a group in Web; join from Android TV. | Android receives GroupJoined and the authoritative queue/state without requiring playback to have started locally. |
| B3 | Join the same group/action twice through rapid remote input. | Only one effective membership transition occurs and UI does not oscillate or show duplicate participants. |
| B4 | Leave from Android, then from Web in a separate run. | Both clients converge to the correct membership; leaving does not stop unrelated solo playback unless the server command requires it. |
| B5 | Delete/end the group or revoke library access while Android is joined. | Android exits following mode, gives a clear bounded error, and remains usable for solo playback. |

### C. Authoritative queue and playback

| ID | Procedure | Pass criteria |
|---|---|---|
| C1 | Queue two items in Web before Android joins, including the same media item twice if the server permits it. | Android preserves server order and occurrence identity and starts the selected occurrence. |
| C2 | Start playback from Web while Android is browsing. | Android launches the correct item and prepares at the server position without a duplicate activity or duplicate playback session. |
| C3 | Start playback on Android while both clients are ready. | Web follows the same item, position, and play state; Android does not execute the local command independently before server authority. |
| C4 | Issue pause, unpause, and seeks from each client in turn, including rapid seek bursts. | The server-approved final command wins; neither client loops commands back to the server; settled position difference is at most 500 ms after two seconds on a quiet local network. |
| C5 | Select next, previous, and a specific queue entry from each client. | Both clients load the same occurrence once and old delayed commands cannot affect the new item. |
| C6 | Let the first item end naturally. | Exactly one next-item request is generated, both clients advance once, and playback progress reporting remains correct. |
| C7 | Stop playback from either client. | Both stop once, the activity/UI state is coherent, and a later stale pause/seek does not restart or alter playback. |
| C8 | Change audio track and subtitles locally. | Local stream preferences remain usable and do not corrupt group position or queue state. Differences that SyncPlay intentionally does not synchronize are documented rather than treated as group failures. |

### D. Readiness, buffering, and timing

| ID | Procedure | Pass criteria |
|---|---|---|
| D1 | Start an item from cold cache on both clients. | Android reports buffering and ready for the correct queue occurrence; group begins only according to server state. |
| D2 | Throttle the emulator network or use the high-bitrate asset until Android buffers. | Android reports buffering once per transition, the group waits or follows server policy, and recovery reports actual position/play state. |
| D3 | Toggle ignore-wait/resync behavior through the available UI while one client buffers. | Requests are reflected by server/group state and controls cannot leave the client permanently detached by accident. |
| D4 | Run continuous playback for 15 minutes, sampling both positions every minute. | No command storm, unbounded drift, repeated seek loop, or growing offset. Investigate any settled offset above 500 ms. |
| D5 | Put the Mac under moderate CPU load and repeat play/pause scheduling. | Commands remain ordered and late commands converge to server-authoritative position without crashing. |

### E. Remote controls and Android lifecycle

| ID | Procedure | Pass criteria |
|---|---|---|
| E1 | While joined, use D-pad transport controls, the seek bar, hardware/media keys, and Android MediaSession commands. | Every group-affecting action passes through the SyncPlay interceptor and is applied once after server authority. |
| E2 | Background Android during playback, send pause/seek from Web, then foreground it. | State converges without applying obsolete commands or creating another player. |
| E3 | Force-stop Android while joined, then relaunch. | Cold start does not claim stale membership as truth; server eventually removes the lost session according to its behavior. |
| E4 | Sign out, switch server, and switch user while joined. | Old membership is invalidated immediately; no cleanup or delayed command is sent using the new session's credentials. |
| E5 | Lock/sleep the emulator if supported, then wake it after a server-side state change. | Client either reconnects and safely resynchronizes or clearly leaves following mode; it never silently plays divergent media. |

### F. Network and server failures

| ID | Procedure | Pass criteria |
|---|---|---|
| F1 | Disable emulator networking for 10 seconds while playing, issue commands from Web, then restore it. | Android pauses or enters a visible disconnected state, cancels deadlines, and does not replay stale commands on reconnect. |
| F2 | Restart the Jellyfin server while Android is joined. | Failure is bounded, UI remains responsive, and recovery either performs a verified resync or requires an explicit rejoin. |
| F3 | Return 401/403/404/500 for group list, create, join, and playback requests using a test proxy or configured account policies. | Each failure produces a stable user-visible state; retries do not duplicate group mutations; solo playback remains available. |
| F4 | Use a server where SyncPlay is disabled or unsupported. | SyncPlay reports unavailable/disabled without breaking login, browsing, or ordinary playback. |
| F5 | Inject delay, packet loss, and WebSocket interruption independently of HTTP. | HTTP success cannot falsely imply synchronized membership when socket updates are unavailable. |

### G. Playback and device compatibility

| ID | Procedure | Pass criteria |
|---|---|---|
| G1 | Repeat core play/pause/seek/next scenarios with direct play and transcoding. | Both paths maintain authoritative position and readiness semantics. |
| G2 | Repeat with HLS, subtitles, and alternate audio tracks. | Stream reconfiguration does not create false end/buffering events or duplicate next-item requests. |
| G3 | Attempt live TV, non-seekable media, photos, and an external player. | Unsupported modes are blocked or explained cleanly and never claim to be synchronized. |
| G4 | Run the smoke suite on the oldest practical TV/API image and on a current image; finish on one physical Android TV device. | UI, media keys, decoding, suspend/resume, and focus work across the supported device sample. Emulator success alone is not release sign-off for hardware decoding or remote behavior. |

## Automation to add after the first manual run

1. Add Android instrumented tests for toolbar and OSD entry points, initial focus, Back behavior, group list rendering, duplicate-action suppression, and activity recreation.
2. Add a local controllable Jellyfin fixture or disposable container with two users and known media. Drive Jellyfin Web with browser automation as the second client.
3. Add an `adb` harness that installs the APK, clears data, launches activities, sends D-pad/media keys, toggles connectivity, captures logcat, records the screen, and gathers app/server versions.
4. Add structured debug logging around group generation, occurrence ID, command type/deadline, player readiness, actual position, and outgoing request. Logs must omit tokens and credentials.
5. Promote B1–B4, C2–C7, D1–D2, E1–E4, and F1–F2 to a repeatable pre-merge integration suite. Keep long-run drift, codec coverage, and physical-device checks as scheduled/release tests.

## Exit criteria

- Existing 53 SyncPlay unit/application tests and the build gate pass.
- All A, B, and C cases pass against the selected Jellyfin release and current Jellyfin Web.
- D1–D3, E1–E4, and F1–F4 pass with no crash, ANR, credential leak, command loop, or stale-session action.
- Settled local-network position difference stays within 500 ms for ordinary playback; exceptions have logs and a reproducible cause.
- A 15-minute run completes without increasing drift or request volume.
- One physical Android TV smoke run confirms remote focus, MediaSession controls, suspend/resume, and representative direct-play/transcode playback before release.

## Run report

Record: commit SHA; APK checksum; emulator name/API/build; host/emulator architecture; Jellyfin server and Web versions; media IDs/codecs; network conditions; case results; measured offsets; links to logcat, server logs, and recordings; every defect with reproduction steps. A failed case stays failed until rerun on the same build or explicitly waived with a linked issue.
