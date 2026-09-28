package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AccountTypeResolutionTest
{
    @Test
    public void freshTutorialZeroIsUnresolvedNotNormal()
    {
        AccountTypeResolution state =
                AccountTypeResolution.resolve(
                        0,
                        12336,
                        "2026-09-27T12:00:00Z"
                );

        assertEquals(
                AccountTypeResolution.Status.UNRESOLVED_ONBOARDING,
                state.getStatus()
        );
        assertNull(state.getOfficialAccountType());
        assertNull(state.getProvisionalAccountType());
        assertEquals(Integer.valueOf(0), state.getRawAccountType());
        assertEquals(Boolean.TRUE, state.getInTutorialIsland());
        assertEquals("null", state.getOfficialAccountTypeJson());
    }

    @Test
    public void tutorialIronmanSelectionIsProvisional()
    {
        AccountTypeResolution state =
                AccountTypeResolution.resolve(
                        1,
                        12336,
                        "2026-09-27T12:00:01Z"
                );

        assertEquals(
                AccountTypeResolution.Status.PROVISIONAL_ONBOARDING,
                state.getStatus()
        );
        assertNull(state.getOfficialAccountType());
        assertEquals(
                "IRONMAN",
                state.getProvisionalAccountType()
        );
    }

    @Test
    public void tutorialHardcoreSelectionIsStillProvisional()
    {
        AccountTypeResolution state =
                AccountTypeResolution.resolve(
                        3,
                        12592,
                        "2026-09-27T12:00:02Z"
                );

        assertEquals(
                AccountTypeResolution.Status.PROVISIONAL_ONBOARDING,
                state.getStatus()
        );
        assertNull(state.getOfficialAccountType());
        assertEquals(
                "HARDCORE_IRONMAN",
                state.getProvisionalAccountType()
        );
    }

    @Test
    public void leavingTutorialFinalizesHardcore()
    {
        AccountTypeResolution state =
                AccountTypeResolution.resolve(
                        3,
                        12850,
                        "2026-09-27T12:00:03Z"
                );

        assertEquals(
                AccountTypeResolution.Status.RESOLVED,
                state.getStatus()
        );
        assertEquals(
                "HARDCORE_IRONMAN",
                state.getOfficialAccountType()
        );
        assertNull(state.getProvisionalAccountType());
        assertEquals(Boolean.FALSE, state.getInTutorialIsland());
        assertEquals(
                "\"HARDCORE_IRONMAN\"",
                state.getOfficialAccountTypeJson()
        );
    }

    @Test
    public void normalOutsideTutorialIsResolvedNormal()
    {
        AccountTypeResolution state =
                AccountTypeResolution.resolve(
                        0,
                        12850,
                        "2026-09-27T12:00:04Z"
                );

        assertEquals(
                AccountTypeResolution.Status.RESOLVED,
                state.getStatus()
        );
        assertEquals(
                "NORMAL",
                state.getOfficialAccountType()
        );
    }

    @Test
    public void unknownRawCodeDoesNotForceAccountType()
    {
        AccountTypeResolution state =
                AccountTypeResolution.resolve(
                        99,
                        12850,
                        "2026-09-27T12:00:05Z"
                );

        assertEquals(
                AccountTypeResolution.Status.UNKNOWN,
                state.getStatus()
        );
        assertNull(state.getOfficialAccountType());
        assertNull(state.getProvisionalAccountType());
        assertEquals(Integer.valueOf(99), state.getRawAccountType());
        assertEquals(Boolean.FALSE, state.getInTutorialIsland());

        String json = state.toJson();

        assertTrue(json.contains("\"status\":\"UNKNOWN\""));
        assertTrue(json.contains("\"rawAccountType\":99"));
        assertTrue(json.contains("\"observedAt\":\"2026-09-27T12:00:05Z\""));
    }

    @Test
    public void missingTutorialDeterminationIsUnknown()
    {
        AccountTypeResolution state =
                AccountTypeResolution.resolve(
                        3,
                        null,
                        "2026-09-27T12:00:06Z"
                );

        assertEquals(
                AccountTypeResolution.Status.UNKNOWN,
                state.getStatus()
        );
        assertNull(state.getOfficialAccountType());
        assertNull(state.getInTutorialIsland());
    }
}