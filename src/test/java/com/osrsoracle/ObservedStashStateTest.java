package com.osrsoracle;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObservedStashStateTest
{
    @Test
    public void completeObservationReplacesPriorState()
    {
        ObservedStashState state =
                new ObservedStashState();

        assertTrue(
                state.observeComplete(
                        List.of(
                                new ObservedStashState.Entry(
                                        1001,
                                        true,
                                        false
                                )
                        ),
                        "2026-09-20T20:00:00Z"
                )
        );

        assertTrue(
                state.observeComplete(
                        List.of(
                                new ObservedStashState.Entry(
                                        1001,
                                        true,
                                        true
                                ),
                                new ObservedStashState.Entry(
                                        1002,
                                        false,
                                        false
                                )
                        ),
                        "2026-09-20T20:01:00Z"
                )
        );

        assertEquals(
                2,
                state.getEntries().size()
        );

        assertTrue(
                state.toJson().contains(
                        "\"objectId\":1001,\"built\":true,\"filled\":true"
                )
        );

        assertTrue(
                state.toJson().contains(
                        "\"objectId\":1002,\"built\":false,\"filled\":false"
                )
        );
    }

    @Test
    public void staleObservationCannotRegressState()
    {
        ObservedStashState state =
                new ObservedStashState();

        assertTrue(
                state.observeComplete(
                        List.of(
                                new ObservedStashState.Entry(
                                        2001,
                                        true,
                                        true
                                )
                        ),
                        "2026-09-20T20:02:00Z"
                )
        );

        assertFalse(
                state.observeComplete(
                        List.of(
                                new ObservedStashState.Entry(
                                        2001,
                                        false,
                                        false
                                )
                        ),
                        "2026-09-20T20:01:00Z"
                )
        );

        assertTrue(
                state.toJson().contains(
                        "\"objectId\":2001,\"built\":true,\"filled\":true"
                )
        );
    }

    @Test
    public void observationIsDefensivelyCopied()
    {
        ObservedStashState state =
                new ObservedStashState();

        List<ObservedStashState.Entry> source =
                new ArrayList<>();

        source.add(
                new ObservedStashState.Entry(
                        3001,
                        true,
                        false
                )
        );

        assertTrue(
                state.observeComplete(
                        source,
                        "2026-09-20T20:03:00Z"
                )
        );

        source.clear();

        assertEquals(
                1,
                state.getEntries().size()
        );
    }

    @Test
    public void resetReturnsToNotObserved()
    {
        ObservedStashState state =
                new ObservedStashState();

        state.observeComplete(
                List.of(),
                "2026-09-20T20:04:00Z"
        );

        state.reset();

        assertEquals(
                null,
                state.getObservedAt()
        );

        assertEquals(
                "null",
                state.toJson()
        );
    }
}
