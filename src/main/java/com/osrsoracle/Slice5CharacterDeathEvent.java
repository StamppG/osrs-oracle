package com.osrsoracle;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

final class Slice5CharacterDeathEvent
{
    private static final long MAX_SAFE_JSON_INTEGER =
            9_007_199_254_740_991L;

    private Slice5CharacterDeathEvent()
    {
    }

    static String serialize(
            String eventId,
            String observedAt,
            String sessionId,
            long sequence,
            int world,
            int x,
            int y,
            int plane,
            AccountTypeResolution accountMode,
            String inventoryJson,
            boolean inventoryObserved,
            String equipmentJson,
            boolean equipmentObserved,
            String finalLivingStateCandidateJson
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
                        sequence >
                                MAX_SAFE_JSON_INTEGER
        )
        {
            throw new IllegalArgumentException(
                    "sequence"
            );
        }

        if (world <= 0)
        {
            throw new IllegalArgumentException(
                    "world"
            );
        }

        if (accountMode == null)
        {
            throw new IllegalArgumentException(
                    "accountMode"
            );
        }

        String officialAccountType =
                accountMode.getOfficialAccountType();

        boolean terminalCapable =
                "HARDCORE_IRONMAN".equals(
                        officialAccountType
                ) ||
                        "HARDCORE_GROUP_IRONMAN".equals(
                                officialAccountType
                        );

        if (terminalCapable)
        {
            if (
                    finalLivingStateCandidateJson == null ||
                            finalLivingStateCandidateJson.isBlank() ||
                            "null".equals(
                                    finalLivingStateCandidateJson
                                            .trim()
                            )
            )
            {
                throw new IllegalArgumentException(
                        "terminal-capable death requires finalLivingStateCandidate"
                );
            }
        }
        else if (
                finalLivingStateCandidateJson != null
        )
        {
            throw new IllegalArgumentException(
                    "non-terminal death cannot carry finalLivingStateCandidate"
            );
        }

        String inventoryPayload =
                observedPayload(
                        inventoryJson,
                        inventoryObserved,
                        "inventory"
                );

        String equipmentPayload =
                observedPayload(
                        equipmentJson,
                        equipmentObserved,
                        "equipment"
                );

        return "{" +
                "\"eventId\":" +
                jsonString(eventId) +
                "," +
                "\"type\":\"CHARACTER_DEATH\"," +
                "\"observedAt\":" +
                jsonString(observedAt) +
                "," +
                "\"origin\":{" +
                "\"source\":\"ACTOR_DEATH\"," +
                "\"sessionId\":" +
                jsonString(sessionId) +
                "," +
                "\"sequence\":" +
                sequence +
                "}," +
                "\"world\":" +
                world +
                "," +
                "\"location\":{" +
                "\"x\":" +
                x +
                "," +
                "\"y\":" +
                y +
                "," +
                "\"plane\":" +
                plane +
                "}," +
                "\"accountMode\":" +
                accountModeJson(accountMode) +
                "," +
                "\"inventory\":" +
                inventoryPayload +
                "," +
                "\"equipment\":" +
                equipmentPayload +
                "," +
                "\"coverage\":{" +
                "\"inventory\":" +
                coverageJson(
                        inventoryObserved,
                        observedAt
                ) +
                "," +
                "\"equipment\":" +
                coverageJson(
                        equipmentObserved,
                        observedAt
                ) +
                "}," +
                "\"responsibleSource\":{" +
                "\"status\":\"UNKNOWN\"" +
                "}," +
                "\"finalLivingStateCandidate\":" +
                (
                        finalLivingStateCandidateJson == null
                                ? "null"
                                : finalLivingStateCandidateJson
                ) +
                "}";
    }

    private static String accountModeJson(
            AccountTypeResolution resolution
    )
    {
        return "{" +
                "\"accountType\":" +
                jsonString(
                        resolution.getOfficialAccountType()
                ) +
                "," +
                "\"resolutionStatus\":" +
                jsonString(
                        resolution.getStatus().name()
                ) +
                "," +
                "\"rawAccountType\":" +
                nullableInteger(
                        resolution.getRawAccountType()
                ) +
                "," +
                "\"provisionalAccountType\":" +
                jsonString(
                        resolution.getProvisionalAccountType()
                ) +
                "," +
                "\"inTutorialIsland\":" +
                nullableBoolean(
                        resolution.getInTutorialIsland()
                ) +
                "}";
    }

    private static String observedPayload(
            String payloadJson,
            boolean observed,
            String label
    )
    {
        if (!observed)
        {
            return "null";
        }

        if (
                payloadJson == null ||
                        payloadJson.isBlank() ||
                        "null".equals(
                                payloadJson.trim()
                        )
        )
        {
            throw new IllegalArgumentException(
                    label +
                            " observed without payload"
            );
        }

        return payloadJson;
    }

    private static String coverageJson(
            boolean observed,
            String observedAt
    )
    {
        if (!observed)
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

    private static String nullableInteger(
            Integer value
    )
    {
        return value == null
                ? "null"
                : Integer.toString(value);
    }

    private static String nullableBoolean(
            Boolean value
    )
    {
        return value == null
                ? "null"
                : Boolean.toString(value);
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