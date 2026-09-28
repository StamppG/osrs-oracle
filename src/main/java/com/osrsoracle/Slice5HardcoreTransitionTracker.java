package com.osrsoracle;

import java.util.Locale;

final class Slice5HardcoreTransitionTracker
{
    static final int MAX_LINK_TICK_DELTA = 20;

    private String pendingAccount;
    private String pendingDeathEventId;
    private int pendingDeathTick = Integer.MIN_VALUE;

    void armDeath(
            String account,
            String deathEventId,
            int deathTick
    )
    {
        pendingAccount =
                normalizeAccount(account);

        if (
                deathEventId == null ||
                        deathEventId.isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "deathEventId"
            );
        }

        pendingDeathEventId =
                deathEventId;

        pendingDeathTick =
                deathTick;
    }

    String observeAccountTypeChange(
            String account,
            int currentTick,
            int newRawAccountType
    )
    {
        if (pendingDeathEventId == null)
        {
            return null;
        }

        String normalizedAccount =
                normalizeAccount(account);

        /*
         * A pending Hardcore death belongs to exactly one observed character.
         * Never allow a later account/session transition to consume another
         * character's death correlation.
         */
        if (!pendingAccount.equals(normalizedAccount))
        {
            clear();
            return null;
        }

        int delta =
                currentTick - pendingDeathTick;

        if (
                delta < 0 ||
                        delta > MAX_LINK_TICK_DELTA
        )
        {
            clear();
            return null;
        }

        /*
         * The death boundary itself proved raw type 3.
         *
         * Only the next directly observed ACCOUNT_TYPE transition to raw 1
         * may establish the frozen production HCIM 3 -> 1 status-loss fact.
         *
         * Any other account-type change breaks that direct chain.
         */
        if (newRawAccountType != 1)
        {
            clear();
            return null;
        }

        String linkedDeathEventId =
                pendingDeathEventId;

        clear();

        return linkedDeathEventId;
    }

    void expire(
            int currentTick
    )
    {
        if (pendingDeathEventId == null)
        {
            return;
        }

        int delta =
                currentTick - pendingDeathTick;

        if (
                delta < 0 ||
                        delta > MAX_LINK_TICK_DELTA
        )
        {
            clear();
        }
    }

    void clear()
    {
        pendingAccount = null;
        pendingDeathEventId = null;
        pendingDeathTick = Integer.MIN_VALUE;
    }

    boolean hasPendingDeath()
    {
        return pendingDeathEventId != null;
    }

    String getPendingAccount()
    {
        return pendingAccount;
    }

    String getPendingDeathEventId()
    {
        return pendingDeathEventId;
    }

    int getPendingDeathTick()
    {
        return pendingDeathTick;
    }

    private static String normalizeAccount(
            String account
    )
    {
        if (
                account == null ||
                        account.isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "account"
            );
        }

        String normalized =
                account
                        .toLowerCase(Locale.ROOT)
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        if (normalized.isEmpty())
        {
            throw new IllegalArgumentException(
                    "account"
            );
        }

        return normalized;
    }
}