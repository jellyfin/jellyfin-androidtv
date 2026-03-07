# SyncPlay Debugging Guide

This project now emits high-signal SyncPlay diagnostics through Timber with two tags in the message body:

- `SyncPlayRepo` for API requests, command suppression, and repository state transitions.
- `SyncPlaySocket` for socket events, queue application, and command timing/drift telemetry.

## What Is Logged

Repository (`SyncPlayRepo`):

- Group lifecycle actions: `refreshGroups`, `createGroup`, `joinGroup`, `leaveGroup`.
- Playback sync actions: queue creation, `sendPause`, `sendUnpause`, `sendSeek`, `sendStop`, `sendReady`.
- Pending-ready handshake lifecycle after queue changes.
- State-impacting server updates: not-in-group, missing group, library denied.

Socket handler (`SyncPlaySocket`):

- Every SyncPlay command and group update frame.
- Queue reconciliation decisions (ignored duplicates, empty queues, item loading chunks, target item selection).
- Navigation and controller-reuse behavior when applying queue updates.
- `sendReady` dedupe decisions.
- Command timing telemetry:
  - `schedulingDelayMs`: local-now minus command `when`.
  - `transitDelayMs`: local-now minus command `emittedAt`.

## Drift and Timing Analysis

Use `SyncPlaySocket` command timing logs to understand drift behavior:

1. High `transitDelayMs` indicates network or server-to-client delivery delay.
2. High `schedulingDelayMs` indicates command execution happened significantly after scheduled time.
3. Compare timing spikes with queue-update logs and `sendReady` cadence.

## SmartScreen Entry Point

The SyncPlay menu is integrated into the library SmartScreen "Views" row.

- Open any SmartScreen library.
- Select the `SyncPlay` grid button.
- Available actions depend on active group state:
  - No active group: create, refresh, join existing groups.
  - Active group: leave and refresh.

## Suggested Logcat Filters

Use one of these patterns in Logcat:

- `SyncPlayRepo|SyncPlaySocket`
- `SyncPlay.*(failed|ignored|timed out|delay)`

## Typical Triage Flow

1. Confirm command/update frames are arriving (`SyncPlaySocket command/update`).
2. Check command delay metrics for drift symptoms.
3. Verify queue mapping and item loading (`loading ... chunks`, `target item=...`).
4. Verify `sendReady` behavior and dedupe decisions.
5. Inspect repository failures for API-side issues (`SyncPlayRepo ... failed`).
