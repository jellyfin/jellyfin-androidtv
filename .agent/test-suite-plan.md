# SyncPlay test suite plan — written before implementation

Baseline and requirement IDs: [initial-prompt.md](initial-prompt.md). Architecture and wire inventory: [integration-plan.md](integration-plan.md).

Status: **designed, not implemented or executed**. No SyncPlay implementation exists in this branch. This document specifies observable behavior, fixtures and pass criteria to drive tests before production changes. Where Android behavior deliberately differs from Web, it tests the proposed behavior rather than copying web implementation quirks.

## 1. Test layers and fixtures

| Layer | Location proposed | Execution and oracle |
| --- | --- | --- |
| Pure coordination tests | `playback/jellyfin/src/test/kotlin/syncplay/` | Kotest/JUnit 5, fake transport/player, injected clocks and coroutine scheduler. Assert state transitions and ordered side effects without sleeps. |
| Generic player contract | `playback/core/src/test/kotlin/`, `playback/media3/exoplayer/src/test/kotlin/` | Test command ownership, readiness, end events and cancellation; Android-backed Media3 behavior also needs instrumentation. |
| SDK/HTTP/socket contract | `playback/jellyfin/src/test/kotlin/syncplay/contract/` | Real SDK serialization and requests against local mock HTTP/WebSocket server; recorded fixtures from pinned released servers. Test SDK decoder, not only already-decoded fake DTOs. |
| App/legacy adapter | `app/src/test/kotlin/syncplay/` | Kotest/MockK following existing `VideoSpeedControllerTests`; spy raw playback operations and group requests, inject callbacks and session changes. |
| TV UI and player instrumentation | `app/src/androidTest/…/syncplay/` | AndroidX runner plus Compose UI tests, legacy view interaction and remote key injection. Emulator plus real TV hardware. Add this infrastructure; it is not assumed to exist already. |
| Real interoperability | Dedicated reproducible server/media setup and scenario scripts | Web + TV + second TV, server traces, client event traces and visual timecode recording. Separate slow acceptance lane. |

Existing app and playback Gradle modules already use Kotest/JUnit 5 and MockK. Add `kotlinx-coroutines-test`, mock HTTP/WebSocket tooling and instrumentation dependencies only when writing the tests; match existing catalog versions. Do not run unit tests against public or personal servers.

Fixtures:

- Fake wall clock and monotonic clock that advance independently; deterministic executor; programmable network and media load delay; explicit position sampling. Assert no outstanding scheduled work after cleanup.
- Fake player models unloaded/preparing/ready/buffering/ended/error separately from desired play state. It can be paused while ready, omit a redundant callback, reject speed changes, seek approximately, fail media loading, or complete an old load after a new one begins.
- Server A/B, users with each access policy, distinct sessions for the same user, duplicate display names, and groups G1/G2. Keep identifiers and authentication separate.
- Queue `[A#q1, B#q2, A#q3]`, where A repeats with distinct `PlaylistItemId`s; unordered metadata responses; inaccessible/deleted item; empty queue/index -1; large queue; nonzero initial index and resume position.
- Timestamp fixtures use UTC with fractional seconds, explicit offsets, equal emission timestamps and tick values of zero, sub-millisecond values, and values exceeding 32-bit milliseconds. Never convert through floating-point ticks.
- JSON corpus for every supported command/group update, old supported server payload shapes, extra fields, unknown enum/subtype and malformed message. Redact tokens and personal data from captured traces.

## 2. Membership, transport and compatibility

| Test | Requirements | Setup / action | Expected result |
| --- | --- | --- | --- |
| M01 | F01,F11 | List zero, one and many groups; refresh after a group disappears | Correct loading/empty/list states; stale row failure is actionable; never auto-join a different group. |
| M02 | F01 | Parameterize None, JoinGroups, CreateAndJoinGroups; invoke APIs as well as UI | Correct affordances; denied requests do not create local membership. Server remains authority. |
| M03 | F01,F02 | Create group; deliver HTTP success before/after GroupJoined; duplicate both | Exactly one joined transition/subscription/timer set. Pending state ends only on authoritative success; no duplicate create retries. |
| M04 | F01,F03 | Join idle, paused and playing groups | Group/queue identity and desired play state correct; join HTTP 204 alone does not invent a queue. |
| M05 | F01,F10 | 401, 403, 404, 429, 500, timeout, socket denial after HTTP success | Clear error/pending cleanup; invalid auth exits feature; no optimistic playback on denial; no blind retry of mutations. |
| M06 | F02 | UserJoined/UserLeft duplicates, same-name sessions, full membership refresh | No crash or fabricated session identity; authoritative snapshot reconciles ambiguous names; no unbounded participant inflation. |
| M07 | F02,F05 | G1→G2, then late G1 updates, timer and metadata completion | No mutation of G2 player/queue/UI. Session/group generation invalidates all old work. |
| M08 | F08,F10 | Leave, duplicate leave, GroupLeft/NotInGroup, leave request timeout | Local timers/control interception released promptly; remote leave outcome reported honestly; remaining group unaffected by an accidental Stop request. |
| M09 | F01,F12 | Unsupported server endpoint and each supported release payload | Feature reports unavailable where necessary; normal playback works; no mandatory dependence on newer `GET /SyncPlay/{id}`. |
| W01 | F01,F04,F06,F07 | Exercise every HTTP operation in integration-plan inventory | Correct method/path/base path/auth, request fields and units; correct handling of 200 create body, 204 mutations and error status. |
| W02 | F02,F05 | Decode real SyncPlayCommand and all GroupUpdate subtypes with actual SDK | GroupId, occurrence IDs, dates, nullable Stop position, enums and payload types retained; legacy shapes handled only if in supported release matrix. |
| W03 | F05,F10 | Unknown field, command enum, group subtype, malformed JSON/date/UUID | Ignore/log affected message or surface compatibility error; subsequent valid messages still work. Test SDK flow failure boundary explicitly. |
| W04 | F10,F12 | HTTPS server under a URL base path; reconnect token/session refresh | Reuse authenticated SDK connection; no second competing socket or lost base path; no token leakage to logs. |

## 3. Clock, scheduling and drift

Timing oracles are independent arithmetic examples, not calls back into implementation helpers.

| Test | Requirements | Setup / action | Expected result |
| --- | --- | --- | --- |
| T01 | F05 | t0=1000, server receipt t1=1120, server send t2=1130, client receipt t3=1050 ms | Offset +100 ms, network round trip 40 ms, reported one-way Ping 20 ms. Server deadline 2100 maps to local 2000. |
| T02 | F05 | More than eight samples with jitter and one slow outlier | Bounded sample window; lowest valid network-delay sample chosen for web-equivalent estimator; expired best sample replaced. |
| T03 | F05,F10 | Failed, negative-delay, impossible and wall-clock-jump measurements | Invalid samples rejected; no “ready” from failed first sample; reacquire valid clock after discontinuity; no busy loop. |
| T04 | F05 | Initial sampling, forced resync, normal polling, detach | One poll loop; characterize web's greedy counter exactly if ported (1-second interval, then 60 seconds); joined pings have millisecond one-way value; stop/cancel on detach. |
| T05 | F05 | Command before time sample or before media is ready | Retain current valid desired command, execute only after required preparation; newer command supersedes it. |
| T06 | F05 | Unpause at future deadline; advance to deadline−1 ms then deadline | No early unpause; exactly one raw unpause at deadline with virtual time. Inject media readiness arriving too late and enter resync instead of stale start. |
| T07 | F05 | Late Unpause: position 10 s at server time 100 s, now server time 102 s | Target 12 s. Late Pause or paused queue remains at 10 s, without extrapolation. |
| T08 | F05 | Future Pause, Seek and Stop; zero seek; Stop without position | Execute correct action at deadline; Pause ends paused at target; Seek prepares target and reports Ready without independently resuming; Stop stops locally. |
| T09 | F05 | Duplicate future/past command, already-paused player, same-position seek lacking callback | No duplicate timer or request loop; satisfy desired state; re-report Ready when needed; bounded event wait, no deadlock. |
| T10 | F05,F10 | Older command after newer; equal timestamp with different command; wrong group/item; Stop with previous item ID | Do not roll back known newer state; preserve arrival order for nonidentical equal-timestamp commands because protocol has no total sequence number; Stop item exception stays group-scoped. |
| T11 | F05,F10 | Supersede scheduled Unpause with Pause/Stop/leave; replace player during correction | Old timers and deferred callbacks cannot resume/seek new playback; restore speed exactly once. |
| T12 | F05 | Tick↔Duration conversion around 0, 10,000 ticks/ms, 10,000,000 ticks/s, long duration | Safe integer conversions with documented rounding; no overflow from existing Int navigation position; no null-for-zero bug; bounded negative/out-of-duration targets. |
| D01 | F09 | Controlled ±drift with correction off/on and thresholds at boundary−1, boundary, boundary+1 | Off measures only; enabled correction chooses declared policy. Characterize Web thresholds: 60 ms speed minimum, 3000 ms speed upper bound, 400 ms seek fallback, 1000 ms nominal correction duration. |
| D02 | F09 | TV bounded speed policy, ahead/behind, unsupported speed or passthrough restriction | Correct direction, positive supported rate; fallback seek when necessary; correction stops at configured bound and returns to 1x during group playback. |
| D03 | F09 | Paused, buffering, preparing, wrong occurrence, seeking, command superseded | No drift correction against an invalid timeline; one correction at a time; no outgoing Seek caused by local drift correction. |
| D04 | F09,F12 | Saved solo speed 1.25x; join, correct, leave/error/logout | Group runs at 1x except corrections; user's solo setting restored on every exit, not overwritten permanently. |

## 4. Buffering and queue consistency

| Test | Requirements | Setup / action | Expected result |
| --- | --- | --- | --- |
| B01 | F03,F06 | Initial load, Ready while paused, server Unpause | No media plays ahead before authorization; one valid Ready describes prepared occurrence and actual position. |
| B02 | F06 | Short stall <3 s; sustained stall ≥3 s; repeated callbacks | For proposed web-equivalent sustained-buffer debounce: short stall canceled, sustained stall reports once; Ready promptly follows recovery. Initial load/seek readiness must not be incorrectly delayed by this debounce. |
| B03 | F06 | Stall at same time as user Pause/Seek; Ready from prior item | Ready/Buffering tied to generation and occurrence; actual IsPlaying distinct from playback intent; old callback rejected. |
| B04 | F06,F08 | One slow TV in group of three; set IgnoreWait then restore | Group waiting follows server state; ignored TV cannot keep peers blocked; resume prepares current position before participation. |
| B05 | F06,F10 | Codec error, inaccessible media, failed transcode, never-ready player | Bounded timeout (proposed 30 s, configurable fixture); clear error; local halt plus best-effort IgnoreWait/leave; Retry starts fresh preparation; no retry storm. |
| B06 | F06 | Approximate keyframe seek near server's reviewed 500 ms readiness tolerance | Report measured position; accept corrective Seek, converge or fail explicitly; no fabricated readiness and no dependence on Web's randomized seek workaround. |
| Q01 | F03,F07 | NewPlaylist with index 1, resume >0, playing/paused IsPlaying | Correct occurrence loaded; preserve state and position; metadata response ordering cannot reorder queue. |
| Q02 | F07 | A appears at q1 and q3; seek or select q3 | Occurrence identity retained end-to-end; no collapse by ItemId; next/previous request contains current PlaylistItemId. |
| Q03 | F07 | Parameterize NewPlaylist, SetCurrentItem, NextItem, PreviousItem, RemoveItems, MoveItem, Queue, QueueNext, RepeatMode, ShuffleMode | Apply each reason; queue-only updates do not restart unchanged current media; current-item changes prepare once. |
| Q04 | F07 | Append/queue-next, move first/last, remove before/current/after item, clear keeping/removing current | Result equals server snapshot, including correct current occurrence and empty/index -1 handling. No local guess at authoritative order. |
| Q05 | F07 | Older/equal queue LastUpdate, two metadata requests complete in reverse order | Only latest valid snapshot commits; equal duplicate ignored; stale async load cannot attach wrong IDs or start media. |
| Q06 | F07 | Missing/deleted/inaccessible ItemId, invalid index, metadata partial result | Do not shift occurrence IDs onto wrong items; actionable halt/error; preserve authoritative queue for recovery. |
| Q07 | F07 | RepeatNone/RepeatOne/RepeatAll; Sorted/Shuffle; repeated end-of-media on two clients | Follow server order and repeat outcome; bounded NextItem per occurrence; no local auto-advance or double skip. |
| Q08 | F04,F07,F12 | Episode auto-next, intro skip, credits actions, prerolls, local shuffle options | Group-aware actions use server requests; disable unsolicited local skips/prerolls/auto-next that would diverge; solo behavior restored. |
| Q09 | F07,F10 | Stop then new queue; reconnect then older snapshot; long queue of 500 entries | Queue state resets by generation; metadata bounded/cancelable; UI remains responsive and preserves focus. |

## 5. Adapter, lifecycle and TV UI

| Test | Requirements | Setup / action | Expected result |
| --- | --- | --- | --- |
| A01 | F04,F12 | Parameterize legacy video, rewrite video, built-in audio; issue user pause/resume/seek/next/previous via UI, remote key and MediaSession | Exactly one intended group request, or documented coalesced seek; no bypass. Outside group invokes original solo behavior. |
| A02 | F04 | Apply server command through each adapter while player emits callbacks synchronously/asynchronously | Zero corresponding group control requests; readiness/progress reporting still allowed; local pause and unpause are explicit, never toggles. |
| A03 | F04,F12 | Ordinary PlayMessage/PlaystateMessage while in group | Single ownership policy: route user-like remote controls through group gateway; no competing SDK socket subscriber also executes raw playback. Volume/tracks remain local. |
| A04 | F06 | Real Media3 STATE_READY paused vs STATE_BUFFERING; legacy prepared vs spinner | Actual ready/buffering transitions reach coordinator; UI spinner is not the sole source of readiness. |
| A05 | F03,F12 | External-player preference on; attempt join/start; live TV/photo in group | Explain supported built-in route choice before changing playback; do not launch unsupported player or silently alter saved preference; other users' supported playback remains intact. |
| L01 | F02,F10 | Open/close group UI, navigate library↔player, recreate player view | One session coordinator; no repeated Join/New, no duplicate subscriptions; navigation does not auto-unpause against server state. |
| L02 | F08,F10 | Back closes overlay, then Back exits player; Home/background/sleep | First Back only dismisses overlay. Exiting group playback halts local following with IgnoreWait; lifecycle cannot accidentally pause everyone or resume solo. |
| L03 | F10 | Socket drops while playing/paused/waiting; reconnect with same/new session | Show disconnected/resync state; cancel deadlines, pause locally, reacquire clock and authoritative state. No replay of queued user mutations; rejoin only intended group if still permitted. |
| L04 | F10 | Group deleted/permission revoked during reconnect; network returns after local Leave | Fall back to unjoined with explanation; never recreate missing group or auto-rejoin after explicit leave. |
| L05 | F02,F10 | Logout/server switch during create, media fetch, seek, correction | Old credentials used only for bounded old-session cleanup; all late results invalidated; zero old-group effects or data in new session. |
| L06 | F10,F12 | Process kill, cold restore, repeated connect/disconnect 100 times | No persisted active membership assumption; no unexpected group playback on cold launch; listener/job counts return to baseline; no ongoing polling when detached. |
| L07 | F12 | Normal playback session reports during group play/pause/seek/stop | Correct actual item and position; one normal start/stop lifecycle per local play session; SyncPlay does not replace watched/resume reporting. |
| U01 | F11 | D-pad only from home and each video OSD to group list/create/join/halt/resume/leave | Every action reachable with visible focus, labels and deterministic Back behavior; focus returns to invoking button. |
| U02 | F01,F11 | Pending mutation; empty/error groups; retry; join-only and disabled users | No double-submit; readable actionable state; creation hidden/disabled consistently with policy; group changes do not steal focus. |
| U03 | F02,F11 | Waiting/playing/paused/idle/disconnected, participants join/leave | Text status matches authoritative state; no color-only signal; notifications rate-limited and do not obscure subtitles. |
| U04 | F11 | Long group/member names, 500 rows, RTL, large fonts, 720p/1080p/4K | No clipped primary actions or focus traps; list scrolls with remote; names treated as text; localized accessible descriptions. |
| U05 | F08,F11 | Stop watching, Resume, Leave while media is running | Clearly distinct results: halt stays member/ignored, resume follows fresh state, leave ends membership and returns solo controls. |

## 6. Real-server interoperability acceptance

Use isolated Jellyfin instances with pinned image digest/release, pinned web build and this APK revision. At execution time select the app's minimum supported server and a current stable server; use each one's matching web release. Add the reviewed development snapshots as a separate forward-compatibility lane. Record exact versions; do not label an untested version supported.

Media: a redistributable or generated seekable video with burnt-in frame/timecode, two episodes, duplicated queue item, seekable audio, direct-play and forced-transcode variants. Record media hash, frame rate, duration and transcode settings. Use separate accounts plus two sessions of the same account. Run on API 23-compatible hardware/emulation where feasible and a modern physical Android TV; cover each supported video route, audio, low-power TV and Wi-Fi.

| Scenario | Steps and pass oracle |
| --- | --- |
| E01: party creation both ways | Web creates→TV joins, TV creates→Web joins, TV→TV; start from empty group and repeat joining playing/paused media. Correct membership/item/position, no premature local playback. |
| E02: shared controls | Alternate Web and TV play/pause/seek/next/previous 20 times, including seeking to zero and near end. Exactly intended server-authoritative state and occurrence on all clients. |
| E03: queue parity | Use duplicate A/B/A queue, insert/remove/move, clear, repeat and shuffle from both clients. Same occurrence/current index/order everywhere; track ending on two clients never skips an extra entry. |
| E04: buffering | Throttle only TV's media stream for 5–15 s, then restore. Observe Buffering→group Waiting→Ready→scheduled resume; verify IgnoreWait allows others to continue and resuming catches TV up. |
| E05: network faults | Delay/jitter control traffic, interrupt WebSocket for 10 s while HTTP still works, then full network outage, restart server. Recover or show actionable unjoined/error state; never execute stale group commands. |
| E06: lifecycle | Halt/resume/leave, TV Home/sleep/wake, user switch, process kill during scheduled start. Remaining clients unaffected by accidental commands; recovery obeys L01–L06. |
| E07: sustained viewing | 30-minute session per primary video route with ±250 ms and ±1 s injected drift; exercise both seek-only and supported speed correction, and direct-play/transcode combinations. Measure convergence and correction count. |
| E08: policy/unsupported | Join-only/denied/library-restricted users, missing item, external preference and live stream selection. Clear TV errors; no unsupported client silently stalls group. |

Proposed quantitative release gates (validate on devices; changes require recorded rationale):

- On a stable LAN (measured RTT ≤100 ms), after all players are ready, converge within 5 s; pairwise displayed content-time skew p95 ≤250 ms and maximum ≤500 ms over each 60-second steady-state window.
- With 200 ms added RTT and ±50 ms jitter, converge within 10 s after network/media recovery; steady-state p95 ≤500 ms. During an actual stall do not claim synchronization; require visible Waiting/resync and eventual recovery.
- Pause/seek final position spread ≤500 ms after preparation, consistent with the reviewed server readiness tolerance; software scheduling tests use exact virtual time, device tests allow decoder/keyframe effects.
- No unsolicited local queue advance, request echo, stale command execution or cross-user state. These are zero-tolerance correctness failures even if timing passes.
- No permanent wait: after a failed load exceeds the configured 30 s client deadline, show recovery action and release this client's group-wait responsibility where connectivity permits. Offline server cleanup latency is measured separately, not promised to be under 30 s.

Measure all clients at a common reference instant. Log command When/EmittedAt, clock estimate, occurrence, local position, ready/buffering and correction method without secrets. For visible skew use a camera recording both screens/timecodes (or synchronized external capture); position API logs alone cannot prove displayed frame synchronization. Exclude clearly labeled buffering intervals from steady-state statistics, but report their duration and recovery separately. Keep raw data and calculate p95/max, not just averages. With Web correction off (its inspected default), report peer drift separately and also run Web correction on; do not hide a web-side limitation by weakening TV correctness tests.

## 7. Test-first sequence and release checklist

1. Write failing wire/clock/reducer tests M/W/T before transport and coordinator code. Capture a real server join trace to settle payload ordering and version differences.
2. Write fake-player B/Q/A contracts before either adapter; share contract cases across all adapters.
3. Add regression tests for the identified direct-control, auto-next and fragment lifecycle paths before changing them.
4. Write UI state/focus tests before adding group UI; implement against fake state first.
5. Run E01–E08 once integration works; keep a short E01/E02/E04 smoke lane and a scheduled full device matrix.

Planned Gradle commands: `./gradlew :app:testDebugUnitTest :playback:jellyfin:testDebugUnitTest :playback:core:testDebugUnitTest :playback:media3:exoplayer:testDebugUnitTest`, then relevant lint/detekt and `:app:assembleDebug`. Confirm actual available tasks with `./gradlew tasks --all` when implementing. Once instrumentation is added, run `:app:connectedDebugAndroidTest` against the device matrix. Do not claim those commands ran in this planning pass.

Maintain a results ledger of test ID, requirement, revision, server/web versions, player/device/media/network, result, trace artifact and defect link. Every F01–F13 must have passed unit/contract coverage plus its relevant device/interoperability scenario. No shipping with critical failures in membership isolation, queue identity, request loops, wrong-item playback or lifecycle cleanup. Optional tuning UI can be deferred explicitly; mandatory behavior cannot be silently dropped.
