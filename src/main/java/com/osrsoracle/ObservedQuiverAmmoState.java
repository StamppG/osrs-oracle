package com.osrsoracle;

import java.time.Instant;

final class ObservedQuiverAmmoState
{
    private Integer ammoItemIdRaw;
    private Integer ammoQuantity;
    private String observedAt;

    boolean observe(
            int ammoItemIdRaw,
            int ammoQuantity,
            String observedAt
    )
    {
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

        this.ammoItemIdRaw = ammoItemIdRaw;
        this.ammoQuantity = ammoQuantity;
        this.observedAt = observedAt;

        return true;
    }

    void reset()
    {
        ammoItemIdRaw = null;
        ammoQuantity = null;
        observedAt = null;
    }

    boolean hasObservation()
    {
        return observedAt != null;
    }

    Integer getAmmoItemIdRaw()
    {
        return ammoItemIdRaw;
    }

    Integer getAmmoQuantity()
    {
        return ammoQuantity;
    }

    String getObservedAt()
    {
        return observedAt;
    }
}