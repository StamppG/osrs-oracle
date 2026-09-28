package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Slice5CharacterDeathEventTest
{
    private static final String EVENT_ID =
            "12345678-1234-4234-8234-123456789abc";

    private static final String SESSION_ID =
            "87654321-4321-4321-8321-cba987654321";

    private static final String OBSERVED_AT =
            "2026-09-27T18:00:00Z";

    @Test
    public void serializesOrdinaryDeathExactly()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        1,
                        12850,
                        OBSERVED_AT
                );

        String json =
                Slice5CharacterDeathEvent.serialize(
                        EVENT_ID,
                        OBSERVED_AT,
                        SESSION_ID,
                        7,
                        301,
                        3147,
                        3208,
                        0,
                        resolution,
                        "[null,{\"slot\":1,\"id\":995,\"quantity\":42,\"name\":\"Coins\"}]",
                        true,
                        "{\"head\":null,\"weapon\":null}",
                        true,
                        null
                );

        assertEquals(
                "{" +
                        "\"eventId\":\"12345678-1234-4234-8234-123456789abc\"," +
                        "\"type\":\"CHARACTER_DEATH\"," +
                        "\"observedAt\":\"2026-09-27T18:00:00Z\"," +
                        "\"origin\":{" +
                        "\"source\":\"ACTOR_DEATH\"," +
                        "\"sessionId\":\"87654321-4321-4321-8321-cba987654321\"," +
                        "\"sequence\":7}," +
                        "\"world\":301," +
                        "\"location\":{\"x\":3147,\"y\":3208,\"plane\":0}," +
                        "\"accountMode\":{" +
                        "\"accountType\":\"IRONMAN\"," +
                        "\"resolutionStatus\":\"RESOLVED\"," +
                        "\"rawAccountType\":1," +
                        "\"provisionalAccountType\":null," +
                        "\"inTutorialIsland\":false}," +
                        "\"inventory\":[null,{\"slot\":1,\"id\":995,\"quantity\":42,\"name\":\"Coins\"}]," +
                        "\"equipment\":{\"head\":null,\"weapon\":null}," +
                        "\"coverage\":{" +
                        "\"inventory\":{\"status\":\"OBSERVED\",\"observedAt\":\"2026-09-27T18:00:00Z\"}," +
                        "\"equipment\":{\"status\":\"OBSERVED\",\"observedAt\":\"2026-09-27T18:00:00Z\"}}," +
                        "\"responsibleSource\":{\"status\":\"UNKNOWN\"}," +
                        "\"finalLivingStateCandidate\":null" +
                        "}",
                json
        );
    }

    @Test
    public void missingContainersAreExplicitlyNotObserved()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        4,
                        12850,
                        OBSERVED_AT
                );

        String json =
                Slice5CharacterDeathEvent.serialize(
                        EVENT_ID,
                        OBSERVED_AT,
                        SESSION_ID,
                        8,
                        480,
                        2275,
                        4684,
                        0,
                        resolution,
                        null,
                        false,
                        null,
                        false,
                        null
                );

        assertTrue(
                json.contains(
                        "\"inventory\":null"
                )
        );

        assertTrue(
                json.contains(
                        "\"equipment\":null"
                )
        );

        assertTrue(
                json.contains(
                        "\"inventory\":{\"status\":\"NOT_OBSERVED\",\"observedAt\":null}"
                )
        );

        assertTrue(
                json.contains(
                        "\"equipment\":{\"status\":\"NOT_OBSERVED\",\"observedAt\":null}"
                )
        );
    }

    @Test
    public void resolvedHardcoreRequiresCandidate()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        3,
                        12850,
                        OBSERVED_AT
                );

        try
        {
            Slice5CharacterDeathEvent.serialize(
                    EVENT_ID,
                    OBSERVED_AT,
                    SESSION_ID,
                    9,
                    301,
                    3147,
                    3208,
                    0,
                    resolution,
                    "[]",
                    true,
                    "{}",
                    true,
                    null
            );

            fail(
                    "Expected missing candidate rejection"
            );
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(
                    expected.getMessage()
                            .contains(
                                    "finalLivingStateCandidate"
                            )
            );
        }
    }

    @Test
    public void resolvedHardcoreCarriesCandidate()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        3,
                        12850,
                        OBSERVED_AT
                );

        String candidate =
                "{\"schema\":2,\"projection\":{},\"coverage\":{}}";

        String json =
                Slice5CharacterDeathEvent.serialize(
                        EVENT_ID,
                        OBSERVED_AT,
                        SESSION_ID,
                        10,
                        301,
                        3147,
                        3208,
                        0,
                        resolution,
                        "[]",
                        true,
                        "{}",
                        true,
                        candidate
                );

        assertTrue(
                json.contains(
                        "\"accountType\":\"HARDCORE_IRONMAN\""
                )
        );

        assertTrue(
                json.endsWith(
                        "\"finalLivingStateCandidate\":" +
                                candidate +
                                "}"
                )
        );
    }

    @Test
    public void ordinaryDeathRejectsCandidate()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        1,
                        12850,
                        OBSERVED_AT
                );

        try
        {
            Slice5CharacterDeathEvent.serialize(
                    EVENT_ID,
                    OBSERVED_AT,
                    SESSION_ID,
                    11,
                    301,
                    3147,
                    3208,
                    0,
                    resolution,
                    "[]",
                    true,
                    "{}",
                    true,
                    "{\"schema\":2,\"projection\":{},\"coverage\":{}}"
            );

            fail(
                    "Expected unexpected candidate rejection"
            );
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(
                    expected.getMessage()
                            .contains(
                                    "non-terminal"
                            )
            );
        }
    }

    @Test
    public void provisionalHardcoreDoesNotPretendResolved()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        3,
                        12336,
                        OBSERVED_AT
                );

        String json =
                Slice5CharacterDeathEvent.serialize(
                        EVENT_ID,
                        OBSERVED_AT,
                        SESSION_ID,
                        12,
                        301,
                        3094,
                        3107,
                        0,
                        resolution,
                        "[]",
                        true,
                        "{}",
                        true,
                        null
                );

        assertTrue(
                json.contains(
                        "\"accountType\":null"
                )
        );

        assertTrue(
                json.contains(
                        "\"resolutionStatus\":\"PROVISIONAL_ONBOARDING\""
                )
        );

        assertTrue(
                json.contains(
                        "\"rawAccountType\":3"
                )
        );

        assertTrue(
                json.contains(
                        "\"provisionalAccountType\":\"HARDCORE_IRONMAN\""
                )
        );
    }

    @Test
    public void observedContainerCannotCarryNullPayload()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        1,
                        12850,
                        OBSERVED_AT
                );

        try
        {
            Slice5CharacterDeathEvent.serialize(
                    EVENT_ID,
                    OBSERVED_AT,
                    SESSION_ID,
                    13,
                    301,
                    3147,
                    3208,
                    0,
                    resolution,
                    "null",
                    true,
                    "{}",
                    true,
                    null
            );

            fail(
                    "Expected observed inventory rejection"
            );
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(
                    expected.getMessage()
                            .contains(
                                    "inventory observed without payload"
                            )
            );
        }
    }
}