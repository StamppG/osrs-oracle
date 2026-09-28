package com.osrsoracle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class Slice5Ack
{
    static final String STATUS_OK =
            "OK";

    static final String STATUS_PARTIAL_CONFLICT =
            "PARTIAL_CONFLICT";

    static final String CONFLICT_IMMUTABLE_EVENT =
            "IMMUTABLE_EVENT_CONFLICT";

    static final class Conflict
    {
        private final String eventId;
        private final String code;

        private Conflict(
                String eventId,
                String code
        )
        {
            this.eventId = eventId;
            this.code = code;
        }

        String getEventId()
        {
            return eventId;
        }

        String getCode()
        {
            return code;
        }
    }

    private final String characterRecordId;
    private final String status;
    private final List<String> acknowledgedEventIds;
    private final List<Conflict> eventConflicts;
    private final List<String> acknowledgedCoverageGapIds;

    private Slice5Ack(
            String characterRecordId,
            String status,
            List<String> acknowledgedEventIds,
            List<Conflict> eventConflicts,
            List<String> acknowledgedCoverageGapIds
    )
    {
        this.characterRecordId =
                characterRecordId;

        this.status =
                status;

        this.acknowledgedEventIds =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                acknowledgedEventIds
                        )
                );

        this.eventConflicts =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                eventConflicts
                        )
                );

        this.acknowledgedCoverageGapIds =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                acknowledgedCoverageGapIds
                        )
                );
    }

    static Slice5Ack parse(
            Gson gson,
            String json
    )
    {
        if (gson == null)
        {
            throw new IllegalArgumentException(
                    "gson"
            );
        }

        if (
                json == null ||
                        json.isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "json"
            );
        }

        final JsonObject root;

        try
        {
            root =
                    gson.fromJson(
                            json,
                            JsonObject.class
                    );
        }
        catch (RuntimeException e)
        {
            throw new IllegalArgumentException(
                    "json",
                    e
            );
        }

        if (
                root == null ||
                        !requiredBoolean(
                                root,
                                "success"
                        )
        )
        {
            throw new IllegalArgumentException(
                    "success"
            );
        }

        String characterRecordId =
                requiredUuidV4(
                        root,
                        "characterRecordId"
                );

        JsonObject ingest =
                requiredObject(
                        root,
                        "eventIngest"
                );

        String status =
                requiredString(
                        ingest,
                        "status"
                );

        if (
                !STATUS_OK.equals(status) &&
                        !STATUS_PARTIAL_CONFLICT.equals(
                                status
                        )
        )
        {
            throw new IllegalArgumentException(
                    "eventIngest.status"
            );
        }

        List<String> acknowledgedEventIds =
                uuidArray(
                        ingest,
                        "acknowledgedEventIds"
                );

        List<String> acknowledgedCoverageGapIds =
                uuidArray(
                        ingest,
                        "acknowledgedCoverageGapIds"
                );

        JsonArray conflictArray =
                requiredArray(
                        ingest,
                        "eventConflicts"
                );

        List<Conflict> conflicts =
                new ArrayList<>();

        Set<String> conflictIds =
                new HashSet<>();

        for (JsonElement element : conflictArray)
        {
            if (
                    element == null ||
                            !element.isJsonObject()
            )
            {
                throw new IllegalArgumentException(
                        "eventConflicts"
                );
            }

            JsonObject conflictJson =
                    element.getAsJsonObject();

            String eventId =
                    requiredUuidV4(
                            conflictJson,
                            "eventId"
                    );

            String code =
                    requiredString(
                            conflictJson,
                            "code"
                    );

            if (
                    !CONFLICT_IMMUTABLE_EVENT.equals(
                            code
                    )
            )
            {
                throw new IllegalArgumentException(
                        "eventConflicts.code"
                );
            }

            if (!conflictIds.add(eventId))
            {
                throw new IllegalArgumentException(
                        "duplicate conflict eventId"
                );
            }

            conflicts.add(
                    new Conflict(
                            eventId,
                            code
                    )
            );
        }

        Set<String> acknowledgedIds =
                new HashSet<>(
                        acknowledgedEventIds
                );

        for (String conflictId : conflictIds)
        {
            if (acknowledgedIds.contains(conflictId))
            {
                throw new IllegalArgumentException(
                        "event both acknowledged and conflicted"
                );
            }
        }

        if (
                STATUS_OK.equals(status) &&
                        !conflicts.isEmpty()
        )
        {
            throw new IllegalArgumentException(
                    "OK response contains conflict"
            );
        }

        if (
                STATUS_PARTIAL_CONFLICT.equals(status) &&
                        conflicts.isEmpty()
        )
        {
            throw new IllegalArgumentException(
                    "PARTIAL_CONFLICT without conflict"
            );
        }

        return new Slice5Ack(
                characterRecordId,
                status,
                acknowledgedEventIds,
                conflicts,
                acknowledgedCoverageGapIds
        );
    }

    String getCharacterRecordId()
    {
        return characterRecordId;
    }

    String getStatus()
    {
        return status;
    }

    List<String> getAcknowledgedEventIds()
    {
        return acknowledgedEventIds;
    }

    List<Conflict> getEventConflicts()
    {
        return eventConflicts;
    }

    List<String> getAcknowledgedCoverageGapIds()
    {
        return acknowledgedCoverageGapIds;
    }

    private static List<String> uuidArray(
            JsonObject object,
            String field
    )
    {
        JsonArray array =
                requiredArray(
                        object,
                        field
                );

        List<String> values =
                new ArrayList<>();

        Set<String> unique =
                new HashSet<>();

        for (JsonElement element : array)
        {
            if (
                    element == null ||
                            !element.isJsonPrimitive() ||
                            !element
                                    .getAsJsonPrimitive()
                                    .isString()
            )
            {
                throw new IllegalArgumentException(
                        field
                );
            }

            String value =
                    requireUuidV4(
                            element.getAsString(),
                            field
                    );

            if (!unique.add(value))
            {
                throw new IllegalArgumentException(
                        "duplicate " + field
                );
            }

            values.add(value);
        }

        return values;
    }

    private static String requiredUuidV4(
            JsonObject object,
            String field
    )
    {
        return requireUuidV4(
                requiredString(
                        object,
                        field
                ),
                field
        );
    }

    private static String requireUuidV4(
            String value,
            String field
    )
    {
        final UUID uuid;

        try
        {
            uuid = UUID.fromString(value);
        }
        catch (RuntimeException e)
        {
            throw new IllegalArgumentException(
                    field,
                    e
            );
        }

        if (uuid.version() != 4)
        {
            throw new IllegalArgumentException(
                    field
            );
        }

        return value;
    }

    private static String requiredString(
            JsonObject object,
            String field
    )
    {
        JsonElement element =
                object.get(field);

        if (
                element == null ||
                        element.isJsonNull() ||
                        !element.isJsonPrimitive() ||
                        !element
                                .getAsJsonPrimitive()
                                .isString()
        )
        {
            throw new IllegalArgumentException(
                    field
            );
        }

        String value =
                element.getAsString();

        if (value.isBlank())
        {
            throw new IllegalArgumentException(
                    field
            );
        }

        return value;
    }

    private static boolean requiredBoolean(
            JsonObject object,
            String field
    )
    {
        JsonElement element =
                object.get(field);

        if (
                element == null ||
                        element.isJsonNull() ||
                        !element.isJsonPrimitive() ||
                        !element
                                .getAsJsonPrimitive()
                                .isBoolean()
        )
        {
            throw new IllegalArgumentException(
                    field
            );
        }

        return element.getAsBoolean();
    }

    private static JsonObject requiredObject(
            JsonObject object,
            String field
    )
    {
        JsonElement element =
                object.get(field);

        if (
                element == null ||
                        element.isJsonNull() ||
                        !element.isJsonObject()
        )
        {
            throw new IllegalArgumentException(
                    field
            );
        }

        return element.getAsJsonObject();
    }

    private static JsonArray requiredArray(
            JsonObject object,
            String field
    )
    {
        JsonElement element =
                object.get(field);

        if (
                element == null ||
                        element.isJsonNull() ||
                        !element.isJsonArray()
        )
        {
            throw new IllegalArgumentException(
                    field
            );
        }

        return element.getAsJsonArray();
    }
}