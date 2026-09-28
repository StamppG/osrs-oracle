package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Slice5HardcoreTransitionTrackerTest
{
    private static final String ACCOUNT =
            "Blue Helm";

    private static final String DEATH_ID =
            "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

    @Test
    public void directThreeToOneWithinWindowConsumesDeathExactlyOnce()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                ACCOUNT,
                DEATH_ID,
                257
        );

        String linked =
                tracker.observeAccountTypeChange(
                        ACCOUNT,
                        264,
                        1
                );

        assertEquals(
                DEATH_ID,
                linked
        );

        assertFalse(
                tracker.hasPendingDeath()
        );

        assertNull(
                tracker.observeAccountTypeChange(
                        ACCOUNT,
                        264,
                        1
                )
        );
    }

    @Test
    public void differentAccountCannotConsumePendingDeath()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                ACCOUNT,
                DEATH_ID,
                257
        );

        assertNull(
                tracker.observeAccountTypeChange(
                        "Red Helm",
                        264,
                        1
                )
        );

        assertFalse(
                tracker.hasPendingDeath()
        );
    }

    @Test
    public void accountNormalizationStillMatchesSameCharacter()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                "Blue Helm",
                DEATH_ID,
                257
        );

        assertEquals(
                "bluehelm",
                tracker.getPendingAccount()
        );

        assertEquals(
                DEATH_ID,
                tracker.observeAccountTypeChange(
                        "blue_helm",
                        264,
                        1
                )
        );
    }

    @Test
    public void safeDeathCanExpireWithoutStatusLoss()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                ACCOUNT,
                DEATH_ID,
                100
        );

        tracker.expire(
                100 +
                        Slice5HardcoreTransitionTracker.MAX_LINK_TICK_DELTA +
                        1
        );

        assertFalse(
                tracker.hasPendingDeath()
        );
    }

    @Test
    public void staleTransitionDoesNotLink()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                ACCOUNT,
                DEATH_ID,
                100
        );

        String linked =
                tracker.observeAccountTypeChange(
                        ACCOUNT,
                        100 +
                                Slice5HardcoreTransitionTracker.MAX_LINK_TICK_DELTA +
                                1,
                        1
                );

        assertNull(linked);
        assertFalse(tracker.hasPendingDeath());
    }

    @Test
    public void differentAccountTypeTransitionBreaksDirectChain()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                ACCOUNT,
                DEATH_ID,
                100
        );

        assertNull(
                tracker.observeAccountTypeChange(
                        ACCOUNT,
                        105,
                        2
                )
        );

        assertFalse(
                tracker.hasPendingDeath()
        );

        assertNull(
                tracker.observeAccountTypeChange(
                        ACCOUNT,
                        106,
                        1
                )
        );
    }

    @Test
    public void newDeathReplacesOlderPendingDeath()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                "Red Helm",
                "11111111-2222-4333-8444-555555555555",
                100
        );

        tracker.armDeath(
                ACCOUNT,
                DEATH_ID,
                120
        );

        assertTrue(
                tracker.hasPendingDeath()
        );

        assertEquals(
                "bluehelm",
                tracker.getPendingAccount()
        );

        assertEquals(
                DEATH_ID,
                tracker.getPendingDeathEventId()
        );

        assertEquals(
                120,
                tracker.getPendingDeathTick()
        );
    }

    @Test
    public void transitionBeforeDeathTickCannotLink()
    {
        Slice5HardcoreTransitionTracker tracker =
                new Slice5HardcoreTransitionTracker();

        tracker.armDeath(
                ACCOUNT,
                DEATH_ID,
                200
        );

        assertNull(
                tracker.observeAccountTypeChange(
                        ACCOUNT,
                        199,
                        1
                )
        );

        assertFalse(
                tracker.hasPendingDeath()
        );
    }
}