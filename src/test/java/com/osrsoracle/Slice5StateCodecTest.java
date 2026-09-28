package com.osrsoracle;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Slice5StateCodecTest
{
    @Test
    public void roundTripPreservesPersistentState()
            throws Exception
    {
        Slice5OutboxState original =
                new Slice5OutboxState();

        String pendingId =
                "80000000-0000-4000-8000-000000000001";

        original.captureEvent(
                "Blue Helm",
                pendingId,
                "CHARACTER_DEATH",
                "2026-09-27T14:00:00Z",
                "{\"eventId\":\"pending\",\"unicode\":\"owl-ðŸ¦‰\"}"
        );

        original.suppressEvent(
                "Blue Helm",
                pendingId
        );

        Slice5OutboxState.CoverageGap closedGap =
                original.ensureOpenGap(
                        "Blue Helm",
                        "2026-09-27T14:00:01Z",
                        Slice5OutboxState.GapReason.OUTBOX_CORRUPTION
                );

        closedGap.close(
                "2026-09-27T14:00:02Z"
        );

        original.ensureOpenGap(
                "Red Helm",
                "2026-09-27T14:01:00Z",
                Slice5OutboxState.GapReason.OUTBOX_CORRUPTION
        );

        String quarantineId =
                "80000000-0000-4000-8000-000000000002";

        original.captureEvent(
                "Red Helm",
                quarantineId,
                "HARDCORE_STATUS_LOSS",
                "2026-09-27T14:02:00Z",
                "{\"eventId\":\"quarantine\"}"
        );

        original.quarantineConflict(
                "Red Helm",
                quarantineId,
                "IMMUTABLE_EVENT_CONFLICT",
                "2026-09-27T14:02:01Z"
        );

        Slice5StateCodec codec =
                new Slice5StateCodec();

        byte[] encoded =
                codec.encode(original);

        Slice5OutboxState restored =
                codec.decode(encoded);

        assertEquals(
                1,
                restored.pendingCount("Blue Helm")
        );

        assertTrue(
                restored.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        assertEquals(
                1,
                restored.coverageGapCount("Blue Helm")
        );

        assertEquals(
                1,
                restored.coverageGapCount("Red Helm")
        );

        assertNotNull(
                restored.getOpenGap("Red Helm")
        );

        assertEquals(
                Slice5OutboxState.GapReason.OUTBOX_CORRUPTION,
                restored.getOpenGap("Red Helm")
                        .getReason()
        );

        assertEquals(
                1,
                restored.quarantineCount()
        );

        Slice5OutboxState.QuarantineRecord record =
                restored.quarantineSnapshot().get(0);

        assertEquals(
                quarantineId,
                record.getEventId()
        );

        assertEquals(
                "HARDCORE_STATUS_LOSS",
                record.getEventType()
        );

        assertFalse(
                record.toJson().contains(
                        "quarantine\"}"
                )
        );
    }

    @Test
    public void truncatedStateIsRejected()
            throws Exception
    {
        Slice5OutboxState state =
                new Slice5OutboxState();

        state.captureEvent(
                "Blue Helm",
                "80000000-0000-4000-8000-000000000003",
                "CHARACTER_DEATH",
                "2026-09-27T14:03:00Z",
                "{\"eventId\":\"truncate-me\"}"
        );

        Slice5StateCodec codec =
                new Slice5StateCodec();

        byte[] encoded =
                codec.encode(state);

        byte[] truncated =
                Arrays.copyOf(
                        encoded,
                        encoded.length - 3
                );

        try
        {
            codec.decode(truncated);
            fail("Expected IOException");
        }
        catch (IOException expected)
        {
            assertTrue(
                    expected.getMessage()
                            .contains("truncated")
            );
        }
    }

    @Test
    public void trailingBytesAreRejected()
            throws Exception
    {
        Slice5StateCodec codec =
                new Slice5StateCodec();

        byte[] encoded =
                codec.encode(
                        new Slice5OutboxState()
                );

        byte[] corrupt =
                Arrays.copyOf(
                        encoded,
                        encoded.length + 1
                );

        corrupt[corrupt.length - 1] = 7;

        try
        {
            codec.decode(corrupt);
            fail("Expected IOException");
        }
        catch (IOException expected)
        {
            assertTrue(
                    expected.getMessage()
                            .contains("trailing")
            );
        }
    }
}