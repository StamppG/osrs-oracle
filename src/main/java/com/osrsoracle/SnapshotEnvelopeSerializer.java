package com.osrsoracle;

final class SnapshotEnvelopeSerializer
{
static final class Parts
{
String account;
String clientTime;
String snapshotReason;
String evidenceJson;
SnapshotLiveStateCollector.State liveState;
String diaryTaskStateJson;
String globalResourceCapabilityStateJson;
String persistentStorageLiveItemStateJson;
String collectionLogJson;
String instantCollectionLogJson;
String inventoryJson;
String bankJson;
String seedVaultJson;
String gimStorageJson;
String coxPrivateStorageJson;
String coxSharedStorageJson;
String gravestoneStorageJson;
String deathsOfficeStorageJson;
String potionStorageJson;
String motherlodeSackJson;
String plankSackJson;
String herbSackJson;
String gemBagJson;
String gemSatchelJson;
String coalBagJson;
String fishBarrelJson;
String logBasketJson;
String lootingBagJson;
String seedBoxJson;
String tackleBoxJson;
String forestryKitJson;
String huntsmansKitJson;
String barbarianKnapsackJson;
String dizanasQuiverAmmoJson;
String stashUnitsJson;
String equipmentJson;
}

private SnapshotEnvelopeSerializer()
{
}

static String serialize(Parts parts)
{
SnapshotLiveStateCollector.State liveState =
parts.liveState;

return String.format(
"{\"account\":\"%s\"," +
"\"accountType\":%s," +
"\"membershipActive\":%s," +
"\"membershipDaysRemaining\":%s," +
"\"clientTime\":\"%s\"," +
"\"snapshotReason\":\"%s\"," +
"\"evidence\":%s," +
"\"slayerTask\":\"%s\"," +
"\"slayerRemaining\":%d," +
"\"combatAchievements\":{" +
"\"easy\":%d," +
"\"medium\":%d," +
"\"hard\":%d," +
"\"elite\":%d," +
"\"master\":%d," +
"\"grandmaster\":%d," +
"\"completedTaskIds\":%s}," +
"\"achievementDiaries\":%s," +
"\"achievementDiaryTaskState\":%s," +
"\"globalResourceCapabilityState\":%s," +
"\"persistentStorageLiveItemState\":%s," +
"\"collectionLog\":%s," +
"\"collectionLogInstant\":%s," +
"\"skills\":%s," +
"\"quests\":%s," +
"\"inventory\":%s," +
"\"bank\":%s," +
"\"seedVault\":%s," +
"\"gimStorage\":%s," +
"\"coxPrivateStorage\":%s," +
"\"coxSharedStorage\":%s," +
"\"gravestoneStorage\":%s," +
"\"deathsOfficeStorage\":%s," +
"\"potionStorage\":%s," +
"\"motherlodeSack\":%s," +
"\"plankSack\":%s," +
"\"herbSack\":%s," +
"\"gemBag\":%s," +
"\"gemSatchel\":%s," +
"\"coalBag\":%s," +
"\"fishBarrel\":%s," +
"\"logBasket\":%s," +
"\"lootingBag\":%s," +
"\"seedBox\":%s," +
"\"tackleBox\":%s," +
"\"forestryKit\":%s," +
"\"huntsmansKit\":%s," +
"\"barbarianKnapsack\":%s," +
"\"dizanasQuiverAmmo\":%s," +
"\"stashUnits\":%s," +
"\"equipment\":%s}",

escapeJson(parts.account),
liveState.accountTypeJson,
liveState.membershipActiveJson,
liveState.membershipDaysJson,
escapeJson(parts.clientTime),
escapeJson(parts.snapshotReason),
parts.evidenceJson,
escapeJson(liveState.slayerTask),
liveState.slayerRemaining,

liveState.caEasy,
liveState.caMedium,
liveState.caHard,
liveState.caElite,
liveState.caMaster,
liveState.caGrandmaster,
liveState.caCompletedIdsJson,

liveState.diaryJson,
parts.diaryTaskStateJson,
parts.globalResourceCapabilityStateJson,
parts.persistentStorageLiveItemStateJson,
parts.collectionLogJson,
parts.instantCollectionLogJson,

liveState.skillsJson,
liveState.questsJson,

parts.inventoryJson,
parts.bankJson,
parts.seedVaultJson,
parts.gimStorageJson,
parts.coxPrivateStorageJson,
parts.coxSharedStorageJson,
parts.gravestoneStorageJson,
parts.deathsOfficeStorageJson,
parts.potionStorageJson,
parts.motherlodeSackJson,
parts.plankSackJson,
parts.herbSackJson,
parts.gemBagJson,
parts.gemSatchelJson,
parts.coalBagJson,
parts.fishBarrelJson,
parts.logBasketJson,
parts.lootingBagJson,
parts.seedBoxJson,
parts.tackleBoxJson,
parts.forestryKitJson,
parts.huntsmansKitJson,
parts.barbarianKnapsackJson,
parts.dizanasQuiverAmmoJson,
parts.stashUnitsJson,
parts.equipmentJson
);
}

private static String escapeJson(String text)
{
if (text == null)
{
return "";
}

return text
.replace("\\", "\\\\")
.replace("\"", "\\\"")
.replace("\n", "\\n")
.replace("\r", "\\r")
.replace("\t", "\\t");
}
}