package com.osrsoracle;

import java.util.UUID;

final class SnapshotEvidence
{
    private static final int SCHEMA_VERSION = 1;
    private static final String CLIENT_VERSION = "1.0";
    private static final String SOURCE = "RUNELITE_CLIENT";

    private SnapshotEvidence()
    {
    }

    static String newSessionId()
    {
        return UUID.randomUUID().toString();
    }

    static String collect(
            String observedAt,
            String trigger,
            String sessionId,
            long sequence,
            boolean inventoryObserved,
            boolean equipmentObserved,
            String bankObservedAt,
            String seedVaultObservedAt,
            String gimStorageObservedAt,
            String coxPrivateStorageObservedAt,
            String coxSharedStorageObservedAt,
            String gravestoneStorageObservedAt,
            String lootingBagObservedAt,
            String seedBoxObservedAt,
            String tackleBoxObservedAt,
            String forestryKitObservedAt,
            String huntsmansKitObservedAt,
            String barbarianKnapsackObservedAt,
            String dizanasQuiverAmmoObservedAt,
            String collectionLogObservedAt,
            int collectionLogPages,
            String collectionLogInstantObservedAt
    )
    {
        String mode =
                "CLOG_MANUAL".equals(trigger)
                        ? "MANUAL"
                        : "AUTOMATIC";

        return "{\"snapshotId\":" + jsonString(UUID.randomUUID().toString()) +
                ",\"observedAt\":" + jsonString(observedAt) +
                ",\"origin\":{\"source\":" + jsonString(SOURCE) +
                ",\"trigger\":" + jsonString(trigger) +
                ",\"mode\":" + jsonString(mode) + "}" +
                ",\"version\":{\"client\":" + jsonString(CLIENT_VERSION) +
                ",\"schema\":" + SCHEMA_VERSION + "}" +
                ",\"ordering\":{\"sessionId\":" + jsonString(sessionId) +
                ",\"sequence\":" + sequence + "}" +
                ",\"coverage\":{\"liveState\":{\"status\":\"OBSERVED\"" +
                ",\"observedAt\":" + jsonString(observedAt) +
                ",\"datasets\":[\"account\",\"accountType\",\"membership\"," +
                "\"slayer\",\"combatAchievements\",\"achievementDiaries\"," +
                "\"achievementDiaryTaskState\",\"globalResourceCapabilityState\"," +
                "\"persistentStorageLiveItemState\",\"skills\",\"quests\"]}" +
                ",\"inventory\":" +
                observation(
                        inventoryObserved ? "OBSERVED" : "NOT_OBSERVED",
                        inventoryObserved ? observedAt : null
                ) +
                ",\"equipment\":" +
                observation(
                        equipmentObserved ? "OBSERVED" : "NOT_OBSERVED",
                        equipmentObserved ? observedAt : null
                ) +
                ",\"bank\":" +
                observation(
                        bankObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        bankObservedAt
                ) +
                ",\"seedVault\":" +
                observation(
                        seedVaultObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        seedVaultObservedAt
                ) +
                ",\"gimStorage\":" +
                observation(
                        gimStorageObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        gimStorageObservedAt
                ) +
                ",\"coxPrivateStorage\":" +
                observation(
                        coxPrivateStorageObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        coxPrivateStorageObservedAt
                ) +
                ",\"coxSharedStorage\":" +
                observation(
                        coxSharedStorageObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        coxSharedStorageObservedAt
                ) +
                ",\"gravestoneStorage\":" +
                observation(
                        gravestoneStorageObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        gravestoneStorageObservedAt
                ) +
                ",\"lootingBag\":" +
                observation(
                        lootingBagObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        lootingBagObservedAt
                ) +
                ",\"seedBox\":" +
                observation(
                        seedBoxObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        seedBoxObservedAt
                ) +
                ",\"tackleBox\":" +
                observation(
                        tackleBoxObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        tackleBoxObservedAt
                ) +
                ",\"forestryKit\":" +
                observation(
                        forestryKitObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        forestryKitObservedAt
                ) +
                ",\"huntsmansKit\":" +
                observation(
                        huntsmansKitObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        huntsmansKitObservedAt
                ) +
                ",\"barbarianKnapsack\":" +
                observation(
                        barbarianKnapsackObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        barbarianKnapsackObservedAt
                ) +
                ",\"dizanasQuiverAmmo\":" +
                observation(
                        dizanasQuiverAmmoObservedAt == null ? "NOT_OBSERVED" : "OBSERVED",
                        dizanasQuiverAmmoObservedAt
                ) +
                ",\"collectionLogPages\":{\"status\":" +
                jsonString(
                        collectionLogPages == 0
                                ? "NOT_OBSERVED"
                                : "PARTIAL"
                ) +
                ",\"observedAt\":" + jsonString(collectionLogObservedAt) +
                ",\"pagesObserved\":" + collectionLogPages + "}" +
                ",\"collectionLogInstant\":" +
                observation(
                        collectionLogInstantObservedAt == null
                                ? "NOT_OBSERVED"
                                : "OBSERVED",
                        collectionLogInstantObservedAt
                ) +
                "}}";
    }

    private static String observation(
            String status,
            String observedAt
    )
    {
        return "{\"status\":" + jsonString(status) +
                ",\"observedAt\":" + jsonString(observedAt) + "}";
    }

    private static String jsonString(String value)
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
