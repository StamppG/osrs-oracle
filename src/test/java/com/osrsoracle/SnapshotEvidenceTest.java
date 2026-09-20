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
}
