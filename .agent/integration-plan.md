# Jellyfin Android TV SyncPlay integration plan

Planning only; read the [functionality baseline](initial-prompt.md) and [test-first plan](test-suite-plan.md) together. Source revisions are pinned in the baseline. File paths in the Android map below exist at that revision; proposed components are explicitly marked new.

## 1. What the Web implementation establishes

All web paths below are relative to the [reviewed web tree](https://github.com/jellyfin/jellyfin-web/tree/ec168353e1a7662881cef13a3358aa2937f3279d/src).

| Source | Observed responsibility | Android implication |
| --- | --- | --- |
| `plugins/syncPlay/plugin.ts` | Registers video/audio/default wrappers; binds player and API-client changes | One authenticated-session coordinator, player-specific adapters and explicit session cleanup |
| `scripts/serverNotifications.js` | Subscribes SyncPlayCommand and SyncPlayGroupUpdate, forwards to manager | Reuse SDK WebSocket subscription; separate these messages from ordinary PlaystateMessage |
| `plugins/syncPlay/core/Manager.js` | Membership, group/participant updates, clock-readiness barrier, command/item checks, following vs halted playback | Serialized state machine with richer cancellation/generation protection |
| `plugins/syncPlay/core/Controller.js` | Converts local playback/queue actions into API requests | Explicit group-aware command gateway shared by all input routes |
| `plugins/syncPlay/core/PlaybackCore.js` | Scheduled commands, Ready/Buffering, duplicate repair, drift measurement and speed/seek correction | Pure timing/synchronization logic plus raw local player operations |
| `plugins/syncPlay/core/QueueCore.js` | Timestamped authoritative queue snapshots, media resolution, current occurrence and startup Ready | Keep `PlaylistItemId` distinct from library `ItemId`; async queue resolution must be versioned |
| `plugins/syncPlay/core/timeSync/{TimeSync,TimeSyncCore,TimeSyncServer}.js` | Four-timestamp offset estimator, lowest-delay sample, server time requests, optional extra offset | Inject wall/monotonic clock, use SDK time API and test offset signs |
| `plugins/syncPlay/ui/players/{NoActivePlayer,HtmlVideoPlayer,HtmlAudioPlayer}.js` | Intercepts playback manager methods, supports joining before a player exists; video reports sustained buffering after 3 s | Preserve behavior with typed interfaces, not runtime monkey-patching; prepare without early playback |
| `plugins/syncPlay/ui/groupSelectionMenu.js` | Create/join list, active group, halt/resume/leave, settings | TV group dialog/screen accessible from browse and OSD |
| `apps/modern/components/AppToolbar/{SyncPlayButton.tsx,menus/SyncPlayMenu.tsx}` and `apps/modern/features/syncPlay/` | Modern toolbar UI, SDK mutations, access-policy checks, participant/group presentation | Reuse TV Compose controls/focus/navigation rather than browser menu geometry |
| `plugins/syncPlay/ui/settings/{SettingsEditor.js,editor.html}` | Correction controls, thresholds, extra clock offset and group information | Diagnostics and a small validated correction preference first; advanced fields optional |

Observed Web caveats should become tests or intentional improvements, not requirements to copy: Manager checks pre-join emission time but does not establish a complete total ordering; async callbacks can outlive ownership; QueueCore startup extrapolates positions broadly and its fallback repeat/shuffle getters appear swapped; duplicate Seek uses randomized offset to provoke a callback; NoActivePlayer patches methods globally and contains a suspicious clearQueue restoration reference. Android should use explicit state, correct enum mappings and deterministic readiness handling. These are static observations, not reproduced runtime bug claims.

## 2. Jellyfin wire contract

Authoritative cross-checks: [server SyncPlayController](https://github.com/jellyfin/jellyfin/blob/1d7b6d97844c8cc848ed3fb5c4b48bb9cdd5b139/Jellyfin.Api/Controllers/SyncPlayController.cs), [time controller](https://github.com/jellyfin/jellyfin/blob/1d7b6d97844c8cc848ed3fb5c4b48bb9cdd5b139/Jellyfin.Api/Controllers/TimeSyncController.cs), [SDK SyncPlayApi](https://github.com/jellyfin/jellyfin-sdk-kotlin/blob/8729ab41951dc5e905271107bbc0031cd7680524/jellyfin-api/src/commonMain/kotlin-generated/org/jellyfin/sdk/api/operations/SyncPlayApi.kt).

The pinned Android dependency already has `ApiClient.syncPlayApi`, `timeSyncApi`, `SyncPlayCommandMessage`, `SyncPlayGroupUpdateMessage` and sealed `GroupUpdate` subtypes. Reuse those APIs; an SDK upgrade is not a prerequisite established by this research. Contract tests must check compatibility with the chosen supported server versions, especially unknown/older update variants and message envelopes.

HTTP inventory (all SyncPlay routes prefixed `/SyncPlay`, through existing authenticated/base-path-aware ApiClient):

| Operation | Method/path | Request essentials |
| --- | --- | --- |
| List / inspect | GET `/List`, GET `/{id}` | Inspect endpoint is optional for older servers and returns only group information, not a playback snapshot |
| Create / join / leave | POST `/New`, `/Join`, `/Leave` | `GroupName`; `GroupId`; no body respectively |
| Replace queue/start selection | POST `/SetNewQueue` | `PlayingQueue` = library IDs, `PlayingItemPosition`, `StartPositionTicks` |
| Select occurrence | POST `/SetPlaylistItem` | `PlaylistItemId` |
| Remove / clear | POST `/RemoveFromPlaylist` | `PlaylistItemIds`, `ClearPlaylist`, `ClearPlayingItem` |
| Move | POST `/MovePlaylistItem` | `PlaylistItemId`, `NewIndex` |
| Append / next insertion | POST `/Queue` | `ItemIds`, `Mode` = Queue or QueueNext |
| Resume / pause / stop | POST `/Unpause`, `/Pause`, `/Stop` | No body; Stop is group-wide, distinct from local halt |
| Seek | POST `/Seek` | `PositionTicks` |
| Buffer / ready | POST `/Buffering`, `/Ready` | `When` UTC, `PositionTicks`, `IsPlaying`, `PlaylistItemId` |
| Wait participation | POST `/SetIgnoreWait` | `IgnoreWait` |
| Next / previous | POST `/NextItem`, `/PreviousItem` | Current `PlaylistItemId` to avoid stale-item advancement |
| Repeat / shuffle | POST `/SetRepeatMode`, `/SetShuffleMode` | `Mode`: RepeatNone/RepeatOne/RepeatAll; Sorted/Shuffle |
| Latency | POST `/Ping` | `Ping`: one-way estimate in milliseconds in the Web implementation |
| Clock | GET `/GetUtcTime` (outside prefix) | Response `RequestReceptionTime`, `ResponseTransmissionTime` |

Reviewed server create returns HTTP 200 with GroupInfoDto; other mutations generally return 204. Older behavior may differ. HTTP success is not proof that media is ready, or that an asynchronous group operation was accepted; consume authoritative updates and errors. Non-idempotent create/queue/next requests must not be retried blindly after ambiguous timeouts.

Socket payloads:

- `SyncPlayCommand` wraps `SendCommand`: `GroupId`, `PlaylistItemId`, `When`, `EmittedAt`, `Command` (Unpause/Pause/Seek/Stop), nullable `PositionTicks`. A Stop need not match current occurrence, but must match group/session.
- `SyncPlayGroupUpdate` carries a `Type`-discriminated update and `GroupId`. Current server/SDK variants: GroupJoined, GroupLeft, UserJoined, UserLeft, StateUpdate, PlayQueue, NotInGroup, GroupDoesNotExist, LibraryAccessDenied. Web also handles GroupUpdate, SyncPlayIsDisabled, CreateGroupDenied and JoinGroupDenied; do not assume the current sealed SDK can decode historical variants. Build captured compatibility fixtures and use a bounded decoder/SDK fix if necessary.
- GroupInfo contains group ID/name, state, participant **names**, LastUpdatedAt. Names are not unique session IDs. Use full refreshed snapshots to reconcile ambiguous membership; do not derive authorization or session ownership from a name.
- PlayQueue contains `Playlist[{ItemId,PlaylistItemId}]`, `PlayingItemIndex`, `StartPositionTicks`, `IsPlaying`, `LastUpdate`, repeat/shuffle and update Reason. Implement all ten reasons listed in test Q03.
- Session WebSocket keepalive is managed by the SDK/session infrastructure; `/SyncPlay/Ping` reports latency, not the standalone Syncplay protocol heartbeat.

Group membership and authority belong to the Jellyfin server. Do not introduce a TV-only host/leader model. Account/library access remains enforced by server policies; local UI mirrors it.

## 3. Existing Android integration map

Unless a path begins `playback/`, paths below are relative to `app/src/main/java/org/jellyfin/androidtv/`.

| Existing file/module | Current behavior and required change |
| --- | --- |
| `di/PlaybackModule.kt` | Installs ExoPlayer, media session and Jellyfin plugins; registers legacy manager and RewriteMediaManager. Wire shared coordinator/gateway/adapters here without constructing duplicate coordinators. |
| `auth/repository/SessionRepository.kt` | Exposes currentSession and switching/restoring state. Add bounded old-session cleanup hook before credentials are replaced, plus generation invalidation on every session change. |
| `data/eventhandling/SocketHandler.kt` | Lifecycle-RESUMED SDK subscriptions, ordinary play/legacy controls, capability reporting. Add session-level SyncPlay ownership through dedicated component; arbitrate ordinary remote commands. Do not invent unsupported capability fields. |
| `ui/playback/PlaybackLauncher.kt` | Chooses external, rewrite or legacy video; audio uses MediaManager. Intercept group play before local queue mutation and external launch; provide a separate server-originated local launch path. |
| `ui/playback/PlaybackController.java`, `VideoManager`, `VideoQueueManager` | Default legacy video route, seek/speed/prepared/completion and local auto-next behavior. Add explicit raw pause/resume/seek adapter operations, real buffering callbacks and group queue ownership. |
| `ui/playback/CustomPlaybackOverlayFragment.java`, `overlay/CustomPlaybackTransportControlGlue.java`, `overlay/VideoPlayerAdapter.java` | Legacy OSD/transport inputs. Route group-affecting actions through gateway; add SyncPlay OSD action. |
| `ui/player/video/{VideoPlayerControls,VideoPlayerFragment,VideoPlayerScreen}.kt`, `ui/player/base/PlayerSeekbar.kt` | Rewrite UI calls PlayerState directly; fragment recreates queue onCreate, pauses onPause, unpauses onResume and stops onStop. These paths must consult coordinator ownership rather than override group state. |
| `playback/core/src/main/kotlin/PlayerState.kt` | MutablePlayerState invokes backend directly, no command-origin hook; desired play state lacks readiness/buffering. Introduce generic command interception and preparation state without importing Jellyfin types into core. |
| `playback/core/src/main/kotlin/queue/QueueService.kt` | Supplies local queue and auto-advances on stream end; random/repeat chosen locally. Add external queue authority mode and authoritative snapshot application. |
| `playback/media3/exoplayer/src/main/kotlin/ExoPlayerBackend.kt` | Media3 callbacks map playback state without exporting dedicated readiness/buffering. Add distinct state/events, seek completion/position discontinuity handling and prepare-paused support. |
| `playback/media3/session/src/main/kotlin/MediaSessionPlayer.kt` | OS/media-session transport entry point. Must use same group-aware control path as OSD. |
| `playback/jellyfin/src/main/kotlin/playsession/{PlaySessionSocketService,PlaySessionService}.kt` | Ordinary remote-control subscriber plus playback reports. Avoid duplicate command execution; retain actual Jellyfin play session reporting. |
| `playback/jellyfin/src/main/kotlin/mediasegment/MediaSegmentService.kt` | Media segment playback behavior. Prevent unsynchronized automatic seeks while grouped; explicit user Skip uses group Seek. |
| `ui/shared/toolbar/MainToolbar.kt` | Existing Compose toolbar/focus restoration. Add discoverable SyncPlay button and active status. |
| `ui/navigation/{Destinations,router}.kt` | Register group destination if using full-screen list; shared dialog may also be hosted over existing player. |

`UserPreferences.playbackRewriteVideoEnabled` defaults to false at the baseline. Therefore implementing only the rewrite would omit normal video users. Prefer a common coordinator and shared adapter contract, land default legacy video first, then rewrite video/audio before declaring full support. An alternative session-only supported-player route is possible, but must be explicit to the user and pass the same acceptance matrix; silently flipping the experimental player preference is not the proposed plan.

## 4. Proposed architecture and ownership

New code names below are design suggestions, not existing APIs.

Place reusable Jellyfin-specific components under `playback/jellyfin/src/main/kotlin/syncplay/`. Keep UI and legacy glue in app `syncplay/` and `ui/syncplay/`. Avoid a new Gradle module until isolation materially requires it.

- **SyncPlayTransport**: wraps SDK APIs and socket updates; maps network/compatibility failures to typed events. Uses existing ApiClient. Exposes no Android view.
- **SyncPlayCoordinator**: one instance per active authenticated session, alive while browsing as well as playing; owns serialized event reducer, immutable StateFlow, membership, queue snapshot, pending operations and current adapter attachment. Key ownership by server/user/session generation + group generation + occurrence/load generation.
- **SyncPlayClock / CommandScheduler / DriftController**: injectable clocks and scheduler; pure calculations and cancelable jobs. Clock measurement readiness is separate from player readiness and group Playing state.
- **SyncPlayQueue**: retains authoritative occurrence list and version, resolves metadata by ItemId while reconstructing exact server order, cancels stale fetches and prepares selected occurrence through adapter. Unknown/missing item cannot silently shift the queue.
- **SyncPlayPlayerAdapter**: ability to prepare an occurrence at an absolute media position while paused; raw pause/unpause/seek/stop; read actual position/playing/preparation; optional supported speed range; ready/buffering/ended/error callbacks. Supply LegacyVideoAdapter in app and RewritePlayerAdapter using playback core. No adapter is required just to discover/join an idle group.
- **PlaybackCommandGateway**: routes group-affecting user intent to the server while joined (including a new play selection while locally halted), and to solo operations when unjoined. A halted member must explicitly leave before starting independent solo playback. Raw server execution bypasses gateway. Introduce an interception seam in core and equivalent legacy facade so OS controls, sockets, UI, seekbar and automatic behaviors cannot bypass ownership.
- **SyncPlayViewModel**: exposes remote-friendly UI state and intents, using session-scoped coordinator; UI recreation must not recreate membership or pollers.

Avoid a short-lived boolean “ignore events” around an async player call. Use explicit operation origin and generation/target identity; a delayed server-induced pause callback must never become a new user Pause request. Real backend readiness, errors and normal playback reporting are still observed for server-induced changes.

State model: membership = Unjoined / Joining / Joined / Leaving / Disconnected; joined group state = Idle / Waiting / Paused / Playing; local following = Following / Halted; preparation = Unloaded / Preparing / Ready / Buffering / Ended / Error. Keep these orthogonal. A paused ready player and a buffering player are not interchangeable.

Apply reducer/network work off the UI thread; confine raw player interactions to main thread. Serialize commits after every async boundary. Cancel command, correction, media-load and readiness jobs on replacement, leave, disconnect or detach. Expose minimal diagnostics for offset, ping, occurrence, desired/actual state, drift and correction count.

## 5. Timing, startup and queue behavior

Clock math: with client send/receive t0/t3 and server receive/send t1/t2, offset = `((t1−t0)+(t2−t3))/2`, network round trip = `(t3−t0)−(t2−t1)`, Web Ping = round(round trip/2). Web retains eight samples and uses minimum delay, polls greedily at 1 s then at 60 s. Use monotonic elapsed time for scheduled waits and age; convert server UTC deadlines using a measured UTC/monotonic anchor. Detect wall-clock discontinuity and reacquire samples. Do not substitute ordinary HTTP round trip for one-way Ping.

1. Subscribe before submitting Create/Join. Enter pending membership; handle HTTP response and socket acknowledgment in either order. Once GroupJoined establishes ownership, request time samples and await queue/commands. Keep group info available without opening a player for an empty group.
2. Apply the authoritative queue snapshot and resolve items without losing duplicate occurrences. Prepare current item paused. A late command may arrive before metadata or player attachment: retain it only for the matching current generation/occurrence.
3. For paused media use given position unchanged. For a playing timeline estimate `positionTicks + (serverNow−When) * 10,000 ticks/ms`; use IsPlaying when falling back to queue snapshot. Validate/clamp positions where duration is known and preserve Long precision through navigation and launch.
4. Report Ready with **actual** prepared position and corrected server timestamp. Execute future commands at converted deadlines; late Unpause catches up to current group time. Seek prepares target, reports readiness and waits for server resume; native Media3 need not mimic Web's temporary unpause workaround.
5. On sustained buffering, debounce the notification (initial proposal 3 s as in Web), then report Buffering. Readiness after initial preparation or group seek has its own explicit path. Report Ready on recovery even when paused; let server resolve group Waiting state. Bound failure to prepare and offer halt/retry.
6. A new command cancels previous scheduled command/correction. Reject old generation, old emission timestamp and wrong occurrence; allow Stop for the group independent of item. Equal timestamps do not provide a total order: deduplicate equivalent payloads, preserve arrival order for distinct commands, and reconcile with subsequent server state. Do not invent sequence numbers the server does not send.
7. Queue snapshots apply monotonically by LastUpdate, with recheck after metadata fetch. Disable local auto-next/repeat/shuffle execution while externally controlled. End-of-media sends NextItem with current occurrence once; new queue determines the next item. Queue-only edits do not rebuild active playback needlessly.

Continuous correction is disabled by default in reviewed Web even though speed/seek strategies are enabled individually. TV proposal: enable conservative automatic correction after hardware validation; seek-only fallback is mandatory, speed correction uses a bounded device-supported range (initial candidate 0.95–1.05x), larger errors use seek. Web's thresholds are useful characterization values, not required TV constants. Evaluate the candidate against the test plan's 5/10 s convergence gates; tune thresholds/duration together before shipping. Disable user speed changes while following, retain saved solo speed, reset to 1x after each correction and restore saved speed on exit. Keep optional extra timing offset as advanced calibration, not necessary setup.

The reviewed server's [WaitingGroupState](https://github.com/jellyfin/jellyfin/blob/1d7b6d97844c8cc848ed3fb5c4b48bb9cdd5b139/MediaBrowser.Controller/SyncPlay/GroupStates/WaitingGroupState.cs) validates occurrence and reported position; [Group](https://github.com/jellyfin/jellyfin/blob/1d7b6d97844c8cc848ed3fb5c4b48bb9cdd5b139/Emby.Server.Implementations/SyncPlay/Group.cs) uses a 500 ms maximum playback offset. Report actual state instead of pretending the player reached the requested seek position. No client algorithm guarantees frame-perfect output across TVs.

## 6. TV product behavior

Use existing JellyfinTheme buttons, popovers/dialogs, strings and focus restoration. A toolbar SyncPlay button opens a TV-sized group list with group name, participants and state. Permit group creation with a localized default name and optional editing via TV keyboard. Loading, retry, empty and access-denied states must be legible without a pointer. Avoid frequent polling of group lists when the menu is closed; refresh on open and explicit retry.

An active group view shows group name, participant names, Waiting/Paused/Playing/Idle status, Resume group playback or Stop watching on this TV, Leave group, and a shared queue view. Queue actions use focused rows and simple menus (Play this, Play next, Remove, Move up/down, Clear, Repeat/Shuffle), rather than requiring web drag-and-drop. All server queue updates must work even when this view is closed.

Add a SyncPlay action to both legacy and rewrite OSDs, reachable without leaving playback. Show a small labeled waiting/resync indicator only when needed; participant notifications must not steal focus. Keep detailed offset/drift diagnostics in playback info/settings, not the primary viewing flow. Advanced tuning fields can wait; the standard UI should work without entering timing numbers.

Semantics:

- Play/Pause, committed seek, next/previous, explicit intro skip and queue edits affect the group. Debounce/coalesce repeated seek gestures, submit the final target, and reflect pending state. Local immediate pause feedback may match Web, but reconciliation after request failure is mandatory.
- Volume/mute/audio/subtitles are local; stream changes that rebuffer still report Buffering/Ready. Prerolls and automatic segment skipping/episode advancement are disabled or group-routed so preferences cannot silently split timelines.
- “Stop watching on this TV” sets IgnoreWait true, cancels local tasks and stops local media while retaining membership. “Resume group playback” restores wait participation and prepares authoritative current playback.
- “Leave group” ends membership, releases timers and restores solo controls/speed. If media is still playing and local queue remains usable, continue it solo; a halted device stays stopped. Never send group Stop as a side effect of leaving.
- Server Stop always stops local group playback. If a TV group-wide stop action is exposed, label it “Stop for everyone” separately; the ordinary exit/halt action is local.
- First Back dismisses group/OSD overlay; exiting the player halts local following. Home/background/sleep also halts/ignores this device where a request is possible. Foreground return offers Resume instead of the current unconditional unpause.
- External players lack reliable continuous control/readiness: explain that SyncPlay requires a supported built-in player and offer that route for this session. Do not change saved player preference silently. Exclude photos, live TV and nonseekable media from synchronized playback initially; retain independent solo playback support.

## 7. Disconnect and resource policy

Group owner lifecycle must not follow a fragment's RESUMED subscription window. Bind to authenticated session plus active participation, observe process lifecycle, and deliberately halt/leave before stopping group subscriptions when backgrounded. Keep SDK heartbeat/connection management in the SDK rather than implementing parallel sockets.

On connection loss: invalidate deadlines, pause locally, mark disconnected, and prevent queued UI mutations. On recovery: reacquire clock, verify intended group still exists and permissions remain, and obtain fresh GroupJoined/PlayQueue/command state through server-supported rejoin behavior. `GET /SyncPlay/{id}` alone is insufficient because it has no queue/position. Characterize repeated Join behavior against supported releases; where it does not refresh an existing membership, use a deliberate Leave→Join reconciliation, with bounded errors and no group recreation. Resume only once state and player are ready.

Explicit leave cancels reconnect intent. Process death does not persist active membership as truth; cold start is unjoined until a deliberate, verified join. Logout/server switch must invalidate local work immediately and attempt bounded best-effort Leave using the old immutable session credentials before replacement. Never send delayed cleanup to the newly signed-in server. Network loss can prevent cleanup; server expiry is authoritative and version-dependent. The reviewed server has a [lost-WebSocket cleanup regression test](https://github.com/jellyfin/jellyfin/blob/1d7b6d97844c8cc848ed3fb5c4b48bb9cdd5b139/tests/Jellyfin.Server.Integration.Tests/SyncPlayLostWebSocketTests.cs); do not promise the same timeout on all releases.

Keep normal playback start/progress/stop reports for actual local media. Avoid duplicate reports when both old/new player services are registered. Permission failure, unsupported payloads and unrecoverable media errors must leave a clear retry/leave path; no permanent spinner or recursive readiness retry.

## 8. Implementation order and unresolved validation

Each phase starts with the matching failing tests from the test plan; no implementation is part of this planning pass.

1. **Contract fixtures and seams** (M/W/T): capture released server traces, verify pinned SDK decoding, add pure clock/reducer tests, define transport/player/gateway interfaces.
2. **Membership and discovery** (M/L/U): session scope, transport, group state and fake-backed TV group view. Joining an idle group works without a player; do not advertise synchronized playback yet.
3. **Default legacy video** (A/B/T): attach explicit operations/readiness, prepare-paused launch, scheduled commands, buffering and normal session reporting. Prove Web↔TV create/join/pause/seek.
4. **Authoritative queue and control audit** (Q/A): preserve occurrence IDs, cover all queue mutations, automatic behaviors, remote keys and ordinary socket controls. Make queue UI usable with D-pad.
5. **Rewrite video and audio** (shared A/B/Q contracts): core readiness/interception/queue ownership, Media3 adapter, fragment lifecycle and MediaSession changes; reuse coordinator. No separate copy of synchronization logic.
6. **Correction and failure hardening** (D/L): measured drift, speed capability fallback, reconnection, logout, background and process cleanup.
7. **Full device/interoperability acceptance** (E01–E08): tune based on measured visible timing, run regressions, record support matrix and limitations. Then enable supported paths by default.

Validation questions to settle through tests/spikes, not by assuming Web behavior:

- Which released server versions remain in the app support floor, and can SDK 1.8.12 decode each version's group updates, unknown types and missing envelope fields without killing the subscriber?
- Does repeat Join reliably refresh current playback on each release? Record exact recovery exchange and fallback; group-info lookup is not a snapshot API.
- Can each built-in backend prepare/seek while paused and expose accurate completion/readiness, including transcoding? Audit absolute content position vs stream-relative offsets before implementing the adapter.
- How do low-power TV devices and audio passthrough react to temporary playback-rate changes? Constrain rates or use seek-only based on real capability; method availability is insufficient.
- Are equal-millisecond timestamps or participant names ambiguous in practice? Reconcile with authoritative updates rather than inventing identity/order; maintain regression traces.
- Does the proposed 3 s buffering debounce and correction policy meet viewing targets across direct-play/transcode and Web's default correction-off behavior? Adjust with evidence and document limitations.

These are bounded engineering validation tasks for implementation, not blockers to the completed research/test/integration plans. No changes outside the three planning documents are intended in this branch at this stage.
