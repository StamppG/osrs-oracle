package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Slice5HardcoreStatusLossEventTest
{
    @Test
    public void serializesOnlyFrozenHcimTransition()
    {
        String json =
                Slice5HardcoreStatusLossEvent.serialize(
                        "12345678-1234-4234-8234-123456789abc",
                        "2026-09-27T21:00:00Z",
                        "87654321-4321-4321-8321-cba987654321",
                        14,
                        "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
                );

        assertEquals(
                "{" +
                        "\"eventId\":\"12345678-1234-4234-8234-123456789abc\"," +
                        "\"type\":\"HARDCORE_STATUS_LOSS\"," +
                        "\"observedAt\":\"2026-09-27T21:00:00Z\"," +
                        "\"origin\":{" +
                        "\"source\":\"ACCOUNT_TYPE_VARBIT_CHANGED\"," +
                        "\"sessionId\":\"87654321-4321-4321-8321-cba987654321\"," +
                        "\"sequence\":14}," +
                        "\"deathEventId\":\"aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee\"," +
                        "\"transition\":{" +
                        "\"preAccountType\":\"HARDCORE_IRONMAN\"," +
                        "\"preRawAccountType\":3," +
                        "\"postAccountType\":\"IRONMAN\"," +
                        "\"postRawAccountType\":1}" +
                        "}",
                json
        );
    }

    @Test
    public void rejectsNonV4DeathLink()
    {
        try
        {
            Slice5HardcoreStatusLossEvent.serialize(
                    "12345678-1234-4234-8234-123456789abc",
                    "2026-09-27T21:00:00Z",
                    "session",
                    15,
                    "not-a-v4-uuid"
            );

            fail("Expected deathEventId rejection");
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals(
                    "deathEventId",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsNegativeSequence()
    {
        try
        {
            Slice5HardcoreStatusLossEvent.serialize(
                    "12345678-1234-4234-8234-123456789abc",
                    "2026-09-27T21:00:00Z",
                    "session",
                    -1,
                    "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
            );

            fail("Expected sequence rejection");
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals(
                    "sequence",
                    expected.getMessage()
            );
        }
    }
}