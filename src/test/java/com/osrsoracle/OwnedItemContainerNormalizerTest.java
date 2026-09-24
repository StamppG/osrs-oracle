package com.osrsoracle;

import net.runelite.api.Item;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OwnedItemContainerNormalizerTest
{
    @Test
    public void placeholderIsExcludedButRealQuantityOneIsPreserved()
    {
        Item placeholder = new Item(1001, 1);
        Item realQuantityOne = new Item(1002, 1);
        Item realStack = new Item(1003, 41);

        Item[] source = {
                placeholder,
                realQuantityOne,
                realStack,
                null
        };

        Item[] normalized =
                OwnedItemContainerNormalizer.withoutPlaceholders(
                        source,
                        itemId -> itemId == 1001
                );

        assertNotNull(normalized);
        assertEquals(4, normalized.length);
        assertNull(normalized[0]);

        assertNotNull(normalized[1]);
        assertEquals(1002, normalized[1].getId());
        assertEquals(1, normalized[1].getQuantity());

        assertNotNull(normalized[2]);
        assertEquals(1003, normalized[2].getId());
        assertEquals(41, normalized[2].getQuantity());

        assertNull(normalized[3]);

        assertNotNull(source[0]);
        assertEquals(1001, source[0].getId());
    }

    @Test
    public void missingSourceRemainsMissing()
    {
        assertNull(
                OwnedItemContainerNormalizer.withoutPlaceholders(
                        null,
                        itemId -> false
                )
        );
    }
}
