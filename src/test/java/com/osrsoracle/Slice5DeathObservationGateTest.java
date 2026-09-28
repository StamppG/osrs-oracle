package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Slice5DeathObservationGateTest
{
    @Test
    public void acceptsOnlyExactLocalActorIdentity()
    {
        Object localPlayer =
                new Object();

        Object otherActor =
                new Object();

        assertTrue(
                Slice5DeathObservationGate.isLocalActor(
                        localPlayer,
                        localPlayer
                )
        );

        assertFalse(
                Slice5DeathObservationGate.isLocalActor(
                        otherActor,
                        localPlayer
                )
        );

        assertFalse(
                Slice5DeathObservationGate.isLocalActor(
                        null,
                        localPlayer
                )
        );

        assertFalse(
                Slice5DeathObservationGate.isLocalActor(
                        localPlayer,
                        null
                )
        );
    }

    @Test
    public void sameAccountSameTickIsDuplicate()
    {
        assertTrue(
                Slice5DeathObservationGate.isDuplicate(
                        "BlueHelmSolo",
                        257,
                        "BlueHelmSolo",
                        257
                )
        );
    }

    @Test
    public void differentAccountSameTickIsNotDuplicate()
    {
        assertFalse(
                Slice5DeathObservationGate.isDuplicate(
                        "BlueHelmSolo",
                        257,
                        "99AweTism",
                        257
                )
        );
    }

    @Test
    public void sameAccountDifferentTickIsNotDuplicate()
    {
        assertFalse(
                Slice5DeathObservationGate.isDuplicate(
                        "BlueHelmSolo",
                        257,
                        "BlueHelmSolo",
                        258
                )
        );
    }
}