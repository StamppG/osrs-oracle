package com.osrsoracle;

import java.time.Instant;

final class ObservedMotherlodeSackState
{
    private Integer quantity;
    private String observedAt;

    boolean observe(
            int quantity,
            String observedAt
    )
    {
        if (quantity < 0)
        {
            throw new IllegalArgumentException(
                    "quantity must not be negative"
            );
        }

        if (observedAt == null)
        {
            throw new IllegalArgumentException(
                    "observedAt must not be null"
            );
        }

        Instant candidate =
                Instant.parse(observedAt);

        if (
                this.observedAt != null &&
                        candidate.isBefore(
                                Instant.parse(this.observedAt)
                        )
        )
        {
            return false;
        }

        this.quantity = quantity;
        this.observedAt = observedAt;

        return true;
    }

    void reset()
    {
        quantity = null;
        observedAt = null;
    }

    boolean hasObservation()
    {
        return observedAt != null;
    }

    Integer getQuantity()
    {
        return quantity;
    }

    String getObservedAt()
    {
        return observedAt;
    }
}
