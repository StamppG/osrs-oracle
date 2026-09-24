package com.osrsoracle;

import java.util.function.IntPredicate;
import net.runelite.api.Item;

final class OwnedItemContainerNormalizer
{
    private OwnedItemContainerNormalizer()
    {
    }

    static Item[] withoutPlaceholders(
            Item[] items,
            IntPredicate isPlaceholder
    )
    {
        if (items == null)
        {
            return null;
        }

        Item[] normalized = items.clone();

        for (int slot = 0; slot < normalized.length; slot++)
        {
            Item item = normalized[slot];

            if (
                    item != null &&
                    item.getId() > 0 &&
                    isPlaceholder.test(item.getId())
            )
            {
                normalized[slot] = null;
            }
        }

        return normalized;
    }
}
