package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ObservedQuiverAmmoStateTest
{
    @Test
    public void startsUnobserved()
    {
        ObservedQuiverAmmoState state =
                new ObservedQuiverAmmoState();

        assertFalse(state.hasObservation());
        assertNull(state.getAmmoItemIdRaw());
        assertNull(state.getAmmoQuantity());
        assertNull(state.getObservedAt());
    }

    @Test
    public void retainsObservedAmmoState()
    {
        ObservedQuiverAmmoState state =
                new ObservedQuiverAmmoState();

        assertTrue(
                state.observe(
                        9244,
                        250,
                        "2026-09-20T20:00:00Z"
                )
        );

        assertTrue(state.hasObservation());

        assertEquals(
                Integer.valueOf(9244),
                state.getAmmoItemIdRaw()
        );

        assertEquals(
                Integer.valueOf(250),
                state.getAmmoQuantity()
        );

        assertEquals(
                "2026-09-20T20:00:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void observedEmptySupersedesPriorContents()
    {
        ObservedQuiverAmmoState state =
                new ObservedQuiverAmmoState();

        state.observe(
                9244,
                250,
                "2026-09-20T20:00:00Z"
        );

        assertTrue(
                state.observe(
                        -1,
                        0,
                        "2026-09-20T20:01:00Z"
                )
        );

        assertEquals(
                Integer.valueOf(-1),
                state.getAmmoItemIdRaw()
        );

        assertEquals(
                Integer.valueOf(0),
                state.getAmmoQuantity()
        );

        assertEquals(
                "2026-09-20T20:01:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void staleObservationCannotRegressState()
    {
        ObservedQuiverAmmoState state =
                new ObservedQuiverAmmoState();

        state.observe(
                9244,
                250,
                "2026-09-20T20:05:00Z"
        );

        assertFalse(
                state.observe(
                        11212,
                        10,
                        "2026-09-20T20:04:59Z"
                )
        );

        assertEquals(
                Integer.valueOf(9244),
                state.getAmmoItemIdRaw()
        );

        assertEquals(
                Integer.valueOf(250),
                state.getAmmoQuantity()
        );

        assertEquals(
                "2026-09-20T20:05:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void equalTimestampMayRefreshObservation()
    {
        ObservedQuiverAmmoState state =
                new ObservedQuiverAmmoState();

        state.observe(
                9244,
                250,
                "2026-09-20T20:05:00Z"
        );

        assertTrue(
                state.observe(
                        9244,
                        249,
                        "2026-09-20T20:05:00Z"
                )
        );

        assertEquals(
                Integer.valueOf(249),
                state.getAmmoQuantity()
        );
    }

    @Test
    public void resetClearsObservation()
    {
        ObservedQuiverAmmoState state =
                new ObservedQuiverAmmoState();

        state.observe(
                9244,
                250,
                "2026-09-20T20:00:00Z"
        );

        state.reset();

        assertFalse(state.hasObservation());
        assertNull(state.getAmmoItemIdRaw());
        assertNull(state.getAmmoQuantity());
        assertNull(state.getObservedAt());
    }
}