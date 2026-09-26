package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SnapshotEnvelopeSerializerTest
{
@Test
public void preservesFrozenFullSnapshotWireContract()
{
SnapshotLiveStateCollector.State liveState =
new SnapshotLiveStateCollector.State(
"\"IRONMAN\"",
"true",
"27",
1,
2,
3,
4,
5,
6,
"[7,8]",
"{\"diary\":9}",
"Abyssal \"demons\"",
10,
"{\"Attack\":11}",
"{\"Quest\":\"FINISHED\"}"
);

SnapshotEnvelopeSerializer.Parts parts =
new SnapshotEnvelopeSerializer.Parts();

parts.account = "Blue\"Helm";
parts.clientTime = "2026-09-26T12:00:00Z";
parts.snapshotReason = "TEST_REASON";
parts.evidenceJson = "{\"evidence\":12}";
parts.liveState = liveState;
parts.diaryTaskStateJson = "{\"diaryTasks\":13}";
parts.globalResourceCapabilityStateJson = "{\"resources\":14}";
parts.persistentStorageLiveItemStateJson = "{\"persistent\":15}";
parts.collectionLogJson = "{\"pages\":16}";
parts.instantCollectionLogJson = "{\"instant\":17}";
parts.inventoryJson = "[18]";
parts.bankJson = "[19]";
parts.seedVaultJson = "[20]";
parts.gimStorageJson = "{\"gim\":21}";
parts.coxPrivateStorageJson = "{\"coxPrivate\":22}";
parts.coxSharedStorageJson = "{\"coxShared\":23}";
parts.gravestoneStorageJson = "{\"grave\":24}";
parts.deathsOfficeStorageJson = "{\"death\":25}";
parts.potionStorageJson = "{\"potions\":26}";
parts.motherlodeSackJson = "{\"mlm\":27}";
parts.plankSackJson = "{\"plank\":28}";
parts.herbSackJson = "{\"herb\":29}";
parts.gemBagJson = "{\"gemBag\":30}";
parts.gemSatchelJson = "{\"gemSatchel\":31}";
parts.coalBagJson = "{\"coal\":32}";
parts.fishBarrelJson = "{\"fish\":33}";
parts.logBasketJson = "{\"logs\":34}";
parts.lootingBagJson = "{\"looting\":35}";
parts.seedBoxJson = "{\"seedBox\":36}";
parts.tackleBoxJson = "{\"tackle\":37}";
parts.forestryKitJson = "{\"forestry\":38}";
parts.huntsmansKitJson = "{\"huntsman\":39}";
parts.barbarianKnapsackJson = "{\"knapsack\":40}";
parts.dizanasQuiverAmmoJson = "{\"quiver\":41}";
parts.stashUnitsJson = "{\"stash\":42}";
parts.equipmentJson = "{\"equipment\":43}";

String json =
SnapshotEnvelopeSerializer.serialize(parts);

assertEquals(
"{\"account\":\"Blue\\\"Helm\"," +
"\"accountType\":\"IRONMAN\"," +
"\"membershipActive\":true," +
"\"membershipDaysRemaining\":27," +
"\"clientTime\":\"2026-09-26T12:00:00Z\"," +
"\"snapshotReason\":\"TEST_REASON\"," +
"\"evidence\":{\"evidence\":12}," +
"\"slayerTask\":\"Abyssal \\\"demons\\\"\"," +
"\"slayerRemaining\":10," +
"\"combatAchievements\":{" +
"\"easy\":1," +
"\"medium\":2," +
"\"hard\":3," +
"\"elite\":4," +
"\"master\":5," +
"\"grandmaster\":6," +
"\"completedTaskIds\":[7,8]}," +
"\"achievementDiaries\":{\"diary\":9}," +
"\"achievementDiaryTaskState\":{\"diaryTasks\":13}," +
"\"globalResourceCapabilityState\":{\"resources\":14}," +
"\"persistentStorageLiveItemState\":{\"persistent\":15}," +
"\"collectionLog\":{\"pages\":16}," +
"\"collectionLogInstant\":{\"instant\":17}," +
"\"skills\":{\"Attack\":11}," +
"\"quests\":{\"Quest\":\"FINISHED\"}," +
"\"inventory\":[18]," +
"\"bank\":[19]," +
"\"seedVault\":[20]," +
"\"gimStorage\":{\"gim\":21}," +
"\"coxPrivateStorage\":{\"coxPrivate\":22}," +
"\"coxSharedStorage\":{\"coxShared\":23}," +
"\"gravestoneStorage\":{\"grave\":24}," +
"\"deathsOfficeStorage\":{\"death\":25}," +
"\"potionStorage\":{\"potions\":26}," +
"\"motherlodeSack\":{\"mlm\":27}," +
"\"plankSack\":{\"plank\":28}," +
"\"herbSack\":{\"herb\":29}," +
"\"gemBag\":{\"gemBag\":30}," +
"\"gemSatchel\":{\"gemSatchel\":31}," +
"\"coalBag\":{\"coal\":32}," +
"\"fishBarrel\":{\"fish\":33}," +
"\"logBasket\":{\"logs\":34}," +
"\"lootingBag\":{\"looting\":35}," +
"\"seedBox\":{\"seedBox\":36}," +
"\"tackleBox\":{\"tackle\":37}," +
"\"forestryKit\":{\"forestry\":38}," +
"\"huntsmansKit\":{\"huntsman\":39}," +
"\"barbarianKnapsack\":{\"knapsack\":40}," +
"\"dizanasQuiverAmmo\":{\"quiver\":41}," +
"\"stashUnits\":{\"stash\":42}," +
"\"equipment\":{\"equipment\":43}}",
json
);

assertTrue(
json.contains(
"\"slayerRemaining\":10"
)
);

assertFalse(
json.contains(
"\"liveState.slayerRemaining\""
)
);
}
}