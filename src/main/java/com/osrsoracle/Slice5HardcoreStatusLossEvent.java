package com.osrsoracle;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

final class Slice5HardcoreStatusLossEvent
{
    private static final long MAX_SAFE_JSON_INTEGER =
            9_007_199_254_740_991L;

    private Slice5HardcoreStatusLossEvent()
    {
    }

    static String serialize(
            String eventId,
            String observedAt,
            String sessionId,
            long sequence,
            String deathEventId
    )
    {
        requireUuidV4(
                eventId,
                "eventId"
        );

        requireInstant(
                observedAt,
                "observedAt"
        );

        requireText(
                sessionId,
                "sessionId"
        );

        if (
                sequence < 0 ||
                        sequence > MAX_SAFE_JSON_INTEGER
        )
        {
            throw new IllegalArgumentException(
                    "sequence"
            );
        }

        requireUuidV4(
                deathEventId,
                "deathEventId"
        );

        return "{" +
                "\"eventId\":" +
                jsonString(eventId) +
                "," +
                "\"type\":\"HARDCORE_STATUS_LOSS\"," +
                "\"observedAt\":" +
                jsonString(observedAt) +
                "," +
                "\"origin\":{" +
                "\"source\":\"ACCOUNT_TYPE_VARBIT_CHANGED\"," +
                "\"sessionId\":" +
                jsonString(sessionId) +
                "," +
                "\"sequence\":" +
                sequence +
                "}," +
                "\"deathEventId\":" +
                jsonString(deathEventId) +
                "," +
                "\"transition\":{" +
                "\"preAccountType\":\"HARDCORE_IRONMAN\"," +
                "\"preRawAccountType\":3," +
                "\"postAccountType\":\"IRONMAN\"," +
                "\"postRawAccountType\":1" +
                "}" +
                "}";
    }

    private static void requireUuidV4(
            String value,
            String label
    )
    {
        requireText(value, label);

        final UUID uuid;

        try
        {
            uuid = UUID.fromString(value);
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException(
                    label,
                    e
            );
        }

        if (uuid.version() != 4)
        {
            throw new IllegalArgumentException(
                    label
            );
        }
    }

    private static void requireInstant(
            String value,
            String label
    )
    {
        requireText(value, label);

        try
        {
            Instant.parse(value);
        }
        catch (DateTimeParseException e)
        {
            throw new IllegalArgumentException(
                    label,
                    e
            );
        }
    }

    private static void requireText(
            String value,
            String label
    )
    {
        if (
                value == null ||
                        value.isBlank()
        )
        {
            throw new IllegalArgumentException(
                    label
            );
        }
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