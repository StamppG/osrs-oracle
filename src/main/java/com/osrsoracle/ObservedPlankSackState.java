package com.osrsoracle;

import java.time.Instant;

final class ObservedPlankSackState
{
    private int[] quantities;
    private String observedAt;

    boolean observe(
            int plain,
            int oak,
            int teak,
            int mahogany,
            int camphor,
            int ironwood,
            int rosewood,
            String observedAt
    )
    {
        int[] candidateQuantities = {
            plain,
            oak,
            teak,
            mahogany,
            camphor,
            ironwood,
            rosewood
        };

        for (int quantity : candidateQuantities)
        {
            if (quantity < 0)
            {
                throw new IllegalArgumentException(
                        "Plank Sack quantities must not be negative"
                );
            }
        }

        if (observedAt == null)
        {
            throw new IllegalArgumentException(
                    "observedAt must not be null"
            );
        }

        Instant candidate = Instant.parse(observedAt);

        if (
                this.observedAt != null &&
                        candidate.isBefore(
                                Instant.parse(this.observedAt)
                        )
        )
        {
            return false;
        }

        quantities = candidateQuantities.clone();
        this.observedAt = observedAt;

        return true;
    }

    void reset()
    {
        quantities = null;
        observedAt = null;
    }

    boolean hasObservation()
    {
        return observedAt != null;
    }

    int[] getQuantities()
    {
        return quantities == null
                ? null
                : quantities.clone();
    }

    String getObservedAt()
    {
        return observedAt;
    }

    String toJson()
    {
        if (!hasObservation())
        {
            return "{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"contents\":null}";
        }

        return "{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"contents\":{" +
                "\"plain\":" + quantities[0] +
                ",\"oak\":" + quantities[1] +
                ",\"teak\":" + quantities[2] +
                ",\"mahogany\":" + quantities[3] +
                ",\"camphor\":" + quantities[4] +
                ",\"ironwood\":" + quantities[5] +
                ",\"rosewood\":" + quantities[6] +
                "}}";
    }
}
