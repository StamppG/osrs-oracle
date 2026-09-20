package com.osrsoracle;

import java.time.Instant;

import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

final class ObservedItemContainerState
{
    enum Scope
    {
        ACCOUNT,
        GROUP,
        ACTIVITY
    }

    private final String dataset;
    private final Scope scope;
    private final boolean shared;

    private Item[] items;
    private String observedAt;

    ObservedItemContainerState(
            String dataset,
            Scope scope,
            boolean shared
    )
    {
        if (dataset == null || dataset.isBlank())
        {
            throw new IllegalArgumentException("dataset");
        }

        if (scope == null)
        {
            throw new IllegalArgumentException("scope");
        }

        this.dataset = dataset;
        this.scope = scope;
        this.shared = shared;
    }

    boolean observeIfPresent(
            ItemContainer container,
            String observationTime
    )
    {
        if (container == null)
        {
            /*
             * Critical retention rule:
             * unavailable / omitted is not evidence of empty.
             */
            return false;
        }

        return observe(container.getItems(), observationTime);
    }

    boolean observe(
            Item[] observedItems,
            String observationTime
    )
    {
        if (observedItems == null)
        {
            throw new IllegalArgumentException("observedItems");
        }

        if (
                observationTime == null ||
                        observationTime.isBlank()
        )
        {
            throw new IllegalArgumentException("observationTime");
        }

        Instant candidateObservation =
                Instant.parse(observationTime);

        if (
                observedAt != null &&
                        candidateObservation.isBefore(
                                Instant.parse(observedAt)
                        )
        )
        {
            return false;
        }

        /*
         * Empty is a valid authoritative observation.
         *
         * Clone the array so later RuneLite container mutation cannot silently
         * rewrite the historical observation we captured.
         */
        items = observedItems.clone();
        observedAt = observationTime;
        return true;
    }

    void reset()
    {
        items = null;
        observedAt = null;
    }

    boolean hasObservation()
    {
        return observedAt != null;
    }

    String getStatus()
    {
        return hasObservation()
                ? "OBSERVED"
                : "NOT_OBSERVED";
    }

    String getDataset()
    {
        return dataset;
    }

    Scope getScope()
    {
        return scope;
    }

    boolean isShared()
    {
        return shared;
    }

    String getObservedAt()
    {
        return observedAt;
    }

    Item[] getItems()
    {
        return items == null
                ? null
                : items.clone();
    }
}