# Slice 4 Retention and Merge Contract

Status: LOCKED
Branch: codex/resources-unlocks-containers

## Core rule

Absence from a snapshot is not evidence of absence from the account.

For any dataset that is not guaranteed to be observable on every snapshot, the platform MUST retain the most recent valid observation until a newer valid observation of that same dataset supersedes it.

## Container merge semantics

### OBSERVED with contents
A successful observation replaces the previously stored contents for that container.

Store:
- new contents
- new observedAt
- source/scope metadata

### OBSERVED empty
A successfully observed empty container is authoritative evidence that the container was empty at observedAt.

Replace the previous contents with an empty collection.

Observed empty is NOT the same as unknown or unobserved.

### NOT_OBSERVED
Do not modify the previously stored contents.

Do not replace the previous observedAt.

The snapshot may record that this dataset was not observed during the current capture, but historical state remains intact.

### OMITTED / missing from payload
Do not modify previously stored state.

Older clients, partial snapshots, collection limitations, interface closure, or unrelated snapshot triggers must never erase valid historical observations.

### Explicit removal
Items are considered removed only when a newer authoritative observation of the same container proves they are no longer present.

Do not infer removal because:
- the container was unavailable
- the interface was closed
- the field was null
- the field was omitted
- a heartbeat did not include it
- a different dataset was refreshed

## Freshness

Each independently observed dataset retains its own observedAt.

A newer overall snapshot timestamp does not make an older cached container observation newer.

Example:

Snapshot received 12:00
Bank last observed 11:45
GIM storage last observed 10:30
Tackle box not observed

The platform retains:
- bank contents observed at 11:45
- GIM storage contents observed at 10:30
- previous tackle-box observation, if one exists

The 12:00 snapshot must not rewrite those observation timestamps.

## Ownership and scope

Retention is independent from ownership.

### ACCOUNT
Belongs to the character.

Examples:
- bank
- Seed Vault
- Tool Leprechaun
- potion storage
- TOA stored pickaxe

### GROUP
Shared GIM state.

GIM storage contents remain historically retained when not observed, but MUST remain marked GROUP / SHARED.

A group item must not silently become personally owned by the observing character.

### ACTIVITY
Temporary or raid-scoped state.

Examples:
- CoX shared storage
- CoX private raid storage
- TOA mid-raid supplies

Activity state may be retained for history, but MUST NOT be merged into durable account-owned resource totals.

## GIM storage

GIM storage follows the same observation rules as bank storage:

OBSERVED:
- replace cached group-storage contents
- update observedAt
- preserve GROUP / SHARED scope
- record access-level evidence where available

NOT_OBSERVED or omitted:
- retain last known group-storage contents
- retain previous observedAt
- do not infer withdrawal, deletion, or loss

Observed empty:
- replace prior contents with empty
- this is distinct from not observed

## Late and out-of-order evidence

A late snapshot may be retained as historical evidence but MUST NOT regress the latest materialized container state if a newer observation of that same dataset has already been accepted.

Container ordering is based on that dataset's observation time/evidence ordering, not merely overall request receipt time.

## Client responsibility

RuneLite reports what it actually observed and when.

RuneLite must distinguish:
- OBSERVED
- NOT_OBSERVED
- observed empty
- unavailable/unsupported where applicable

RuneLite does not decide whether historical state should be deleted.

## Platform responsibility

The platform owns:
- durable retention
- merge ordering
- stale/current interpretation
- group/account/activity ownership boundaries
- history
- prevention of accidental state regression

