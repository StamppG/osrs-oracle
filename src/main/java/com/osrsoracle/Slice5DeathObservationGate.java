package com.osrsoracle;

final class Slice5DeathObservationGate
{
    private Slice5DeathObservationGate()
    {
    }

    static boolean isLocalActor(
            Object actor,
            Object localPlayer
    )
    {
        return actor != null &&
                localPlayer != null &&
                actor == localPlayer;
    }

    static boolean isDuplicate(
            String previousAccount,
            int previousTick,
            String currentAccount,
            int currentTick
    )
    {
        if (
                previousAccount == null ||
                        currentAccount == null ||
                        previousAccount.isBlank() ||
                        currentAccount.isBlank()
        )
        {
            return false;
        }

        return previousTick == currentTick &&
                previousAccount.equals(currentAccount);
    }
}