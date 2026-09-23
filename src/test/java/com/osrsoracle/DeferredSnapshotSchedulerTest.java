package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeferredSnapshotSchedulerTest
{
    @Test
    public void startsEmpty()
    {
        DeferredSnapshotScheduler scheduler =
                new DeferredSnapshotScheduler();

        assertFalse(scheduler.hasPending());
        assertFalse(scheduler.isDue(100));
        assertNull(scheduler.takeIfDue(100));
    }

    @Test
    public void zeroSettleCanBecomeDueImmediately()
    {
        DeferredSnapshotScheduler scheduler =
                new DeferredSnapshotScheduler();

        scheduler.schedule("HEARTBEAT", 100, 0);

        assertTrue(scheduler.isDue(100));
        assertEquals(100L, scheduler.getDueTick());
    }

    @Test
    public void delayedSnapshotWaitsForDueTick()
    {
        DeferredSnapshotScheduler scheduler =
                new DeferredSnapshotScheduler();

        scheduler.schedule("LOGIN", 100, 3);

        assertFalse(scheduler.isDue(102));
        assertTrue(scheduler.isDue(103));
    }

    @Test
    public void newestReasonWinsWithoutCreatingQueue()
    {
        DeferredSnapshotScheduler scheduler =
                new DeferredSnapshotScheduler();

        scheduler.schedule("VARBIT", 100, 0);
        scheduler.schedule("POTION_STORAGE", 100, 1);

        assertEquals(
                "POTION_STORAGE",
                scheduler.getPendingReason()
        );
    }

    @Test
    public void laterSafetyRequirementExtendsPendingSnapshot()
    {
        DeferredSnapshotScheduler scheduler =
                new DeferredSnapshotScheduler();

        scheduler.schedule("VARBIT", 100, 0);
        scheduler.schedule("WORLD_HOP", 100, 3);

        assertEquals(103L, scheduler.getDueTick());
        assertFalse(scheduler.isDue(102));
    }

    @Test
    public void shorterNewRequestCannotReduceExistingSafetyDelay()
    {
        DeferredSnapshotScheduler scheduler =
                new DeferredSnapshotScheduler();

        scheduler.schedule("WORLD_HOP", 100, 3);
        scheduler.schedule("HEARTBEAT", 101, 0);

        assertEquals("HEARTBEAT", scheduler.getPendingReason());
        assertEquals(103L, scheduler.getDueTick());
    }

    @Test
    public void takingDueSnapshotClearsPendingState()
    {
        DeferredSnapshotScheduler scheduler =
                new DeferredSnapshotScheduler();

        scheduler.schedule("POTION_STORAGE", 100, 1);

        assertEquals(
                "POTION_STORAGE",
                scheduler.takeIfDue(101)
        );
        assertFalse(scheduler.hasPending());
        assertEquals(-1L, scheduler.getDueTick());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankReason()
    {
        new DeferredSnapshotScheduler().schedule(" ", 100, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeSettleTicks()
    {
        new DeferredSnapshotScheduler().schedule(
                "TEST",
                100,
                -1
        );
    }
}
