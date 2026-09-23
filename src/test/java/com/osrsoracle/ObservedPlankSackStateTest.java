package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ObservedPlankSackStateTest
{
    @Test
    public void startsUnobserved()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        assertFalse(state.hasObservation());
        assertNull(state.getQuantities());
        assertNull(state.getObservedAt());
    }

    @Test
    public void retainsAllSevenPlankTypes()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        assertTrue(
                state.observe(
                        1, 2, 3, 4, 5, 6, 7,
                        "2026-09-23T04:00:00Z"
                )
        );

        assertArrayEquals(
                new int[] {1, 2, 3, 4, 5, 6, 7},
                state.getQuantities()
        );
    }

    @Test
    public void observedEmptySupersedesPriorContents()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        state.observe(
                1, 2, 3, 4, 5, 6, 7,
                "2026-09-23T04:00:00Z"
        );

        assertTrue(
                state.observe(
                        0, 0, 0, 0, 0, 0, 0,
                        "2026-09-23T04:01:00Z"
                )
        );

        assertArrayEquals(
                new int[] {0, 0, 0, 0, 0, 0, 0},
                state.getQuantities()
        );
    }

    @Test
    public void staleObservationCannotRegressState()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        state.observe(
                7, 6, 5, 4, 3, 2, 1,
                "2026-09-23T04:05:00Z"
        );

        assertFalse(
                state.observe(
                        1, 1, 1, 1, 1, 1, 1,
                        "2026-09-23T04:04:59Z"
                )
        );

        assertArrayEquals(
                new int[] {7, 6, 5, 4, 3, 2, 1},
                state.getQuantities()
        );
    }

    @Test
    public void returnedQuantitiesAreDefensiveCopy()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        state.observe(
                1, 2, 3, 4, 5, 6, 7,
                "2026-09-23T04:00:00Z"
        );

        int[] quantities = state.getQuantities();
        quantities[0] = 999;

        assertArrayEquals(
                new int[] {1, 2, 3, 4, 5, 6, 7},
                state.getQuantities()
        );
    }

    @Test
    public void rejectsNegativeQuantity()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        boolean thrown = false;

        try
        {
            state.observe(
                    0, 0, -1, 0, 0, 0, 0,
                    "2026-09-23T04:00:00Z"
            );
        }
        catch (IllegalArgumentException ex)
        {
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void serializesNeverObservedSeparatelyFromEmpty()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        assertEquals(
                "{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"contents\":null}",
                state.toJson()
        );
    }

    @Test
    public void serializesObservedContents()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        state.observe(
                1, 2, 3, 4, 5, 6, 7,
                "2026-09-23T04:00:00Z"
        );

        assertEquals(
                "{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"contents\":{\"plain\":1,\"oak\":2,\"teak\":3,\"mahogany\":4,\"camphor\":5,\"ironwood\":6,\"rosewood\":7}}",
                state.toJson()
        );
    }

    @Test
    public void serializesObservedEmptyAsAuthoritativeEmpty()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        state.observe(
                0, 0, 0, 0, 0, 0, 0,
                "2026-09-23T04:00:00Z"
        );

        assertEquals(
                "{\"scope\":\"ACCOUNT\",\"ownership\":\"PERSONAL\",\"contents\":{\"plain\":0,\"oak\":0,\"teak\":0,\"mahogany\":0,\"camphor\":0,\"ironwood\":0,\"rosewood\":0}}",
                state.toJson()
        );
    }

    @Test
    public void resetClearsObservation()
    {
        ObservedPlankSackState state =
                new ObservedPlankSackState();

        state.observe(
                1, 2, 3, 4, 5, 6, 7,
                "2026-09-23T04:00:00Z"
        );

        state.reset();

        assertFalse(state.hasObservation());
        assertNull(state.getQuantities());
        assertNull(state.getObservedAt());
    }
}
