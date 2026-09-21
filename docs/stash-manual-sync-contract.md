# STASH Manual Sync Contract

Status: ENGINE AND RETENTION IMPLEMENTED / SIDEBAR TRIGGER DEFERRED

## Product rule

STASH state is durable account state, but discovery is deliberately manual.

SagaSeer MUST NOT:

- scan every STASH unit automatically on login
- scan every STASH unit automatically on world hop
- scan every STASH unit on heartbeat
- replace the last successful complete STASH state with a partial or failed scan

The player must explicitly request a STASH synchronization.

The future SagaSeer RuneLite sidebar will call `startManualStashSync()` through a **Sync STASH Units** button.

The sidebar control itself is intentionally deferred to later sidebar/UI work.

## Authoritative RuneLite source

`ScriptID.WATSON_STASH_UNIT_CHECK` is RuneLite's STASH-state script.

RuneLite documents it as checking a STASH unit and returning two boolean results: built and filled.

SagaSeer uses RuneLite's current `STASHUnit.values()` catalog rather than maintaining an independent copy of STASH object IDs.

SagaSeer records only factual `built` and `filled` results.

## Client scan lifecycle

A manual scan:

1. starts only after an explicit call to `startManualStashSync()`
2. records the character name that started the scan
3. processes a bounded batch of STASH units per game tick
4. stores results only in temporary in-progress state while scanning
5. aborts if the character changes during the scan
6. aborts if RuneLite returns invalid script results
7. leaves the last successful retained STASH state untouched after an abort
8. atomically replaces retained STASH state only after the complete catalog succeeds
9. assigns one independent `observedAt` timestamp to the completed scan
10. requests exactly one snapshot with reason `STASH_MANUAL`

Current batch size: 8 STASH units per game tick.

## Retained client state

Top-level dataset: `stashUnits`

Shape:

- `scope`: `ACCOUNT`
- `shared`: `false`
- `observedAt`: timestamp of the complete successful scan
- `units`: complete STASH observation array

Each unit contains:

- `objectId`
- `built`
- `filled`

An empty `units` array from a complete newer observation is authoritative.

Ordinary later snapshots may carry the retained STASH dataset with its original `observedAt`; this does not imply STASH was freshly observed.

## Evidence semantics

`SnapshotEvidence.coverage.stashUnits` is independent from other datasets.

Before a successful scan:

- `status`: `NOT_OBSERVED`
- `observedAt`: `null`

After a successful scan:

- `status`: `OBSERVED`
- `observedAt`: timestamp of the successful complete scan

`STASH_MANUAL` uses evidence mode `MANUAL`.

## Backend retention

Durable STASH materialization is owned by the backend.

Backend rules:

- missing STASH data does not erase the previous complete observation
- `null` STASH data does not erase the previous complete observation
- malformed STASH data does not erase the previous complete observation
- older `observedAt` data cannot regress newer retained state
- a newer valid complete observation supersedes the previous state
- a newer valid complete observation with `units: []` is authoritative

Backend implementation checkpoint:

- `bcc7c6d Preserve manual STASH state`

## Deferred UI

Future RuneLite sidebar behavior should expose:

- **Sync STASH Units** button
- active synchronization progress
- last successful STASH synchronization time

Intended website instruction:

> **STASH data requires a manual sync.** In RuneLite, open the SagaSeer sidebar and click **Sync STASH Units**.
