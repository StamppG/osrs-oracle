package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Slice5FinalLivingCoverageTest
{
    @Test
    public void preservesRetainedFreshnessInsteadOfRestampingDeathTime()
    {
        String deathAt =
                "2026-09-27T20:00:00Z";

        String bankAt =
                "2026-09-27T18:15:00Z";

        String clogAt =
                "2026-09-26T14:30:00Z";

        String json =
                Slice5FinalLivingCoverage.serialize(
                        deathAt,
                        bankAt,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        clogAt,
                        17
                );

        assertTrue(
                json.contains(
                        "\"liveState\":{\"status\":\"OBSERVED\",\"observedAt\":\"" +
                                deathAt +
                                "\""
                )
        );

        assertTrue(
                json.contains(
                        "\"bank\":{\"status\":\"OBSERVED\",\"observedAt\":\"" +
                                bankAt +
                                "\"}"
                )
        );

        assertTrue(
                json.contains(
                        "\"seedVault\":{\"status\":\"NOT_OBSERVED\",\"observedAt\":null}"
                )
        );

        assertTrue(
                json.contains(
                        "\"collectionLogPages\":{\"status\":\"PARTIAL\",\"observedAt\":\"" +
                                clogAt +
                                "\",\"pagesObserved\":17}"
                )
        );

        /*
         * Bank and Collection Log must retain their own older freshness.
         * The death timestamp only belongs to genuinely live death-time facts.
         */
        assertFalse(
                json.contains(
                        "\"bank\":{\"status\":\"OBSERVED\",\"observedAt\":\"" +
                                deathAt +
                                "\"}"
                )
        );

        assertFalse(
                json.contains(
                        "\"collectionLogPages\":{\"status\":\"PARTIAL\",\"observedAt\":\"" +
                                deathAt +
                                "\""
                )
        );
    }

    @Test
    public void excludesCanonicalDeathAndTransportCoverage()
    {
        String json =
                Slice5FinalLivingCoverage.serialize(
                        "2026-09-27T20:00:00Z",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0
                );

        assertFalse(
                json.contains(
                        "\"accountType\":"
                )
        );

        assertFalse(
                json.contains(
                        "\"accountTypeResolution\":"
                )
        );

        assertFalse(
                json.contains(
                        "\"inventory\":"
                )
        );

        assertFalse(
                json.contains(
                        "\"equipment\":"
                )
        );

        assertFalse(
                json.contains(
                        "\"events\":"
                )
        );

        assertFalse(
                json.contains(
                        "\"slice5CoverageGaps\":"
                )
        );

        assertFalse(
                json.contains(
                        "\"slice5EventCoverage\":"
                )
        );

        assertFalse(
                json.contains(
                        "\"collectionLogInstant\":"
                )
        );
    }

    @Test
    public void zeroCollectionLogPagesIsNotObserved()
    {
        String json =
                Slice5FinalLivingCoverage.serialize(
                        "2026-09-27T20:00:00Z",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0
                );

        assertTrue(
                json.contains(
                        "\"collectionLogPages\":{\"status\":\"NOT_OBSERVED\",\"observedAt\":null,\"pagesObserved\":0}"
                )
        );
    }
}