# SyncPlay implementation progress

Updated 2026-09-15 on `dev/syncplay`.

## Implemented

The protocol/timing layer lives in `playback/jellyfin/src/main/kotlin/syncplay/`:

- four-timestamp server clock estimation, minimum-delay sample selection, Ping calculation, monotonic scheduling and wall-clock discontinuity reset;
- a generation-protected membership/queue reducer that keeps Jellyfin library item IDs distinct from repeated playlist occurrence IDs;
- scheduled Pause/Unpause/Seek/Stop handling with deadline replacement, late-Unpause catch-up and stale occurrence rejection;
- SDK-backed HTTP and WebSocket transport for membership, clock, readiness, playback and queue requests;
- a session coordinator for create/join/leave, clock acquisition, queue reconciliation, paused preparation, Ready/Buffering reports, halt/resume, disconnect/error handling and one-shot end-of-item advancement;
- explicit `SyncPlayClient` and `SyncPlayPlayer` boundaries for UI and backend integration.

Android application integration now includes:

- one process-lifecycle coordinator and Media3 player adapter registered through Koin;
- Media3 buffering and ready backend events, paused preparation, absolute seek verification and playback error/end propagation;
- a policy-gated toolbar action and TV side panel for group discovery, create/join, participants/state, retry, halt/resume and leave;
- the same SyncPlay action in the rewrite video OSD;
- group-aware playback launches that submit `SetNewQueue` and wait for the authoritative server queue instead of starting an independent local queue;
- group-aware Compose play/pause, committed seek, skip forward/back and previous/next controls;
- MediaSession interception so remote/media-key playback, seek and stop commands cannot silently split the local timeline; stop halts this TV rather than stopping the group;
- player lifecycle behavior that halts following when playback goes to the background and does not unpause independently on return;
- bounded best-effort remote leave before authenticated session replacement/destruction, followed by immediate local state invalidation.

## Verification

The Android toolchain is installed outside the repository:

- Java 21: `/Users/simon/Library/Java/JavaVirtualMachines/syncplay-jdk-21.jdk`
- Android SDK: `/Users/simon/Library/Android/sdk`
- API 37 platform, build-tools 37/36, platform-tools and Apple Silicon command-line tools

`local.properties` points Gradle at that SDK and remains ignored by Git.

The canonical module tests pass. The SyncPlay foundation currently has 43 coordinator/clock/session/scheduler/transport tests. Ten application tests cover discovery, safe failures, duplicate membership actions, normalized creation, session isolation, halt/resume/leave, authoritative playback launches, and MediaSession interception. Production app and Media3 modules compile with Java 21.

Commands used:

```sh
JAVA_HOME=/Users/simon/Library/Java/JavaVirtualMachines/syncplay-jdk-21.jdk/Contents/Home \
  ./gradlew :playback:jellyfin:testDebugUnitTest

JAVA_HOME=/Users/simon/Library/Java/JavaVirtualMachines/syncplay-jdk-21.jdk/Contents/Home \
  ./gradlew :app:testDebugUnitTest --tests '*SyncPlayViewModelTests' --tests '*PlaybackLauncherSyncPlayTests'

JAVA_HOME=/Users/simon/Library/Java/JavaVirtualMachines/syncplay-jdk-21.jdk/Contents/Home \
  ./gradlew detekt
```

Detekt completes successfully. The repository-wide report still contains pre-existing findings; SyncPlay-specific complexity suppressions document the state-machine and declarative-menu cases.

## Remaining validation and hardening

This is a functional first integration, but it has not yet been exercised against a real Jellyfin server or a physical/emulated TV. The highest-priority remaining work is:

1. run captured/released-server compatibility fixtures and Web-to-TV interoperability for create/join, queue load, pause, seek, buffering, next and disconnect;
2. add focused tests around the Media3 adapter and MediaSession interceptor, including readiness races and a seek that never settles;
3. add explicit reconnect/rejoin reconciliation and unknown historical group-update handling;
4. add queue editing UI and repeat/shuffle requests, plus visible waiting/resync feedback;
5. verify audio, transcode, unsupported/live/nonseekable behavior and decide the supported media matrix;
6. measure drift on hardware and add conservative correction only after the test-plan convergence criteria pass.

The complete acceptance matrix and research rationale remain in `test-suite-plan.md`, `initial-prompt.md`, and `integration-plan.md`.
