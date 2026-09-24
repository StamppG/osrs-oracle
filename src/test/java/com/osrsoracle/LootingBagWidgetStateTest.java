package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LootingBagWidgetStateTest
{
    @Test
    public void visibleWidgetWithNoRealItemsIsAuthoritativeEmpty()
    {
        assertTrue(
                LootingBagWidgetState.isAuthoritativeEmpty(
                        true,
                        new int[] {-1, 0, -1}
                )
        );
    }

    @Test
    public void visibleWidgetWithRealItemIsNotEmpty()
    {
        assertFalse(
                LootingBagWidgetState.isAuthoritativeEmpty(
                        true,
                        new int[] {-1, 250, -1}
                )
        );
    }

    @Test
    public void hiddenWidgetCannotEstablishEmptyState()
    {
        assertFalse(
                LootingBagWidgetState.isAuthoritativeEmpty(
                        false,
                        new int[] {-1, 0, -1}
                )
        );
    }

    @Test
    public void missingWidgetItemsCannotEstablishEmptyState()
    {
        assertFalse(
                LootingBagWidgetState.isAuthoritativeEmpty(
                        true,
                        null
                )
        );
    }
}
