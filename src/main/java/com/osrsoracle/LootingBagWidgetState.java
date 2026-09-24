package com.osrsoracle;

final class LootingBagWidgetState
{
    private LootingBagWidgetState()
    {
    }

    static boolean isAuthoritativeEmpty(
            boolean visible,
            int[] itemIds
    )
    {
        if (!visible || itemIds == null)
        {
            return false;
        }

        for (int itemId : itemIds)
        {
            if (itemId > 0)
            {
                return false;
            }
        }

        return true;
    }
}
