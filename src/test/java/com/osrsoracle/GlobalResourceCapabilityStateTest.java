package com.osrsoracle;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import static org.junit.Assert.assertTrue;

public class GlobalResourceCapabilityStateTest
{
    @Test
    public void serializesRawResourceAndCapabilityEvidence()
    {
        Map<Integer, Integer> varbits = new HashMap<>();
        Map<Integer, Integer> varps = new HashMap<>();

        varbits.put(VarbitID.SLAYER_POINTS, 742);
        varbits.put(VarbitID.SLAYER_TASKS_COMPLETED, 88);
        varbits.put(VarbitID.HOSIDIUS_TITHE_REWARDPOINTS, 31);

        varps.put(VarPlayerID.NZONE_REWARDPOINTS, 1234567);
        varps.put(VarPlayerID.SOUL_WARS_ZEAL, 94);
        varps.put(
                VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS,
                812
        );
        varps.put(VarPlayerID.MIXOLOGY_MOX_POINTS, 11);
        varps.put(VarPlayerID.MIXOLOGY_AGA_POINTS, 22);
        varps.put(VarPlayerID.MIXOLOGY_LYE_POINTS, 33);
        varps.put(VarPlayerID.CAMDOZAAL_STORED_BARRONITE, 444);
        varps.put(VarPlayerID.SCAR_ESSENCEMINE_COFFER, 555);
        varps.put(VarPlayerID.COLOSSEUM_GLORY, 666);
        varps.put(VarPlayerID.BH_2023_POINTS, 77);

        varps.put(VarPlayerID.SLAYER_REWARDS_UNLOCKS, -1);
        varps.put(VarPlayerID.SLAYER_REWARDS_UNLOCKS1, 123);
        varps.put(VarPlayerID.SLAYER_REWARDS_UNLOCKS2, 456);
        varps.put(VarPlayerID.SLAYER_STORED_VARP, 789);

        varbits.put(VarbitID.SLAYER_UNLOCK_STORAGE, 1);
        varbits.put(VarbitID.PRAYERBOOK, 0);
        varbits.put(VarbitID.KR_KNIGHTWAVES_STATE, 8);
        varbits.put(VarbitID.PRAYER_RIGOUR_UNLOCKED, 1);
        varbits.put(VarbitID.PRAYER_AUGURY_UNLOCKED, 1);
        varbits.put(VarbitID.PRAYER_PRESERVE_UNLOCKED, 1);
        varbits.put(VarbitID.PRAYER_DEADEYE_UNLOCKED, 0);
        varbits.put(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED, 1);

        varbits.put(VarbitID.SPELLBOOK, 3);
        varbits.put(VarbitID.SPELLBOOK_SUBLIST, 9);
        varbits.put(VarbitID.ARCEUUS_SPELLBOOK_UNLOCKED, 1);
        varbits.put(VarbitID.MAGICTRAINING_BONESPEACHES, 1);
        varbits.put(VarbitID.FARMING_BLOCKWEEDS, 1);

        varbits.put(VarbitID.LOVAKENGJ_MINECARTS_STATUS, 27);
        varbits.put(VarbitID.FAIRYRING_PERMISSION, 1);
        varbits.put(VarbitID.ZEAH_FAIRYRING_CIS_UNLOCKED, 1);
        varps.put(VarPlayerID.QUETZALS_UNLOCKED, 1023);
        varbits.put(VarbitID.QUETZAL_KASTORI, 1);

        varbits.put(VarbitID.GOTR_UNLOCKED_NEEDLE, 1);
        varbits.put(VarbitID.GOTR_BAG_OBTAINED, 1);

        varbits.put(
                VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_7,
                1
        );
        varbits.put(
                VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_11,
                1
        );
        varbits.put(
                VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_10,
                1
        );

        varbits.put(VarbitID.MOTHERLODE_SACK_TRANSMIT, 52);
        varbits.put(VarbitID.MOTHERLODE_BIGGERSACK, 1);

        String json = GlobalResourceCapabilityState.collect(
                new GlobalResourceCapabilityState.ValueSource()
                {
                    @Override
                    public int varbit(int id)
                    {
                        return varbits.getOrDefault(id, 0);
                    }

                    @Override
                    public int varp(int id)
                    {
                        return varps.getOrDefault(id, 0);
                    }
                }
        );

        assertTrue(json.contains(
                "\"slayer\":{\"points\":742,\"taskStreak\":88}"
        ));
        assertTrue(json.contains(
                "\"nightmareZone\":{\"rewardPoints\":1234567}"
        ));
        assertTrue(json.contains(
                "\"masteringMixology\":{\"moxPoints\":11," +
                        "\"agaPoints\":22,\"lyePoints\":33}"
        ));
        assertTrue(json.contains("\"storedBarronite\":444"));
        assertTrue(json.contains("\"coffer\":555"));
        assertTrue(json.contains("\"glory\":666"));
        assertTrue(json.contains("\"points\":77"));

        assertTrue(json.contains(
                "\"unlockWord0\":4294967295"
        ));
        assertTrue(json.contains("\"unlockWord1\":123"));
        assertTrue(json.contains("\"unlockWord2\":456"));
        assertTrue(json.contains("\"storageUnlockedRaw\":1"));
        assertTrue(json.contains("\"storedTaskRaw\":789"));

        assertTrue(json.contains("\"knightWavesStateRaw\":8"));
        assertTrue(json.contains("\"rigourUnlockedRaw\":1"));
        assertTrue(json.contains("\"auguryUnlockedRaw\":1"));
        assertTrue(json.contains("\"preserveUnlockedRaw\":1"));
        assertTrue(json.contains("\"deadeyeUnlockedRaw\":0"));
        assertTrue(json.contains("\"mysticVigourUnlockedRaw\":1"));

        assertTrue(json.contains("\"currentSpellbookRaw\":3"));
        assertTrue(json.contains("\"currentSpellbookSublistRaw\":9"));
        assertTrue(json.contains(
                "\"arceuusSpellbookUnlockedRaw\":1"
        ));
        assertTrue(json.contains(
                "\"bonesToPeachesUnlockedRaw\":1"
        ));
        assertTrue(json.contains("\"autoWeedRaw\":1"));

        assertTrue(json.contains("\"lovakengjMinecartsRaw\":27"));
        assertTrue(json.contains("\"fairyRingPermissionRaw\":1"));
        assertTrue(json.contains("\"fairyRingCisUnlockedRaw\":1"));
        assertTrue(json.contains("\"quetzalUnlockWordRaw\":1023"));
        assertTrue(json.contains("\"kastori\":1"));

        assertTrue(json.contains("\"needleUnlockedRaw\":1"));
        assertTrue(json.contains("\"bagObtainedRaw\":1"));

        assertTrue(json.contains("\"ricasso7\":1"));
        assertTrue(json.contains("\"blade11\":1"));
        assertTrue(json.contains("\"tip10\":1"));

        assertTrue(json.contains("\"biggerSackRaw\":1"));
    }

    @Test
    public void preservesObservedZeroAsZero()
    {
        String json = GlobalResourceCapabilityState.collect(
                new GlobalResourceCapabilityState.ValueSource()
                {
                    @Override
                    public int varbit(int id)
                    {
                        return 0;
                    }

                    @Override
                    public int varp(int id)
                    {
                        return 0;
                    }
                }
        );

        assertTrue(json.contains("\"points\":0"));
        assertTrue(json.contains("\"rigourUnlockedRaw\":0"));
        assertTrue(json.contains("\"currentSpellbookRaw\":0"));
        assertTrue(json.contains("\"storedBarronite\":0"));
    }
}