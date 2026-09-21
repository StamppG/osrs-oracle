package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ObservedMotherlodeSackStateTest
{
    @Test
    public void startsUnobserved()
    {
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        assertFalse(state.hasObservation());
        assertNull(state.getQuantity());
        assertNull(state.getObservedAt());
    }

    @Test
    public void retainsObservedQuantity()
    {
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        assertTrue(
                state.observe(
                        42,
                        "2026-09-21T07:10:00Z"
                )
        );

        assertEquals(
                Integer.valueOf(42),
                state.getQuantity()
        );
        assertEquals(
                "2026-09-21T07:10:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void observedZeroSupersedesPriorQuantity()
    {
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        state.observe(
                42,
                "2026-09-21T07:10:00Z"
        );

        assertTrue(
                state.observe(
                        0,
                        "2026-09-21T07:11:00Z"
                )
        );

        assertEquals(
                Integer.valueOf(0),
                state.getQuantity()
        );
    }

    @Test
    public void staleObservationCannotRegressState()
    {
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        state.observe(
                60,
                "2026-09-21T07:15:00Z"
        );

        assertFalse(
                state.observe(
                        10,
                        "2026-09-21T07:14:59Z"
                )
        );

        assertEquals(
                Integer.valueOf(60),
                state.getQuantity()
        );
        assertEquals(
                "2026-09-21T07:15:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void equalTimestampMayRefreshObservation()
    {
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        state.observe(
                60,
                "2026-09-21T07:15:00Z"
        );

        assertTrue(
                state.observe(
                        61,
                        "2026-09-21T07:15:00Z"
                )
        );

        assertEquals(
                Integer.valueOf(61),
                state.getQuantity()
        );
    }

    @Test
    public void rejectsNegativeQuantity()
    {
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        boolean thrown = false;

        try
        {
            state.observe(
                    -1,
                    "2026-09-21T07:15:00Z"
            );
        }
        catch (IllegalArgumentException ex)
        {
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void resetClearsObservation()
    {
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        state.observe(
                42,
                "2026-09-21T07:10:00Z"
        );

        state.reset();

        assertFalse(state.hasObservation());
        assertNull(state.getQuantity());
        assertNull(state.getObservedAt());
    }
}
