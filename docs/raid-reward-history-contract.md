# Raid Reward History Contract

Status: DEFERRED TO DATA / HISTORY ARCHITECTURE

## Product requirement

SagaSeer must preserve raid reward-opening history for:

- Chambers of Xeric (CoX)
- Tombs of Amascut (ToA)
- Theatre of Blood (ToB)

Players must eventually be able to query historical reward openings, including
use cases such as the last 10 rewards, rewards over a date range, unique-drop
history, and raid-specific reward history.

The history retention limit is not "10". Presentation limits such as the last
10 or last 25 rewards are query/UI choices over the durable history.

## Architecture boundary

The RuneLite client observes facts.

The backend owns history.

RuneLite is not responsible for maintaining the player's long-term raid reward
history locally.

## Source-proven RuneLite observation points

### CoX

Reward interface:

- InterfaceID.RAIDS_REWARDS

Reward container:

- InventoryID.RAIDS_REWARDS

### ToA

Reward interface:

- InterfaceID.TOA_CHESTS

Reward container:

- InventoryID.TOA_CHESTS

### ToB

Reward interface:

- InterfaceID.TOB_CHESTS

Reward container:

- InventoryID.TOB_CHESTS

RuneLite's own Loot Tracker uses these reward-interface/container pairs.

## Future RuneLite responsibility

When the reward-event pipeline is commissioned, the RuneLite client should:

1. detect the authoritative reward-interface lifecycle;
2. read the corresponding reward container;
3. capture factual item IDs and quantities;
4. capture the client observation time;
5. trigger an immediate upload;
6. avoid duplicate capture when the same reward interface is reopened.

The implementation should follow RuneLite's proven lifecycle pattern rather
than treating arbitrary reward-container change events as independent rewards.

ToB requires the same contextual care RuneLite applies to its reward chest so
unrelated uses of the interface/container are not misclassified.

## Future backend responsibility

The backend/history layer should:

- authenticate the character receiving the event;
- store accepted raid reward events durably;
- deduplicate retries;
- preserve complete reward history;
- retain event observation time separately from server receipt time;
- support chronological and raid-filtered queries;
- expose reward history to SagaSeer AI and the website.

Derived interpretation belongs server-side.

Examples include:

- unique versus common rewards;
- purple classification;
- Grand Exchange or alchemy value;
- statistical luck;
- dry streaks;
- reward summaries.

## Ownership rule

A historical reward event proves what the reward interface displayed at that
time.

It does not, by itself, prove where those items are currently stored.

Reward history MUST NOT be merged directly into current:

- bank;
- inventory;
- equipment;
- account storage;
- GIM storage.

Current ownership/location continues to come from authoritative current-state
observations.

## Current implementation status

No durable raid reward-history pipeline is implemented yet.

The previous retained `toaRewardChest` current-state model is intentionally
removed because a reward opening is a historical event, not persistent current
account state.

CoX, ToA, and ToB reward-event capture should be commissioned together against
the backend history contract rather than implemented as three unrelated
features.