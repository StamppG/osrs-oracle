package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ObservedPotionStorageStateTest
{
    @Test
    public void startsUnobserved()
    {
        ObservedPotionStorageState state =
                new ObservedPotionStorageState();

        assertFalse(state.hasObservation());
        assertNull(state.getEntries());
        assertNull(state.getObservedAt());
    }

    @Test
    public void retainsObservedPotionState()
    {
        ObservedPotionStorageState state =
                new ObservedPotionStorageState();

        assertTrue(
                state.observe(
                        new ObservedPotionStorageState.Entry[] {
                            new ObservedPotionStorageState.Entry(
                                    2434,
                                    127,
                                    ObservedPotionStorageState.AMOUNT_DOSES
                            ),
                            new ObservedPotionStorageState.Entry(
                                    229,
                                    42,
                                    ObservedPotionStorageState.AMOUNT_QUANTITY
                            )
                        },
                        "2026-09-21T07:00:00Z"
                )
        );

        ObservedPotionStorageState.Entry[] entries =
                state.getEntries();

        assertEquals(2, entries.length);
        assertEquals(2434, entries[0].getItemId());
        assertEquals(127, entries[0].getAmount());
        assertEquals(
                ObservedPotionStorageState.AMOUNT_DOSES,
                entries[0].getAmountType()
        );
        assertEquals(229, entries[1].getItemId());
        assertEquals(42, entries[1].getAmount());
        assertEquals(
                ObservedPotionStorageState.AMOUNT_QUANTITY,
                entries[1].getAmountType()
        );
        assertEquals(
                "2026-09-21T07:00:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void observedEmptySupersedesPriorContents()
    {
        ObservedPotionStorageState state =
                new ObservedPotionStorageState();

        state.observe(
                new ObservedPotionStorageState.Entry[] {
                    new ObservedPotionStorageState.Entry(
                            2434,
                            127,
                            ObservedPotionStorageState.AMOUNT_DOSES
                    )
                },
                "2026-09-21T07:00:00Z"
        );

        assertTrue(
                state.observe(
                        new ObservedPotionStorageState.Entry[0],
                        "2026-09-21T07:01:00Z"
                )
        );

        assertEquals(0, state.getEntries().length);
        assertEquals(
                "2026-09-21T07:01:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void staleObservationCannotRegressState()
    {
        ObservedPotionStorageState state =
                new ObservedPotionStorageState();

        state.observe(
                new ObservedPotionStorageState.Entry[] {
                    new ObservedPotionStorageState.Entry(
                            2434,
                            127,
                            ObservedPotionStorageState.AMOUNT_DOSES
                    )
                },
                "2026-09-21T07:05:00Z"
        );

        assertFalse(
                state.observe(
                        new ObservedPotionStorageState.Entry[] {
                            new ObservedPotionStorageState.Entry(
                                    2434,
                                    50,
                                    ObservedPotionStorageState.AMOUNT_DOSES
                            )
                        },
                        "2026-09-21T07:04:59Z"
                )
        );

        assertEquals(
                127,
                state.getEntries()[0].getAmount()
        );
        assertEquals(
                "2026-09-21T07:05:00Z",
                state.getObservedAt()
        );
    }

    @Test
    public void returnedArrayCannotMutateCachedObservation()
    {
        ObservedPotionStorageState state =
                new ObservedPotionStorageState();

        state.observe(
                new ObservedPotionStorageState.Entry[] {
                    new ObservedPotionStorageState.Entry(
                            2434,
                            127,
                            ObservedPotionStorageState.AMOUNT_DOSES
                    )
                },
                "2026-09-21T07:00:00Z"
        );

        ObservedPotionStorageState.Entry[] copy =
                state.getEntries();

        copy[0] =
                new ObservedPotionStorageState.Entry(
                        229,
                        999,
                        ObservedPotionStorageState.AMOUNT_QUANTITY
                );

        ObservedPotionStorageState.Entry[] retained =
                state.getEntries();

        assertEquals(2434, retained[0].getItemId());
        assertEquals(127, retained[0].getAmount());
    }

    @Test
    public void resetClearsObservation()
    {
        ObservedPotionStorageState state =
                new ObservedPotionStorageState();

        state.observe(
                new ObservedPotionStorageState.Entry[] {
                    new ObservedPotionStorageState.Entry(
                            2434,
                            127,
                            ObservedPotionStorageState.AMOUNT_DOSES
                    )
                },
                "2026-09-21T07:00:00Z"
        );

        state.reset();

        assertFalse(state.hasObservation());
        assertNull(state.getEntries());
        assertNull(state.getObservedAt());
    }
}
