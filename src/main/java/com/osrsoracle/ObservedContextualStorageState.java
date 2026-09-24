package com.osrsoracle;

import java.time.Instant;
import java.util.StringJoiner;

final class ObservedContextualStorageState
{
    static final class Entry
    {
        private final String name;
        private final int quantity;

        Entry(
                String name,
                int quantity
        )
        {
            if (name == null || name.isBlank())
            {
                throw new IllegalArgumentException("name");
            }

            if (quantity <= 0)
            {
                throw new IllegalArgumentException(
                        "quantity must be positive"
                );
            }

            this.name = name;
            this.quantity = quantity;
        }

        String getName()
        {
            return name;
        }

        int getQuantity()
        {
            return quantity;
        }
    }

    private Entry[] entries;
    private String observedAt;

    boolean observe(
            Entry[] candidateEntries,
            String candidateObservedAt
    )
    {
        if (candidateEntries == null)
        {
            throw new IllegalArgumentException("entries");
        }

        if (
                candidateObservedAt == null ||
                        candidateObservedAt.isBlank()
        )
        {
            throw new IllegalArgumentException("observedAt");
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

        Entry[] copy =
                new Entry[candidateEntries.length];

        for (int i = 0; i < candidateEntries.length; i++)
        {
            Entry entry = candidateEntries[i];

            if (entry == null)
            {
                throw new IllegalArgumentException(
                        "entries must not contain null"
                );
            }

            copy[i] =
                    new Entry(
                            entry.getName(),
                            entry.getQuantity()
                    );
        }

        entries = copy;
        observedAt = candidateObservedAt;
        return true;
    }

    void reset()
    {
        entries = null;
        observedAt = null;
    }

    boolean hasObservation()
    {
        return observedAt != null;
    }

    Entry[] getEntries()
    {
        return entries == null
                ? null
                : entries.clone();
    }

    String getObservedAt()
    {
        return observedAt;
    }

    String toJson()
    {
        if (!hasObservation())
        {
            return "{" +
                    "\"scope\":\"ACCOUNT\"," +
                    "\"ownership\":\"PERSONAL\"," +
                    "\"observedAt\":null," +
                    "\"items\":null" +
                    "}";
        }

        StringJoiner items =
                new StringJoiner(",", "[", "]");

        for (Entry entry : entries)
        {
            items.add(
                    "{" +
                            "\"nameRaw\":" +
                            jsonString(entry.getName()) +
                            ",\"quantity\":" +
                            entry.getQuantity() +
                            "}"
            );
        }

        return "{" +
                "\"scope\":\"ACCOUNT\"," +
                "\"ownership\":\"PERSONAL\"," +
                "\"observedAt\":" + jsonString(observedAt) + "," +
                "\"items\":" + items +
                "}";
    }

    private static String jsonString(String value)
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
