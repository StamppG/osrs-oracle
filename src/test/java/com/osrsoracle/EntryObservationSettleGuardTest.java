package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntryObservationSettleGuardTest
{
    @Test
    public void startsInactive()
    {
        EntryObservationSettleGuard guard =
                new EntryObservationSettleGuard();

        assertFalse(guard.isSettling(100));
        assertEquals(0, guard.remainingTicks(100));
    }

    @Test
    public void threeTickEntryWindowBlocksUntilDueTick()
    {
        EntryObservationSettleGuard guard =
                new EntryObservationSettleGuard();

        guard.begin(100, 3);

        assertTrue(guard.isSettling(100));
        assertTrue(guard.isSettling(102));
        assertEquals(3, guard.remainingTicks(100));
        assertEquals(1, guard.remainingTicks(102));
        assertFalse(guard.isSettling(103));
        assertEquals(0, guard.remainingTicks(103));
    }

    @Test
    public void rearmingReplacesPriorEntryDeadline()
    {
        EntryObservationSettleGuard guard =
                new EntryObservationSettleGuard();

        guard.begin(100, 3);
        guard.begin(102, 3);

        assertTrue(guard.isSettling(104));
        assertFalse(guard.isSettling(105));
    }

    @Test
    public void transientMotherlodeZeroIsIgnoredDuringEntrySettle()
    {
        EntryObservationSettleGuard guard =
                new EntryObservationSettleGuard();
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        state.observe(
                27,
                "2026-09-23T06:27:53Z"
        );

        guard.begin(100, 3);

        if (!guard.isSettling(101))
        {
            state.observe(
                    0,
                    "2026-09-23T06:28:59Z"
            );
        }

        assertEquals(
                Integer.valueOf(27),
                state.getQuantity()
        );
    }

    @Test
    public void synchronizedZeroRemainsAuthoritativeAfterSettle()
    {
        EntryObservationSettleGuard guard =
                new EntryObservationSettleGuard();
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        state.observe(
                27,
                "2026-09-23T06:27:53Z"
        );

        guard.begin(100, 3);
        guard.clearIfSettled(103);

        if (!guard.isSettling(103))
        {
            state.observe(
                    0,
                    "2026-09-23T06:29:01Z"
            );
        }

        assertEquals(
                Integer.valueOf(0),
                state.getQuantity()
        );
    }

    @Test
    public void resetClearsEntrySettle()
    {
        EntryObservationSettleGuard guard =
                new EntryObservationSettleGuard();

        guard.begin(100, 3);
        guard.reset();

        assertFalse(guard.isSettling(100));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeSettleTicks()
    {
        new EntryObservationSettleGuard().begin(100, -1);
    }

    @Test
    public void retainsAuthoritativeQuantityAcrossHopTransientZero()
    {
        EntryObservationSettleGuard guard =
                new EntryObservationSettleGuard();
        ObservedMotherlodeSackState state =
                new ObservedMotherlodeSackState();

        state.observe(
                27,
                "2026-09-23T06:27:53Z"
        );

        guard.begin(100, 3);

        if (!guard.isSettling(101))
        {
            state.observe(
                    0,
                    "2026-09-23T06:28:59Z"
            );
        }

        assertEquals(
                Integer.valueOf(27),
                state.getQuantity()
        );

        guard.clearIfSettled(103);

        if (!guard.isSettling(103))
        {
            state.observe(
                    27,
                    "2026-09-23T06:29:01Z"
            );
        }

        assertEquals(
                Integer.valueOf(27),
                state.getQuantity()
        );
        assertEquals(
                "2026-09-23T06:29:01Z",
                state.getObservedAt()
        );
    }
}
