# Slice 4 â€” Resource, Capability, Container, and Unlock Source Matrix

Status: IN PROGRESS
Branch: codex/resources-unlocks-containers

## Contract rules

The RuneLite client reports factual observations only.

The client MUST NOT:
- infer ownership from a cached variable alone
- infer capability when multiple requirements must be combined
- promote shared/activity resources to personal ownership
- convert unknown or unobserved state to zero
- erase historical state because a dataset is absent from a later snapshot
- interpret opaque bitfields when interpretation belongs server-side

## Scope classes

- GLOBAL â€” persistent character state readable during normal live snapshots
- ACCOUNT_STORAGE â€” persistent resources belonging to the character outside the normal bank
- OBSERVED_CONTAINER â€” contents are authoritative only after RuneLite actually observes the container
- GROUP_STORAGE â€” resources shared by a GIM group
- ACTIVITY_STORAGE â€” raid/minigame scoped resources
- ITEM_STATE â€” state tied to a positively identified item
- DERIVED_SERVER_SIDE â€” client reports factual inputs; platform derives meaning
- SPECIAL_SYNC â€” deliberately synchronized state not appropriate for heartbeat polling
- NEEDS_PROOF â€” source identified, semantics still require validation

## Global resources and currencies

| Dataset | RuneLite/Jagex source | Scope | Status |
|---|---|---|---|
| Slayer points | VarbitID.SLAYER_POINTS | GLOBAL | READY |
| Slayer task streak | VarbitID.SLAYER_TASKS_COMPLETED | GLOBAL | READY |
| NMZ reward points | VarPlayerID.NZONE_REWARDPOINTS | GLOBAL | READY |
| Tithe Farm points | VarbitID.HOSIDIUS_TITHE_REWARDPOINTS | GLOBAL | READY |
| Soul Wars zeal | VarPlayerID.SOUL_WARS_ZEAL | GLOBAL | READY |
| Giants' Foundry points | VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS | GLOBAL | READY |
| Mastering Mixology Mox | VarPlayerID.MIXOLOGY_MOX_POINTS | GLOBAL | READY |
| Mastering Mixology Aga | VarPlayerID.MIXOLOGY_AGA_POINTS | GLOBAL | READY |
| Mastering Mixology Lye | VarPlayerID.MIXOLOGY_LYE_POINTS | GLOBAL | READY |
| Camdozaal stored Barronite | VarPlayerID.CAMDOZAAL_STORED_BARRONITE | ACCOUNT_STORAGE | READY |
| Scar essence mine coffer | VarPlayerID.SCAR_ESSENCEMINE_COFFER | ACCOUNT_STORAGE | READY |
| Colosseum glory | VarPlayerID.COLOSSEUM_GLORY | GLOBAL | READY |
| Barbarian Assault role points | BARBASSAULT_POINTS_* | GLOBAL | READY |
| Bounty Hunter points | VarPlayerID.BH_2023_POINTS | GLOBAL | READY |
| Motherlode sack quantity | VarbitID.MOTHERLODE_SACK_TRANSMIT + RuneLite Motherlode map-region context | ACTIVITY_STORAGE / CONTEXTUAL | IMPLEMENTED_SOURCE_PROVEN |
| Motherlode sack upgrade | VarbitID.MOTHERLODE_BIGGERSACK | GLOBAL | READY |
| GOTR persistent/reward state | GOTR_* persistent vars | GLOBAL | NEEDS_PROOF |
| MTA currencies/state | MAGICTRAINING_* vars | GLOBAL | NEEDS_PROOF |

## Permanent capability and unlock evidence

| Dataset | Source | Scope | Status |
|---|---|---|---|
| Slayer unlock words | SLAYER_REWARDS_UNLOCKS / 1 / 2 | GLOBAL | READY |
| Slayer stored-task unlock/state | SLAYER_UNLOCK_STORAGE + SLAYER_STORED_VARP | GLOBAL | READY |
| Rigour | PRAYER_RIGOUR_UNLOCKED | GLOBAL | READY |
| Augury | PRAYER_AUGURY_UNLOCKED | GLOBAL | READY |
| Preserve | PRAYER_PRESERVE_UNLOCKED | GLOBAL | READY |
| Deadeye | PRAYER_DEADEYE_UNLOCKED | GLOBAL | READY |
| Mystic Vigour | PRAYER_MYSTIC_VIGOUR_UNLOCKED | GLOBAL | READY |
| Piety / Chivalry | KR_KNIGHTWAVES_STATE + existing quest/stat evidence | DERIVED_SERVER_SIDE | NEEDS_PROOF |
| Current spellbook | SPELLBOOK + SPELLBOOK_SUBLIST | GLOBAL | READY |
| Prayer book | VarbitID.PRAYERBOOK | GLOBAL | READY |
| Arceuus spellbook unlocked | ARCEUUS_SPELLBOOK_UNLOCKED | GLOBAL | READY |
| Bones to Peaches | MAGICTRAINING_BONESPEACHES | GLOBAL | READY |
| Auto-weed | FARMING_BLOCKWEEDS | GLOBAL | READY |
| Quetzal destinations | QUETZAL_* destination vars + QUETZALS_UNLOCKED | GLOBAL | READY |
| Lovakengj minecarts | LOVAKENGJ_MINECARTS_STATUS | GLOBAL | READY |
| Fairy-ring permission | FAIRYRING_PERMISSION | GLOBAL | READY |
| Fairy-ring CIS unlock | ZEAH_FAIRYRING_CIS_UNLOCKED | GLOBAL | READY |
| POH Portal Nexus teleports | POH_NEXUS_TELEPORT* | GLOBAL | NEEDS_DECODER |
| GOTR needle/bag unlock | GOTR_UNLOCKED_NEEDLE / GOTR_BAG_OBTAINED | GLOBAL | READY |
| Giants' Foundry mould unlocks | GIANTS_FOUNDRY_UNLOCKED_MOULD_* | GLOBAL | READY |

## Persistent off-bank storage

| Storage | Source | Scope | Status |
|---|---|---|---|
| Bank | InventoryID.BANK | OBSERVED_CONTAINER | IMPLEMENTED |
| Seed Vault | InventoryID.SEED_VAULT | OBSERVED_CONTAINER | IMPLEMENTED |
| Tool Leprechaun | FARMING_TOOLS_* varbits | ACCOUNT_STORAGE | NEEDS_OBSERVATION_PROOF |
| Potion storage | POTIONSTORE_BUILD / POTIONSTORE_DOSE_CHANGE + Bankmain.POTIONSTORE_ITEMS + POTIONSTORE_VIALS | ACCOUNT_STORAGE / OBSERVED_UI | IMPLEMENTED_SOURCE_PROVEN |
| Rune pouch | RUNE_POUCH_TYPE_1..6 + QUANTITY_1..6 | ITEM_STATE | READY_LIVE |
| Essence pouches | essence pouch amount/type/degradation vars | ITEM_STATE | READY_LIVE |
| Bolt pouch | XBOWS_POUCH slot/quantity vars | ITEM_STATE | NEEDS_OBSERVATION_PROOF |
| Plank sack | PLANK_SACK_* vars | ACCOUNT_STORAGE | NEEDS_OBSERVATION_PROOF |
| Master scroll book | BOOKOFSCROLLS* vars | ACCOUNT_STORAGE | NEEDS_DECODER |
| Fossil Island storage | FOSSIL_STORAGE* vars | ACCOUNT_STORAGE | NEEDS_OBSERVATION_PROOF |
| Hallowed Sepulchre storage | HALLOWED_STORAGE_* vars | ACCOUNT_STORAGE | NEEDS_OBSERVATION_PROOF |
| Clue scroll case | SCROLL_CASE_* vars | ACCOUNT_STORAGE | NEEDS_OBSERVATION_PROOF |
| TOA stored pickaxe | TOA_PICKAXE_STORAGE | ACCOUNT_STORAGE | NEEDS_OBSERVATION_PROOF |
| Dizana's quiver ammo var state | DIZANAS_QUIVER_TEMP_AMMO / DIZANAS_QUIVER_TEMP_AMMO_AMOUNT + quiver-capable item context | ITEM_STATE | IMPLEMENTED_SOURCE_PROVEN |
| Pre-pot device / chugging loadouts | PREPOT_DEVICE_* vars | ACCOUNT_STORAGE | READY |
| POH Costume Room | POH costume inventories | OBSERVED_CONTAINER | READY |
| STASH units | WATSON_STASH_UNIT_CHECK | SPECIAL_SYNC | NEEDS_DESIGN |

## Observed containers

| Container | Inventory source | Scope | Status |
|---|---|---|---|
| Seed box | SEED_BOX | OBSERVED_CONTAINER | NEEDS_PROOF |
| Tackle box | TACKLE_BOX | OBSERVED_CONTAINER | NEEDS_PROOF |
| Forestry kit | FORESTRY_KIT | OBSERVED_CONTAINER | NEEDS_PROOF |
| Huntsman's kit | HUNTSMANS_KIT | OBSERVED_CONTAINER | NEEDS_PROOF |
| Looting bag | LOOTING_BAG | OBSERVED_CONTAINER | NEEDS_PROOF |
| Dizana quiver ammo container | DIZANAS_QUIVER_AMMO | OBSERVED_CONTAINER | NEEDS_PROOF |
| Dizana quiver charges | no standalone source-proven charge variable identified | ITEM_STATE | NEEDS_PROOF |
| GIM shared storage | INV_GROUP_TEMP / group storage inventory | GROUP_STORAGE | IMPLEMENTED_SOURCE_PROVEN |
| GIM storage access level | GIM_STORAGE_ACCESS_LEVEL | GROUP_STORAGE | NEEDS_PROOF |
| Gravestone storage | InventoryID.GRAVESTONE / InterfaceID.GRAVESTONE_GENERIC | ACCOUNT_STORAGE / OBSERVED_CONTAINER | IMPLEMENTED_NEEDS_RUNTIME_PROOF |
| Kept-on-death preview | InventoryID.DEATHKEEP / DEATHKEEP_ITEMS | PREVIEW_UI | EXCLUDED_FROM_OWNERSHIP |
| Death's Office permanent storage | InventoryID.DEATH_PERMANENT | ACCOUNT_STORAGE / OBSERVED_CONTAINER | NEEDS_PROOF |
| CoX private storage | RAIDS_PRIVATESTORAGE | ACCOUNT_STORAGE / OBSERVED_CONTAINER | IMPLEMENTED_SOURCE_PROVEN |
| CoX shared storage | RAIDS_SHAREDSTORAGE | ACTIVITY_STORAGE / SHARED | IMPLEMENTED_SOURCE_PROVEN |
| Forestry log storage | FORESTRY_SHOP_LOG_STORAGE | OBSERVED_CONTAINER | NEEDS_PROOF |
| Barbarian knapsack | BARBARIAN_KNAPSACK | OBSERVED_CONTAINER | NEEDS_PROOF |

## GIM rules

GIM storage is a first-class SagaSeer resource source.

Client facts:
- most recently observed shared-storage contents
- observedAt
- group/shared scope
- storage access-level evidence only after source semantics are proven

Platform rules:
- omitted or NOT_OBSERVED does not erase prior GIM storage state
- observed empty does erase prior contents
- group-shared items do not become personally owned items
- recommendations may separately reason about personally owned and group-available resources
- late observations must not regress newer accepted group-storage state

## Charged items

RuneLite exposes CHARGES_* state for many item families.

Do not publish a charge value unless:
1. item identity is positively established,
2. the charge source is proven to correspond to that item,
3. zero is distinguishable from unknown/unobserved.

Candidate families include:
- Arclight
- Bow of Faerdhinen
- Blade of Saeldor
- Dizana's quiver
- tridents
- Serpentine helm
- Toxic staff
- wilderness weapons
- Bonecrusher
- Ash sanctifier
- crystal armour/tools
- Sanguinesti staff
- Ring of suffering
- Xeric's talisman
- Venator bow
- Toxic blowpipe
- Blood fury
- tomes
- Warped sceptre
- Tumeken's Shadow
- Soul bearer
- Bracelet of ethereum
- Celestial ring

Scope: ITEM_STATE
Status: NEEDS_IDENTITY_PROOF

## Observation and retention rules

GLOBAL:
- collect on each valid live-state snapshot

Persistent var-backed ACCOUNT_STORAGE:
- collect on each valid live-state snapshot where semantics are proven global

OBSERVED_CONTAINER:
- NOT_OBSERVED until RuneLite actually exposes the container
- retain latest successful contents locally where appropriate
- preserve independent observedAt
- observed empty is authoritative
- omission or NOT_OBSERVED never means empty

GROUP_STORAGE:
- same retention model as bank
- preserve GROUP / SHARED ownership
- retain prior state until newer authoritative group-storage observation

ACTIVITY_STORAGE:
- preserve activity scope and timestamp
- do not merge into durable account totals

SPECIAL_SYNC:
- manual, deliberate, or opportunistic synchronization
- no heartbeat script spam

## Implementation groups

A. Global resources, currencies, unlocks, prayers, spellbooks, travel
B. Persistent var-backed account storage
C. Generic observed-container cache framework
D. GIM/group and raid/activity-scoped containers
E. Charged-item identity/state
F. STASH and other special scripted synchronization
G. Evidence coverage, focused tests, real-client proof


## Raid reward history sources

Raid reward openings are not retained current-state containers.

They are future historical event sources whose durable retention belongs to the backend/history architecture.

Source-proven RuneLite observation pairs:

- CoX: InterfaceID.RAIDS_REWARDS + InventoryID.RAIDS_REWARDS
- ToA: InterfaceID.TOA_CHESTS + InventoryID.TOA_CHESTS
- ToB: InterfaceID.TOB_CHESTS + InventoryID.TOB_CHESTS

When the historical event pipeline is implemented, the RuneLite client is responsible for:

- observing the reward interface lifecycle
- reading factual reward item IDs and quantities
- recording the client observation time
- triggering an immediate upload
- suppressing duplicate capture from reopening the same reward lifecycle

The backend is responsible for:

- durable reward-history retention
- retry deduplication
- authenticated character ownership
- ordering and querying history
- derived classifications such as uniques, purples, value, luck, and dryness

Reward-event contents MUST NOT be merged directly into current bank, inventory, equipment, or storage ownership.
