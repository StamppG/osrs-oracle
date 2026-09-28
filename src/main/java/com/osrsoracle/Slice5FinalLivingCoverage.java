package com.osrsoracle;

final class Slice5FinalLivingCoverage
{
    private Slice5FinalLivingCoverage()
    {
    }

    static String serialize(
            String deathObservedAt,
            String bankObservedAt,
            String seedVaultObservedAt,
            String gimStorageObservedAt,
            String coxPrivateStorageObservedAt,
            String coxSharedStorageObservedAt,
            String gravestoneStorageObservedAt,
            String deathsOfficeStorageObservedAt,
            String potionStorageObservedAt,
            String motherlodeSackObservedAt,
            String plankSackObservedAt,
            String herbSackObservedAt,
            String gemBagObservedAt,
            String gemSatchelObservedAt,
            String coalBagObservedAt,
            String fishBarrelObservedAt,
            String logBasketObservedAt,
            String lootingBagObservedAt,
            String seedBoxObservedAt,
            String tackleBoxObservedAt,
            String forestryKitObservedAt,
            String huntsmansKitObservedAt,
            String barbarianKnapsackObservedAt,
            String dizanasQuiverAmmoObservedAt,
            String stashUnitsObservedAt,
            String collectionLogObservedAt,
            int collectionLogPages
    )
    {
        if (
                deathObservedAt == null ||
                        deathObservedAt.isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "deathObservedAt"
            );
        }

        if (collectionLogPages < 0)
        {
            throw new IllegalArgumentException(
                    "collectionLogPages"
            );
        }

        return "{" +

                /*
                 * These are genuinely live factual domains collected at the
                 * death boundary. Account/accountType are deliberately absent
                 * because the death event owns account-mode context.
                 */
                "\"liveState\":{" +
                "\"status\":\"OBSERVED\"," +
                "\"observedAt\":" +
                jsonString(deathObservedAt) +
                "," +
                "\"datasets\":[" +
                "\"membership\"," +
                "\"slayer\"," +
                "\"combatAchievements\"," +
                "\"achievementDiaries\"," +
                "\"achievementDiaryTaskState\"," +
                "\"globalResourceCapabilityState\"," +
                "\"persistentStorageLiveItemState\"," +
                "\"skills\"," +
                "\"quests\"" +
                "]" +
                "}," +

                "\"bank\":" +
                observation(bankObservedAt) +
                "," +

                "\"seedVault\":" +
                observation(seedVaultObservedAt) +
                "," +

                "\"gimStorage\":" +
                observation(gimStorageObservedAt) +
                "," +

                "\"coxPrivateStorage\":" +
                observation(coxPrivateStorageObservedAt) +
                "," +

                "\"coxSharedStorage\":" +
                observation(coxSharedStorageObservedAt) +
                "," +

                "\"gravestoneStorage\":" +
                observation(gravestoneStorageObservedAt) +
                "," +

                "\"deathsOfficeStorage\":" +
                observation(deathsOfficeStorageObservedAt) +
                "," +

                "\"potionStorage\":" +
                observation(potionStorageObservedAt) +
                "," +

                "\"motherlodeSack\":" +
                observation(motherlodeSackObservedAt) +
                "," +

                "\"plankSack\":" +
                observation(plankSackObservedAt) +
                "," +

                "\"herbSack\":" +
                observation(herbSackObservedAt) +
                "," +

                "\"gemBag\":" +
                observation(gemBagObservedAt) +
                "," +

                "\"gemSatchel\":" +
                observation(gemSatchelObservedAt) +
                "," +

                "\"coalBag\":" +
                observation(coalBagObservedAt) +
                "," +

                "\"fishBarrel\":" +
                observation(fishBarrelObservedAt) +
                "," +

                "\"logBasket\":" +
                observation(logBasketObservedAt) +
                "," +

                "\"lootingBag\":" +
                observation(lootingBagObservedAt) +
                "," +

                "\"seedBox\":" +
                observation(seedBoxObservedAt) +
                "," +

                "\"tackleBox\":" +
                observation(tackleBoxObservedAt) +
                "," +

                "\"forestryKit\":" +
                observation(forestryKitObservedAt) +
                "," +

                "\"huntsmansKit\":" +
                observation(huntsmansKitObservedAt) +
                "," +

                "\"barbarianKnapsack\":" +
                observation(barbarianKnapsackObservedAt) +
                "," +

                "\"dizanasQuiverAmmo\":" +
                observation(dizanasQuiverAmmoObservedAt) +
                "," +

                "\"stashUnits\":" +
                observation(stashUnitsObservedAt) +
                "," +

                "\"collectionLogPages\":{" +
                "\"status\":" +
                jsonString(
                        collectionLogPages == 0
                                ? "NOT_OBSERVED"
                                : "PARTIAL"
                ) +
                "," +
                "\"observedAt\":" +
                jsonString(collectionLogObservedAt) +
                "," +
                "\"pagesObserved\":" +
                collectionLogPages +
                "}" +

                "}";
    }

    private static String observation(
            String observedAt
    )
    {
        if (observedAt == null)
        {
            return "{" +
                    "\"status\":\"NOT_OBSERVED\"," +
                    "\"observedAt\":null" +
                    "}";
        }

        return "{" +
                "\"status\":\"OBSERVED\"," +
                "\"observedAt\":" +
                jsonString(observedAt) +
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