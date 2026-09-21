package com.osrsoracle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

final class ObservedStashState
{
    static final class Entry
    {
        private final int objectId;
        private final boolean built;
        private final boolean filled;

        Entry(
                int objectId,
                boolean built,
                boolean filled
        )
        {
            this.objectId = objectId;
            this.built = built;
            this.filled = filled;
        }

        int getObjectId()
        {
            return objectId;
        }

        boolean isBuilt()
        {
            return built;
        }

        boolean isFilled()
        {
            return filled;
        }
    }

    private List<Entry> entries =
            Collections.emptyList();

    private String observedAt;

    boolean observeComplete(
            List<Entry> observedEntries,
            String candidateObservedAt
    )
    {
        if (
                observedEntries == null ||
                candidateObservedAt == null
        )
        {
            return false;
        }

        Instant candidate =
                Instant.parse(candidateObservedAt);

        if (
                observedAt != null &&
                candidate.isBefore(
                        Instant.parse(observedAt)
                )
        )
        {
            return false;
        }

        List<Entry> copy =
                new ArrayList<>(observedEntries.size());

        for (Entry entry : observedEntries)
        {
            if (entry == null)
            {
                return false;
            }

            copy.add(
                    new Entry(
                            entry.getObjectId(),
                            entry.isBuilt(),
                            entry.isFilled()
                    )
            );
        }

        entries =
                Collections.unmodifiableList(copy);

        observedAt =
                candidateObservedAt;

        return true;
    }

    void reset()
    {
        entries =
                Collections.emptyList();

        observedAt =
                null;
    }

    String getObservedAt()
    {
        return observedAt;
    }

    List<Entry> getEntries()
    {
        return entries;
    }

    String toJson()
    {
        if (observedAt == null)
        {
            return "null";
        }

        StringJoiner units =
                new StringJoiner(
                        ",",
                        "[",
                        "]"
                );

        for (Entry entry : entries)
        {
            units.add(
                    "{\"objectId\":" +
                            entry.getObjectId() +
                            ",\"built\":" +
                            entry.isBuilt() +
                            ",\"filled\":" +
                            entry.isFilled() +
                            "}"
            );
        }

        return "{\"scope\":\"ACCOUNT\"" +
                ",\"shared\":false" +
                ",\"observedAt\":" +
                jsonString(observedAt) +
                ",\"units\":" +
                units +
                "}";
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
                        .replace("\"", "\\\"") +
                "\"";
    }
}
