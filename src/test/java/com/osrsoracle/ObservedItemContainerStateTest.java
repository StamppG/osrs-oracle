package com.osrsoracle;

import net.runelite.api.Item;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ObservedItemContainerStateTest
{
    @Test
    public void missingObservationDoesNotEraseKnownState()
    {
        ObservedItemContainerState state =
                new ObservedItemContainerState(
                        "gimStorage",
                        ObservedItemContainerState.Scope.GROUP,
                        true
                );

        state.observe(
                new Item[] {
                    new Item(995, 1000),
                    new Item(4151, 1)
                },
                "2026-09-20T20:00:00Z"
        );

        boolean changed =
                state.observeIfPresent(
                        null,
                        "2026-09-20T21:00:00Z"
                );

        assertFalse(changed);
        assertTrue(state.hasObservation());
        assertEquals("OBSERVED", state.getStatus());
        assertEquals(
                "2026-09-20T20:00:00Z",
                state.getObservedAt()
        );

        Item[] retained = state.getItems();

        assertNotNull(retained);
        assertEquals(2, retained.length);
        assertEquals(995, retained[0].getId());
        assertEquals(1000, retained[0].getQuantity());
        assertEquals(4151, retained[1].getId());
    }

    @Test
    public void observedEmptyReplacesPreviousContents()
    {
        ObservedItemContainerState state =
                new ObservedItemContainerState(
                        "bank",
                        ObservedItemContainerState.Scope.ACCOUNT,
                        false
                );

        state.observe(
                new Item[] {
                    new Item(995, 500)
                },
                "2026-09-20T20:00:00Z"
        );

        state.observe(
                new Item[0],
                "2026-09-20T21:00:00Z"
        );

        assertTrue(state.hasObservation());
        assertEquals(
                "2026-09-20T21:00:00Z",
                state.getObservedAt()
        );

        Item[] items = state.getItems();

        assertNotNull(items);
        assertEquals(0, items.length);
    }

    @Test
    public void newerObservationReplacesOlderObservation()
    {
        ObservedItemContainerState state =
                new ObservedItemContainerState(
                        "coxPrivateStorage",
                        ObservedItemContainerState.Scope.ACCOUNT,
                        false
                );

        state.observe(
                new Item[] {
                    new Item(20997, 1)
                },
                "2026-09-20T20:00:00Z"
        );

        state.observe(
                new Item[] {
                    new Item(20997, 2),
                    new Item(560, 100)
                },
                "2026-09-20T20:05:00Z"
        );

        assertEquals(
                "2026-09-20T20:05:00Z",
                state.getObservedAt()
        );

        Item[] items = state.getItems();

        assertNotNull(items);
        assertEquals(2, items.length);
        assertEquals(2, items[0].getQuantity());
        assertEquals(560, items[1].getId());
    }

    @Test
    public void olderObservationCannotRegressKnownState()
    {
        ObservedItemContainerState state =
                new ObservedItemContainerState(
                        "gimStorage",
                        ObservedItemContainerState.Scope.GROUP,
                        true
                );

        assertTrue(
                state.observe(
                        new Item[] {
                            new Item(995, 2000)
                        },
                        "2026-09-20T21:00:00Z"
                )
        );

        assertFalse(
                state.observe(
                        new Item[] {
                            new Item(995, 1000)
                        },
                        "2026-09-20T20:00:00Z"
                )
        );

        assertEquals(
                "2026-09-20T21:00:00Z",
                state.getObservedAt()
        );

        Item[] retained = state.getItems();

        assertNotNull(retained);
        assertEquals(1, retained.length);
        assertEquals(995, retained[0].getId());
        assertEquals(2000, retained[0].getQuantity());
    }
    @Test
    public void groupScopeRemainsExplicitlyShared()
    {
        ObservedItemContainerState state =
                new ObservedItemContainerState(
                        "gimStorage",
                        ObservedItemContainerState.Scope.GROUP,
                        true
                );

        assertEquals("gimStorage", state.getDataset());
        assertEquals(
                ObservedItemContainerState.Scope.GROUP,
                state.getScope()
        );
        assertTrue(state.isShared());
        assertFalse(state.hasObservation());
        assertEquals("NOT_OBSERVED", state.getStatus());
        assertNull(state.getObservedAt());
        assertNull(state.getItems());
    }

    @Test
    public void returnedItemsCannotMutateCachedObservation()
    {
        ObservedItemContainerState state =
                new ObservedItemContainerState(
                        "seedVault",
                        ObservedItemContainerState.Scope.ACCOUNT,
                        false
                );

        state.observe(
                new Item[] {
                    new Item(5318, 12)
                },
                "2026-09-20T20:00:00Z"
        );

        Item[] copy = state.getItems();
        copy[0] = new Item(995, 999999);

        Item[] retained = state.getItems();

        assertNotNull(retained);
        assertEquals(5318, retained[0].getId());
        assertEquals(12, retained[0].getQuantity());
    }
}