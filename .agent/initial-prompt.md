# SyncPlay: initial request and functionality baseline

Planning date: 2026-09-12. Branch: `dev/syncplay`.
This is a research and test-design task; no production code or executable tests are implemented in this pass.

## Original request

> Make a new branch called dev/syncplay.
>
> The goal is to add syncplay (like netflix teleparty) support to the androidtv client of Jellyfin. The webclient found here has support: https://github.com/jellyfin/jellyfin-web. The syncplay protocol is here: https://syncplay.pl/about/protocol/
>
> First, take a pass at the webclient code and the syncplay to understand exactly what needs to be implemented for syncplay support to be added, in terms of functionality. Design a comprehensive test suite (before writing any code) that validates the criteria above is fullfiled.
>
> Then, look through the webclient to plan out how it should be ideally integrated into the jellyfin android tv codebase. Ideally use it to figure UI intergration and so on, but perfect adherence is not needed and likely, the difference between web and android will prohibit that anyway.
>
> Save this and the test and integration plan to .agent/initial-prompt.md, .agent/test-suite-plan.md and .agent/integration-plan.md

## Protocol decision

Implement **Jellyfin SyncPlay interoperability with Jellyfin Web**, using the authenticated Jellyfin session, HTTP APIs and existing WebSocket. The [linked Syncplay protocol](https://syncplay.pl/about/protocol/) describes a separate application: JSON over TCP, Hello authentication, named rooms, file metadata, List/Set/State messages, ping and forced-state acknowledgements (`ignoringOnTheFly`). Its published page labels the protocol v1.2.7 and discusses subsequent changes. It is useful conceptual background for coordinated playback, latency and conflicting updates, but is not Jellyfin's wire contract. Do not implement its TCP connection, password hashing, filename exchange or four-second heartbeat in this client.

Jellyfin instead identifies server library items and distinct queue occurrences, accepts group requests over HTTP, and sends scheduled commands and group updates over its session WebSocket. No separate Syncplay server, Teleparty service or peer-to-peer transport is needed. Chat, invite links, host election, room passwords, cross-server libraries and standalone Syncplay compatibility are not implied by this request or the inspected web implementation.

## Reproducible research sources

Source inspection used shallow checkouts outside this repository; those temporary directories are not required to use these plans. Links below pin the actual reviewed revisions, rather than claiming compatibility with every release.

| Reference | Reviewed revision / entry points |
| --- | --- |
| Android TV baseline | `ecae0c1c76569b22f0f4586719a7853ef0171b5b`; local repository, clean before branch creation |
| Jellyfin Web | [`ec168353e1a7662881cef13a3358aa2937f3279d`](https://github.com/jellyfin/jellyfin-web/tree/ec168353e1a7662881cef13a3358aa2937f3279d/src/plugins/syncPlay) |
| Jellyfin server (contract cross-check) | [`1d7b6d97844c8cc848ed3fb5c4b48bb9cdd5b139`](https://github.com/jellyfin/jellyfin/blob/1d7b6d97844c8cc848ed3fb5c4b48bb9cdd5b139/Jellyfin.Api/Controllers/SyncPlayController.cs) |
| Kotlin SDK already selected by Android TV | `1.8.12`, tag `v1.8.12`, commit [`8729ab41951dc5e905271107bbc0031cd7680524`](https://github.com/jellyfin/jellyfin-sdk-kotlin/tree/8729ab41951dc5e905271107bbc0031cd7680524); `SyncPlayApi`, `TimeSyncApi`, generated message models |
| Standalone Syncplay | [Protocol page](https://syncplay.pl/about/protocol/), read 2026-09-12; conceptual comparison only |

The server and web references are their fetched default-branch snapshots. Release support must be established with actual released server/web pairs before shipping. The app derives its minimum supported server version from `Jellyfin.minimumVersion` in `auth/repository/ServerRepository.kt`; do not infer a SyncPlay compatibility floor from the latest sources.

## Required functionality and acceptance IDs

These IDs define the test plan's requirements. Numeric performance targets and TV behavior choices below are proposed product requirements, not protocol guarantees.

| ID | Required outcome |
| --- | --- |
| F01 | Discover groups on the signed-in server, create a named group, join and leave; allow creation only with `CreateAndJoinGroups`, joining with `JoinGroups` or creation access, and respect `None`. Handle HTTP errors and asynchronous denials. |
| F02 | Show authoritative membership, participant names and group state (Idle, Waiting, Paused, Playing). Membership persists across in-app browsing and player UI recreation, but never leaks between users or servers. |
| F03 | A TV can create a party and start media; a TV can join one created by Web before playback, while paused, or while playing. Load the correct library item/queue occurrence, prepare at the correct position, and wait until the server authorizes playback. |
| F04 | Any permitted participant's play/resume, pause, seek, next/previous and queue actions coordinate the group. Commands received from the server execute locally without request loops. Volume, mute, audio tracks and subtitles remain local choices. |
| F05 | Synchronize client/server clocks, report latency, schedule Unpause/Pause/Seek/Stop, compensate for late Unpause, cancel superseded work and reject stale/wrong-group/wrong-item commands. Preserve zero positions and 64-bit tick values. |
| F06 | Report actual readiness and buffering with timestamp, position, playing flag and playlist occurrence ID. A waiting member can hold the group; recovery resumes together. Failure cannot leave other participants waiting indefinitely without explanation or an exit path. |
| F07 | Maintain server-owned queue order, unique occurrence IDs, current index, repeat/shuffle and all queue update reasons. Handle duplicate library items, end-of-item advancement and queue edits without independently choosing another item. |
| F08 | Support “Stop watching on this TV” (`SetIgnoreWait(true)` plus local stop) and “Resume group playback” (`false` plus resynchronization), separately from leaving and from a server Stop. Leaving returns control to solo playback. |
| F09 | Measure drift and correct it through bounded playback speed or seek, without audible/visible correction storms. Restore solo speed on leaving. Web exposes correction settings but defaults continuous correction OFF; TV defaults must be validated explicitly. |
| F10 | Survive delayed/duplicate messages, dropped socket, request timeout, offline device, foreground/background changes, playback failure, logout and process death. Reconnect must obtain fresh membership/playback state before applying commands. |
| F11 | Provide discoverable, remote-friendly entry points from browsing and supported playback UIs; loading/empty/error/pending/waiting states; participants; halt/resume/leave; accessible focus and localized labels. |
| F12 | Preserve normal solo playback, session reporting and remote control. Support both built-in video routes in the final feature, and built-in audio for web functional breadth. External players, photos and live/nonseekable streams require explicit unsupported handling. |
| F13 | Prove Web↔TV and TV↔TV operation using a real Jellyfin server and controlled media/network conditions; unit tests alone do not establish synchronized viewing. |

## Completion and scope

The present deliverables are this baseline, the [test suite design](test-suite-plan.md), and the [integration plan](integration-plan.md). Tests must be authored before the corresponding production changes in the subsequent implementation effort. No server feature work is presumed necessary.

For the eventual feature, completion requires all mandatory F01–F13 cases to pass for the declared supported built-in player matrix. A staged implementation may start with legacy video, then the rewrite and audio, but must not describe a one-route prototype as full support. Advanced tuning widgets and precise web menu layout are optional; interoperable commands, queue behavior, lifecycle isolation and usable TV controls are not.
