package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SnapshotEvidenceTest
{
    @Test
    public void reportsObservedAndMissingCoverageSeparately()
    {
        String json =
                SnapshotEvidence.collect(
                        "2026-09-20T20:00:00Z",
                        "HEARTBEAT",
                        "session-1",
                        7,
                        true,
                        true,
                        "2026-09-20T19:55:00Z",
                        null,
                        "2026-09-20T19:57:00Z",
                        "2026-09-20T19:58:00Z",
                        "2026-09-20T19:59:00Z",
                        "2026-09-20T19:59:50Z",
                        "2026-09-20T19:59:50.250Z",
                        "2026-09-20T19:59:50.500Z",
                        "2026-09-20T19:59:50.750Z",
                        "2026-09-20T19:59:51Z",
                        "2026-09-20T19:59:52Z",
                        "2026-09-20T19:59:53Z",
                        "2026-09-20T19:59:54Z",
                        "2026-09-20T19:59:55Z",
                        "2026-09-20T19:59:56Z",
                        "2026-09-20T19:59:57Z",
                        "2026-09-20T19:50:00Z",
                        3,
                        null
                );

        assertTrue(json.contains("\"source\":\"RUNELITE_CLIENT\""));
        assertTrue(json.contains("\"schema\":1"));
        assertTrue(json.contains("\"sessionId\":\"session-1\""));
        assertTrue(json.contains("\"sequence\":7"));
        assertTrue(json.contains(
                "\"datasets\":[\"account\",\"accountType\",\"membership\""
        ));
        assertTrue(json.contains(
                "\"achievementDiaryTaskState\",\"globalResourceCapabilityState\"," +
                        "\"persistentStorageLiveItemState\",\"skills\",\"quests\"]"
        ));
        assertTrue(json.contains(
                "\"bank\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:55:00Z\"}"
        ));
        assertTrue(json.contains(
                "\"seedVault\":{\"status\":\"NOT_OBSERVED\"," +
                        "\"observedAt\":null}"
        ));
        assertTrue(json.contains(
                "\"gimStorage\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:57:00Z\"}"
        ));
        assertTrue(json.contains(
                "\"coxPrivateStorage\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:58:00Z\"}"
        ));
        assertTrue(json.contains(
                "\"coxSharedStorage\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:00Z\"}"
        ));
        assertTrue(json.contains(
                "\"gravestoneStorage\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:50Z\"}"
        ));
        assertTrue(json.contains(
                "\"deathsOfficeStorage\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:50.250Z\"}"
        ));
        assertTrue(json.contains(
                "\"potionStorage\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:50.500Z\"}"
        ));
        assertTrue(json.contains(
                "\"motherlodeSack\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:50.750Z\"}"
        ));
        assertTrue(json.contains(
                "\"lootingBag\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:51Z\"}"
        ));
        assertTrue(json.contains(
                "\"seedBox\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:52Z\"}"
        ));
        assertTrue(json.contains(
                "\"tackleBox\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:53Z\"}"
        ));
        assertTrue(json.contains(
                "\"forestryKit\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:54Z\"}"
        ));
        assertTrue(json.contains(
                "\"huntsmansKit\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:55Z\"}"
        ));
        assertTrue(json.contains(
                "\"barbarianKnapsack\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:56Z\"}"
        ));
        assertTrue(json.contains(
                "\"dizanasQuiverAmmo\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T19:59:57Z\"}"
        ));
        assertTrue(json.contains(
                "\"stashUnits\":{\"status\":\"NOT_OBSERVED\"," +
                        "\"observedAt\":null}"
        ));
        assertTrue(json.contains(
                "\"collectionLogPages\":{\"status\":\"PARTIAL\"," +
                        "\"observedAt\":\"2026-09-20T19:50:00Z\"," +
                        "\"pagesObserved\":3}"
        ));
    }

    @Test
    public void marksExplicitManualCollection()
    {
        String json =
                SnapshotEvidence.collect(
                        "2026-09-20T20:00:00Z",
                        "CLOG_MANUAL",
                        "session-2",
                        1,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        "2026-09-20T20:00:00Z"
                );

        assertTrue(json.contains("\"mode\":\"MANUAL\""));
        assertTrue(json.contains(
                "\"inventory\":{\"status\":\"NOT_OBSERVED\"," +
                        "\"observedAt\":null}"
        ));
        assertTrue(json.contains(
                "\"collectionLogPages\":{\"status\":\"NOT_OBSERVED\"," +
                        "\"observedAt\":null,\"pagesObserved\":0}"
        ));
        assertTrue(json.contains(
                "\"collectionLogInstant\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T20:00:00Z\"}"
        ));
    }
    @Test
    public void reportsManualStashCoverage()
    {
        String json =
                SnapshotEvidence.collect(
                        "2026-09-20T21:00:00Z",
                        "STASH_MANUAL",
                        "session-stash",
                        9,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        null,
                        "2026-09-20T20:59:59Z",
                        null
                );

        assertTrue(json.contains(
                "\"trigger\":\"STASH_MANUAL\""
        ));

        assertTrue(json.contains(
                "\"mode\":\"MANUAL\""
        ));

        assertTrue(json.contains(
                "\"stashUnits\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-20T20:59:59Z\"}"
        ));
    }

    @Test
    public void reportsPlankSackCoverageIndependently()
    {
        String json =
                SnapshotEvidence.collect(
                        "2026-09-23T05:00:00Z",
                        "HEARTBEAT",
                        "session-plank",
                        1,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        null,
                        null,
                        "2026-09-23T04:59:59Z"
                );

        assertTrue(json.contains(
                "\"plankSack\":{\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-23T04:59:59Z\"}"
        ));
    }
}
