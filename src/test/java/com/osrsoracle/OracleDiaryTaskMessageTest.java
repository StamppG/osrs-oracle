package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OracleDiaryTaskMessageTest
{
    @Test
    public void recognizesIndividualDiaryTaskCompletion()
    {
        assertTrue(
                OraclePlugin.isIndividualDiaryTaskMessage(
                        "well done! you have completed a hard task in the varrock area. " +
                                "your achievement diary has been updated."
                )
        );
    }

    @Test
    public void rejectsSimilarNonTaskMessages()
    {
        assertFalse(
                OraclePlugin.isIndividualDiaryTaskMessage(
                        "congratulations! you have completed all of the hard tasks " +
                                "in the varrock area."
                )
        );

        assertFalse(
                OraclePlugin.isIndividualDiaryTaskMessage(
                        "well done! you have completed a hard task in the varrock area."
                )
        );
    }
}
