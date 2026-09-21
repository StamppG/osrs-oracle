package com.osrsoracle;

import java.time.Instant;

final class ObservedPotionStorageState
{
    static final String AMOUNT_DOSES = "DOSES";
    static final String AMOUNT_QUANTITY = "QUANTITY";

    static final class Entry
    {
        private final int itemId;
        private final int amount;
        private final String amountType;

        Entry(
                int itemId,
                int amount,
                String amountType
        )
        {
            if (itemId <= 0)
            {
                throw new IllegalArgumentException(
                        "itemId must be positive"
                );
            }

            if (amount < 0)
            {
                throw new IllegalArgumentException(
                        "amount must not be negative"
                );
            }

            if (
                    !AMOUNT_DOSES.equals(amountType) &&
                            !AMOUNT_QUANTITY.equals(amountType)
            )
            {
                throw new IllegalArgumentException(
                        "unsupported amountType"
                );
            }

            this.itemId = itemId;
            this.amount = amount;
            this.amountType = amountType;
        }

        int getItemId()
        {
            return itemId;
        }

        int getAmount()
        {
            return amount;
        }

        String getAmountType()
        {
            return amountType;
        }
    }

    private Entry[] entries;
    private String observedAt;

    boolean observe(
            Entry[] entries,
            String observedAt
    )
    {
        if (entries == null)
        {
            throw new IllegalArgumentException(
                    "entries must not be null"
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

        this.entries =
                entries.clone();

        this.observedAt =
                observedAt;

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
}
