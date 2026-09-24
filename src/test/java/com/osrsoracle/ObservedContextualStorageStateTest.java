package com.osrsoracle;

import org.junit.Test;
import static org.junit.Assert.*;

public class ObservedContextualStorageStateTest
{
    @Test
    public void distinguishesUnknownEmptyAndPopulated()
    {
        ObservedContextualStorageState state =
                new ObservedContextualStorageState();

        assertTrue(state.toJson().contains("\"observedAt\":null"));
        assertTrue(state.toJson().contains("\"items\":null"));

        assertTrue(
                state.observe(
                        new ObservedContextualStorageState.Entry[0],
                        "2026-09-23T20:00:00Z"
                )
        );

        assertTrue(state.toJson().contains(
                "\"observedAt\":\"2026-09-23T20:00:00Z\""
        ));
        assertTrue(state.toJson().contains("\"items\":[]"));

        assertTrue(
                state.observe(
                        new ObservedContextualStorageState.Entry[] {
                            new ObservedContextualStorageState.Entry(
                                    "Grimy avantoe",
                                    8
                            )
                        },
                        "2026-09-23T20:01:00Z"
                )
        );

        assertTrue(state.toJson().contains(
                "\"nameRaw\":\"Grimy avantoe\",\"quantity\":8"
        ));
    }

    @Test
    public void rejectsOlderObservation()
    {
        ObservedContextualStorageState state =
                new ObservedContextualStorageState();

        assertTrue(
                state.observe(
                        new ObservedContextualStorageState.Entry[0],
                        "2026-09-23T20:02:00Z"
                )
        );

        assertFalse(
                state.observe(
                        new ObservedContextualStorageState.Entry[] {
                            new ObservedContextualStorageState.Entry(
                                    "Coal",
                                    15
                            )
                        },
                        "2026-09-23T20:01:00Z"
                )
        );

        assertTrue(state.toJson().contains("\"items\":[]"));
    }
}
