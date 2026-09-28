package com.osrsoracle;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Slice5AckTest
{
    private final Gson gson =
            new Gson();

    @Test
    public void parsesOkAck()
    {
        Slice5Ack ack =
                Slice5Ack.parse(
                        gson,
                        "{" +
                                "\"success\":true," +
                                "\"account\":\"bluehelmsolo\"," +
                                "\"characterRecordId\":\"11111111-2222-4333-8444-555555555555\"," +
                                "\"eventIngest\":{" +
                                "\"status\":\"OK\"," +
                                "\"acknowledgedEventIds\":[" +
                                "\"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee\"" +
                                "]," +
                                "\"eventConflicts\":[]," +
                                "\"acknowledgedCoverageGapIds\":[" +
                                "\"12345678-1234-4234-8234-123456789abc\"" +
                                "]" +
                                "}" +
                                "}"
                );

        assertEquals(
                "11111111-2222-4333-8444-555555555555",
                ack.getCharacterRecordId()
        );

        assertEquals(
                "OK",
                ack.getStatus()
        );

        assertEquals(
                1,
                ack.getAcknowledgedEventIds().size()
        );

        assertEquals(
                0,
                ack.getEventConflicts().size()
        );

        assertEquals(
                1,
                ack.getAcknowledgedCoverageGapIds().size()
        );
    }

    @Test
    public void parsesPartialConflict()
    {
        Slice5Ack ack =
                Slice5Ack.parse(
                        gson,
                        "{" +
                                "\"success\":true," +
                                "\"characterRecordId\":\"11111111-2222-4333-8444-555555555555\"," +
                                "\"eventIngest\":{" +
                                "\"status\":\"PARTIAL_CONFLICT\"," +
                                "\"acknowledgedEventIds\":[" +
                                "\"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee\"" +
                                "]," +
                                "\"eventConflicts\":[{" +
                                "\"eventId\":\"99999999-8888-4777-8666-555555555555\"," +
                                "\"code\":\"IMMUTABLE_EVENT_CONFLICT\"" +
                                "}]," +
                                "\"acknowledgedCoverageGapIds\":[]" +
                                "}" +
                                "}"
                );

        assertEquals(
                1,
                ack.getAcknowledgedEventIds().size()
        );

        assertEquals(
                1,
                ack.getEventConflicts().size()
        );

        assertEquals(
                "99999999-8888-4777-8666-555555555555",
                ack.getEventConflicts()
                        .get(0)
                        .getEventId()
        );

        assertEquals(
                "IMMUTABLE_EVENT_CONFLICT",
                ack.getEventConflicts()
                        .get(0)
                        .getCode()
        );
    }

    @Test
    public void rejectsMalformedAck()
    {
        expectRejected(
                "{\"success\":true}"
        );
    }

    @Test
    public void rejectsConflictAlsoAcknowledged()
    {
        expectRejected(
                "{" +
                        "\"success\":true," +
                        "\"characterRecordId\":\"11111111-2222-4333-8444-555555555555\"," +
                        "\"eventIngest\":{" +
                        "\"status\":\"PARTIAL_CONFLICT\"," +
                        "\"acknowledgedEventIds\":[" +
                        "\"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee\"" +
                        "]," +
                        "\"eventConflicts\":[{" +
                        "\"eventId\":\"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee\"," +
                        "\"code\":\"IMMUTABLE_EVENT_CONFLICT\"" +
                        "}]," +
                        "\"acknowledgedCoverageGapIds\":[]" +
                        "}" +
                        "}"
        );
    }

    @Test
    public void rejectsPartialConflictWithoutConflict()
    {
        expectRejected(
                "{" +
                        "\"success\":true," +
                        "\"characterRecordId\":\"11111111-2222-4333-8444-555555555555\"," +
                        "\"eventIngest\":{" +
                        "\"status\":\"PARTIAL_CONFLICT\"," +
                        "\"acknowledgedEventIds\":[]," +
                        "\"eventConflicts\":[]," +
                        "\"acknowledgedCoverageGapIds\":[]" +
                        "}" +
                        "}"
        );
    }

    @Test
    public void rejectsNonV4Ids()
    {
        expectRejected(
                "{" +
                        "\"success\":true," +
                        "\"characterRecordId\":\"not-a-uuid\"," +
                        "\"eventIngest\":{" +
                        "\"status\":\"OK\"," +
                        "\"acknowledgedEventIds\":[]," +
                        "\"eventConflicts\":[]," +
                        "\"acknowledgedCoverageGapIds\":[]" +
                        "}" +
                        "}"
        );
    }

    private void expectRejected(
            String json
    )
    {
        try
        {
            Slice5Ack.parse(
                    gson,
                    json
            );

            fail(
                    "Expected malformed ACK rejection"
            );
        }
        catch (IllegalArgumentException expected)
        {
            // Expected.
        }
    }
}