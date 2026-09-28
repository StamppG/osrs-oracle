package com.osrsoracle;

import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Slice5OutboxStateTest
{
    @Test
    public void batchesOldestEventsPerAccount()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        for (int i = 0; i < 20; i++)
        {
            String id =
                    String.format(
                            "10000000-0000-4000-8000-%012d",
                            i
                    );

            assertEquals(
                    Slice5OutboxState.CaptureStatus.QUEUED,
                    state.captureEvent(
                            "Blue Helm",
                            id,
                            "CHARACTER_DEATH",
                            "2026-09-27T12:00:00Z",
                            "{\"eventId\":\"" + id + "\"}"
                    )
            );
        }

        Slice5OutboxState.Batch batch =
                state.oldestBatch("BlueHelm");

        assertEquals(16, batch.getEvents().size());
        assertEquals(
                "10000000-0000-4000-8000-000000000000",
                batch.getEventIds().get(0)
        );
        assertEquals(
                "10000000-0000-4000-8000-000000000015",
                batch.getEventIds().get(15)
        );
    }

    @Test
    public void accountPartitionsDoNotCrossSend()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        state.captureEvent(
                "Blue Helm",
                "10000000-0000-4000-8000-000000000001",
                "CHARACTER_DEATH",
                "2026-09-27T12:00:00Z",
                "{\"eventId\":\"blue\"}"
        );

        state.captureEvent(
                "Red Helm",
                "10000000-0000-4000-8000-000000000002",
                "CHARACTER_DEATH",
                "2026-09-27T12:00:01Z",
                "{\"eventId\":\"red\"}"
        );

        assertEquals(
                Collections.singletonList(
                        "10000000-0000-4000-8000-000000000001"
                ),
                state.oldestBatch("Blue Helm").getEventIds()
        );

        assertEquals(
                Collections.singletonList(
                        "10000000-0000-4000-8000-000000000002"
                ),
                state.oldestBatch("Red Helm").getEventIds()
        );
    }

    @Test
    public void eventTooLargeCreatesDegradedGap()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String huge =
                "{\"payload\":\"" +
                        "x".repeat(
                                Slice5OutboxState.MAX_SINGLE_EVENT_BYTES
                        ) +
                        "\"}";

        assertEquals(
                Slice5OutboxState.CaptureStatus.EVENT_TOO_LARGE,
                state.captureEvent(
                        "Blue Helm",
                        "10000000-0000-4000-8000-000000000001",
                        "CHARACTER_DEATH",
                        "2026-09-27T12:00:00Z",
                        huge
                )
        );

        assertEquals(0, state.pendingCount());

        Slice5OutboxState.CoverageGap gap =
                state.getOpenGap("Blue Helm");

        assertNotNull(gap);
        assertEquals(
                Slice5OutboxState.GapReason.EVENT_TOO_LARGE,
                gap.getReason()
        );

        assertTrue(
                state.eventCoverageJson(
                        "Blue Helm",
                        "2026-09-27T12:00:01Z"
                ).contains("\"status\":\"DEGRADED\"")
        );
    }

    @Test
    public void successfulLaterCaptureClosesTooLargeGap()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String huge =
                "{\"payload\":\"" +
                        "x".repeat(
                                Slice5OutboxState.MAX_SINGLE_EVENT_BYTES
                        ) +
                        "\"}";

        state.captureEvent(
                "Blue Helm",
                "10000000-0000-4000-8000-000000000001",
                "CHARACTER_DEATH",
                "2026-09-27T12:00:00Z",
                huge
        );

        Slice5OutboxState.CoverageGap gap =
                state.getOpenGap("Blue Helm");

        assertNotNull(gap);

        state.captureEvent(
                "Blue Helm",
                "10000000-0000-4000-8000-000000000002",
                "CHARACTER_DEATH",
                "2026-09-27T12:00:02Z",
                "{\"eventId\":\"small\"}"
        );

        assertNull(state.getOpenGap("Blue Helm"));
        assertEquals(
                "2026-09-27T12:00:02Z",
                gap.getEndedAt()
        );

        assertTrue(
                state.eventCoverageJson(
                        "Blue Helm",
                        "2026-09-27T12:00:03Z"
                ).contains("\"status\":\"COMPLETE\"")
        );

        assertTrue(
                state.coverageGapsJson("Blue Helm")
                        .contains(
                                "\"endedAt\":\"2026-09-27T12:00:02Z\""
                        )
        );
    }

    @Test
    public void fullOutboxCreatesGapAndAckRecoveryClosesIt()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        for (
                int i = 0;
                i < Slice5OutboxState.MAX_PENDING_EVENTS;
                i++
        )
        {
            String id =
                    String.format(
                            "20000000-0000-4000-8000-%012d",
                            i
                    );

            assertEquals(
                    Slice5OutboxState.CaptureStatus.QUEUED,
                    state.captureEvent(
                            "Blue Helm",
                            id,
                            "CHARACTER_DEATH",
                            "2026-09-27T12:00:00Z",
                            "{\"eventId\":\"" + id + "\"}"
                    )
            );
        }

        assertEquals(
                Slice5OutboxState.CaptureStatus.OUTBOX_CAPACITY,
                state.captureEvent(
                        "Blue Helm",
                        "30000000-0000-4000-8000-000000000001",
                        "CHARACTER_DEATH",
                        "2026-09-27T12:00:01Z",
                        "{\"eventId\":\"overflow\"}"
                )
        );

        Slice5OutboxState.CoverageGap gap =
                state.getOpenGap("Blue Helm");

        assertNotNull(gap);
        assertEquals(
                Slice5OutboxState.GapReason.OUTBOX_CAPACITY,
                gap.getReason()
        );

        state.acknowledgeEvents(
                "Blue Helm",
                Collections.singletonList(
                        "20000000-0000-4000-8000-000000000000"
                ),
                "2026-09-27T12:00:02Z"
        );

        assertNull(state.getOpenGap("Blue Helm"));
        assertEquals(
                "2026-09-27T12:00:02Z",
                gap.getEndedAt()
        );
    }

    @Test
    public void successfulCaptureThatRefillsOutboxDoesNotClaimCapacityRecovery()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        for (int i = 0; i < Slice5OutboxState.MAX_PENDING_EVENTS - 1; i++)
        {
            String id =
                    String.format(
                            "86000000-0000-4000-8000-%012d",
                            i
                    );

            assertEquals(
                    Slice5OutboxState.CaptureStatus.QUEUED,
                    state.captureEvent(
                            "Red Helm",
                            id,
                            "CHARACTER_DEATH",
                            "2026-09-27T15:30:00Z",
                            "{\"eventId\":\"" + id + "\"}"
                    )
            );
        }

        Slice5OutboxState.CoverageGap gap =
                state.ensureOpenGap(
                        "Blue Helm",
                        "2026-09-27T15:30:01Z",
                        Slice5OutboxState.GapReason.OUTBOX_CAPACITY
                );

        assertEquals(
                Slice5OutboxState.CaptureStatus.QUEUED,
                state.captureEvent(
                        "Red Helm",
                        "87000000-0000-4000-8000-000000000001",
                        "CHARACTER_DEATH",
                        "2026-09-27T15:30:02Z",
                        "{\"eventId\":\"fills-final-slot\"}"
                )
        );

        assertNotNull(
                state.getOpenGap("Blue Helm")
        );

        assertNull(
                gap.getEndedAt()
        );
    }

    @Test
    public void otherAccountAckClosesGlobalOutboxCapacityGap()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String firstRedEventId =
                "81000000-0000-4000-8000-000000000000";

        for (
                int i = 0;
                i < Slice5OutboxState.MAX_PENDING_EVENTS;
                i++
        )
        {
            String id =
                    String.format(
                            "81000000-0000-4000-8000-%012d",
                            i
                    );

            assertEquals(
                    Slice5OutboxState.CaptureStatus.QUEUED,
                    state.captureEvent(
                            "Red Helm",
                            id,
                            "CHARACTER_DEATH",
                            "2026-09-27T14:00:00Z",
                            "{\"eventId\":\"" + id + "\"}"
                    )
            );
        }

        assertEquals(
                Slice5OutboxState.CaptureStatus.OUTBOX_CAPACITY,
                state.captureEvent(
                        "Blue Helm",
                        "82000000-0000-4000-8000-000000000001",
                        "CHARACTER_DEATH",
                        "2026-09-27T14:00:01Z",
                        "{\"eventId\":\"blue-overflow\"}"
                )
        );

        Slice5OutboxState.CoverageGap blueGap =
                state.getOpenGap("Blue Helm");

        assertNotNull(blueGap);
        assertEquals(
                Slice5OutboxState.GapReason.OUTBOX_CAPACITY,
                blueGap.getReason()
        );

        state.acknowledgeEvents(
                "Red Helm",
                Collections.singletonList(
                        firstRedEventId
                ),
                "2026-09-27T14:00:02Z"
        );

        assertNull(
                state.getOpenGap("Blue Helm")
        );

        assertEquals(
                "2026-09-27T14:00:02Z",
                blueGap.getEndedAt()
        );
    }

    @Test
    public void otherAccountQuarantineClosesGlobalOutboxCapacityGap()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String firstRedEventId =
                "83000000-0000-4000-8000-000000000000";

        for (
                int i = 0;
                i < Slice5OutboxState.MAX_PENDING_EVENTS;
                i++
        )
        {
            String id =
                    String.format(
                            "83000000-0000-4000-8000-%012d",
                            i
                    );

            assertEquals(
                    Slice5OutboxState.CaptureStatus.QUEUED,
                    state.captureEvent(
                            "Red Helm",
                            id,
                            "CHARACTER_DEATH",
                            "2026-09-27T14:01:00Z",
                            "{\"eventId\":\"" + id + "\"}"
                    )
            );
        }

        assertEquals(
                Slice5OutboxState.CaptureStatus.OUTBOX_CAPACITY,
                state.captureEvent(
                        "Blue Helm",
                        "84000000-0000-4000-8000-000000000001",
                        "CHARACTER_DEATH",
                        "2026-09-27T14:01:01Z",
                        "{\"eventId\":\"blue-overflow\"}"
                )
        );

        Slice5OutboxState.CoverageGap blueGap =
                state.getOpenGap("Blue Helm");

        assertNotNull(blueGap);

        assertEquals(
                Slice5OutboxState.QuarantineStatus.QUARANTINED,
                state.quarantineConflict(
                        "Red Helm",
                        firstRedEventId,
                        "IMMUTABLE_EVENT_CONFLICT",
                        "2026-09-27T14:01:02Z"
                )
        );

        assertNull(
                state.getOpenGap("Blue Helm")
        );

        assertEquals(
                "2026-09-27T14:01:02Z",
                blueGap.getEndedAt()
        );
    }

    @Test
    public void closedGapPersistsUntilAcknowledged()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        Slice5OutboxState.CoverageGap gap =
                state.ensureOpenGap(
                        "Blue Helm",
                        "2026-09-27T12:00:00Z",
                        Slice5OutboxState.GapReason.OUTBOX_CORRUPTION
                );

        gap.close(
                "2026-09-27T12:00:05Z"
        );

        assertEquals(
                1,
                state.coverageGapCount("Blue Helm")
        );

        state.acknowledgeCoverageGaps(
                "Blue Helm",
                Collections.singletonList(
                        gap.getGapId()
                )
        );

        assertEquals(
                0,
                state.coverageGapCount("Blue Helm")
        );
    }

    @Test
    public void suppressedConflictIsNeverRebatched()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String eventId =
                "10000000-0000-4000-8000-000000000001";

        state.captureEvent(
                "Blue Helm",
                eventId,
                "CHARACTER_DEATH",
                "2026-09-27T12:00:00Z",
                "{\"eventId\":\"conflict\"}"
        );

        assertFalse(
                state.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        state.suppressEvent(
                "Blue Helm",
                eventId
        );

        assertTrue(
                state.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        assertEquals(
                1,
                state.pendingCount("Blue Helm")
        );
    }

    @Test
    public void successfulConflictQuarantineStoresOnlyMetadataAndRemovesPayload()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String eventId =
                "40000000-0000-4000-8000-000000000001";

        String payload =
                "{\"eventId\":\"" +
                        eventId +
                        "\",\"secretPayload\":\"must-not-enter-quarantine\"}";

        state.captureEvent(
                "Blue Helm",
                eventId,
                "CHARACTER_DEATH",
                "2026-09-27T12:00:00Z",
                payload
        );

        assertEquals(
                Slice5OutboxState.QuarantineStatus.QUARANTINED,
                state.quarantineConflict(
                        "Blue Helm",
                        eventId,
                        "IMMUTABLE_EVENT_CONFLICT",
                        "2026-09-27T12:00:05Z"
                )
        );

        assertEquals(
                0,
                state.pendingCount("Blue Helm")
        );

        assertEquals(
                1,
                state.quarantineCount()
        );

        Slice5OutboxState.QuarantineRecord record =
                state.quarantineSnapshot().get(0);

        assertEquals(
                eventId,
                record.getEventId()
        );

        assertEquals(
                "CHARACTER_DEATH",
                record.getEventType()
        );

        assertEquals(
                "IMMUTABLE_EVENT_CONFLICT",
                record.getErrorCode()
        );

        assertEquals(
                "2026-09-27T12:00:00Z",
                record.getFirstObservedAt()
        );

        assertEquals(
                "2026-09-27T12:00:05Z",
                record.getQuarantinedAt()
        );

        assertFalse(
                record.toJson().contains(
                        "secretPayload"
                )
        );

        assertFalse(
                record.toJson().contains(
                        "must-not-enter-quarantine"
                )
        );
    }

    @Test
    public void quarantinedEventCannotReenterPendingOutbox()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String eventId =
                "85000000-0000-4000-8000-000000000001";

        assertEquals(
                Slice5OutboxState.CaptureStatus.QUEUED,
                state.captureEvent(
                        "Blue Helm",
                        eventId,
                        "CHARACTER_DEATH",
                        "2026-09-27T15:00:00Z",
                        "{\"eventId\":\"original\"}"
                )
        );

        assertEquals(
                Slice5OutboxState.QuarantineStatus.QUARANTINED,
                state.quarantineConflict(
                        "Blue Helm",
                        eventId,
                        "IMMUTABLE_EVENT_CONFLICT",
                        "2026-09-27T15:00:01Z"
                )
        );

        assertEquals(
                0,
                state.pendingCount("Blue Helm")
        );

        assertEquals(
                Slice5OutboxState.CaptureStatus.ALREADY_QUEUED,
                state.captureEvent(
                        "Blue Helm",
                        eventId,
                        "CHARACTER_DEATH",
                        "2026-09-27T15:00:00Z",
                        "{\"eventId\":\"attempted-reintroduction\"}"
                )
        );

        assertEquals(
                0,
                state.pendingCount("Blue Helm")
        );

        assertEquals(
                1,
                state.quarantineCount()
        );

        assertTrue(
                state.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );
    }

    @Test
    public void quarantineConflictIsIdempotent()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String eventId =
                "40000000-0000-4000-8000-000000000002";

        state.captureEvent(
                "Blue Helm",
                eventId,
                "CHARACTER_DEATH",
                "2026-09-27T12:01:00Z",
                "{\"eventId\":\"same\"}"
        );

        assertEquals(
                Slice5OutboxState.QuarantineStatus.QUARANTINED,
                state.quarantineConflict(
                        "Blue Helm",
                        eventId,
                        "IMMUTABLE_EVENT_CONFLICT",
                        "2026-09-27T12:01:05Z"
                )
        );

        assertEquals(
                Slice5OutboxState.QuarantineStatus.ALREADY_QUARANTINED,
                state.quarantineConflict(
                        "Blue Helm",
                        eventId,
                        "IMMUTABLE_EVENT_CONFLICT",
                        "2026-09-27T12:01:10Z"
                )
        );

        assertEquals(
                1,
                state.quarantineCount()
        );
    }

    @Test
    public void quarantineCapacitySuppressesConflictWithoutLosingPayload()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        for (
                int i = 0;
                i < Slice5OutboxState.MAX_QUARANTINE_RECORDS;
                i++
        )
        {
            String eventId =
                    String.format(
                            "50000000-0000-4000-8000-%012d",
                            i
                    );

            state.captureEvent(
                    "Blue Helm",
                    eventId,
                    "CHARACTER_DEATH",
                    "2026-09-27T12:02:00Z",
                    "{\"eventId\":\"" +
                            eventId +
                            "\"}"
            );

            assertEquals(
                    Slice5OutboxState.QuarantineStatus.QUARANTINED,
                    state.quarantineConflict(
                            "Blue Helm",
                            eventId,
                            "IMMUTABLE_EVENT_CONFLICT",
                            "2026-09-27T12:02:05Z"
                    )
            );
        }

        assertEquals(
                Slice5OutboxState.MAX_QUARANTINE_RECORDS,
                state.quarantineCount()
        );

        String overflowId =
                "60000000-0000-4000-8000-000000000001";

        state.captureEvent(
                "Blue Helm",
                overflowId,
                "CHARACTER_DEATH",
                "2026-09-27T12:03:00Z",
                "{\"eventId\":\"overflow-conflict\"}"
        );

        assertEquals(
                Slice5OutboxState.QuarantineStatus.QUARANTINE_CAPACITY,
                state.quarantineConflict(
                        "Blue Helm",
                        overflowId,
                        "IMMUTABLE_EVENT_CONFLICT",
                        "2026-09-27T12:03:05Z"
                )
        );

        /*
         * The conflicting payload remains represented locally.
         */
        assertEquals(
                1,
                state.pendingCount("Blue Helm")
        );

        /*
         * But it may never automatically resend while quarantine persistence
         * is unavailable.
         */
        assertTrue(
                state.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        Slice5OutboxState.CoverageGap gap =
                state.getOpenGap("Blue Helm");

        assertNotNull(gap);

        assertEquals(
                Slice5OutboxState.GapReason.QUARANTINE_CAPACITY,
                gap.getReason()
        );

        assertTrue(
                state.eventCoverageJson(
                        "Blue Helm",
                        "2026-09-27T12:03:06Z"
                ).contains(
                        "\"status\":\"DEGRADED\""
                )
        );
    }

    @Test
    public void copyPreservesRestartRelevantState()
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        String pendingId =
                "70000000-0000-4000-8000-000000000001";

        state.captureEvent(
                "Blue Helm",
                pendingId,
                "CHARACTER_DEATH",
                "2026-09-27T13:00:00Z",
                "{\"eventId\":\"pending\"}"
        );

        state.suppressEvent(
                "Blue Helm",
                pendingId
        );

        Slice5OutboxState.CoverageGap gap =
                state.ensureOpenGap(
                        "Blue Helm",
                        "2026-09-27T13:00:01Z",
                        Slice5OutboxState.GapReason.OUTBOX_CORRUPTION
                );

        gap.close(
                "2026-09-27T13:00:02Z"
        );

        String quarantineId =
                "70000000-0000-4000-8000-000000000002";

        state.captureEvent(
                "Red Helm",
                quarantineId,
                "CHARACTER_DEATH",
                "2026-09-27T13:01:00Z",
                "{\"eventId\":\"quarantine\"}"
        );

        state.quarantineConflict(
                "Red Helm",
                quarantineId,
                "IMMUTABLE_EVENT_CONFLICT",
                "2026-09-27T13:01:01Z"
        );

        Slice5OutboxState copy =
                state.copy();

        assertEquals(
                1,
                copy.pendingCount("Blue Helm")
        );

        assertTrue(
                copy.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        assertEquals(
                1,
                copy.coverageGapCount("Blue Helm")
        );

        assertEquals(
                "2026-09-27T13:00:02Z",
                copy.coverageGapSnapshot()
                        .get(0)
                        .getEndedAt()
        );

        assertEquals(
                1,
                copy.quarantineCount()
        );

        assertEquals(
                quarantineId,
                copy.quarantineSnapshot()
                        .get(0)
                        .getEventId()
        );
    }
}
