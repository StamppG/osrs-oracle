package com.osrsoracle;

final class Slice5FinalLivingStateCandidate
{
    private Slice5FinalLivingStateCandidate()
    {
    }

    static String serialize(
            SnapshotEnvelopeSerializer.Parts parts,
            String residualCoverageJson
    )
    {
        if (parts == null)
        {
            throw new IllegalArgumentException("parts");
        }

        if (parts.liveState == null)
        {
            throw new IllegalArgumentException("liveState");
        }

        if (
                residualCoverageJson == null ||
                        residualCoverageJson.isBlank() ||
                        "null".equals(
                                residualCoverageJson.trim()
                        )
        )
        {
            throw new IllegalArgumentException(
                    "residualCoverageJson"
            );
        }

        SnapshotLiveStateCollector.State liveState =
                parts.liveState;

        return "{" +
                "\"schema\":2," +
                "\"projection\":{" +

                "\"membershipActive\":" +
                liveState.membershipActiveJson +
                "," +

                "\"membershipDaysRemaining\":" +
                liveState.membershipDaysJson +
                "," +

                "\"slayerTask\":" +
                jsonString(liveState.slayerTask) +
                "," +

                "\"slayerRemaining\":" +
                liveState.slayerRemaining +
                "," +

                "\"combatAchievements\":{" +
                "\"easy\":" +
                liveState.caEasy +
                "," +
                "\"medium\":" +
                liveState.caMedium +
                "," +
                "\"hard\":" +
                liveState.caHard +
                "," +
                "\"elite\":" +
                liveState.caElite +
                "," +
                "\"master\":" +
                liveState.caMaster +
                "," +
                "\"grandmaster\":" +
                liveState.caGrandmaster +
                "," +
                "\"completedTaskIds\":" +
                liveState.caCompletedIdsJson +
                "}," +

                "\"achievementDiaries\":" +
                liveState.diaryJson +
                "," +

                "\"achievementDiaryTaskState\":" +
                parts.diaryTaskStateJson +
                "," +

                "\"globalResourceCapabilityState\":" +
                parts.globalResourceCapabilityStateJson +
                "," +

                "\"persistentStorageLiveItemState\":" +
                parts.persistentStorageLiveItemStateJson +
                "," +

                "\"collectionLog\":" +
                parts.collectionLogJson +
                "," +

                "\"skills\":" +
                liveState.skillsJson +
                "," +

                "\"quests\":" +
                liveState.questsJson +
                "," +

                "\"bank\":" +
                parts.bankJson +
                "," +

                "\"seedVault\":" +
                parts.seedVaultJson +
                "," +

                "\"gimStorage\":" +
                parts.gimStorageJson +
                "," +

                "\"coxPrivateStorage\":" +
                parts.coxPrivateStorageJson +
                "," +

                "\"coxSharedStorage\":" +
                parts.coxSharedStorageJson +
                "," +

                "\"gravestoneStorage\":" +
                parts.gravestoneStorageJson +
                "," +

                "\"deathsOfficeStorage\":" +
                parts.deathsOfficeStorageJson +
                "," +

                "\"potionStorage\":" +
                parts.potionStorageJson +
                "," +

                "\"motherlodeSack\":" +
                parts.motherlodeSackJson +
                "," +

                "\"plankSack\":" +
                parts.plankSackJson +
                "," +

                "\"herbSack\":" +
                parts.herbSackJson +
                "," +

                "\"gemBag\":" +
                parts.gemBagJson +
                "," +

                "\"gemSatchel\":" +
                parts.gemSatchelJson +
                "," +

                "\"coalBag\":" +
                parts.coalBagJson +
                "," +

                "\"fishBarrel\":" +
                parts.fishBarrelJson +
                "," +

                "\"logBasket\":" +
                parts.logBasketJson +
                "," +

                "\"lootingBag\":" +
                parts.lootingBagJson +
                "," +

                "\"seedBox\":" +
                parts.seedBoxJson +
                "," +

                "\"tackleBox\":" +
                parts.tackleBoxJson +
                "," +

                "\"forestryKit\":" +
                parts.forestryKitJson +
                "," +

                "\"huntsmansKit\":" +
                parts.huntsmansKitJson +
                "," +

                "\"barbarianKnapsack\":" +
                parts.barbarianKnapsackJson +
                "," +

                "\"dizanasQuiverAmmo\":" +
                parts.dizanasQuiverAmmoJson +
                "," +

                "\"stashUnits\":" +
                parts.stashUnitsJson +

                "}," +

                "\"coverage\":" +
                residualCoverageJson +

                "}";
    }

    private static String jsonString(
            String value
    )
    {
        if (value == null)
        {
            return "null";
        }

        return "\"" +
                value
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r") +
                "\"";
    }
}