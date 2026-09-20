package com.osrsoracle;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import static org.junit.Assert.assertTrue;

public class PersistentStorageEvidenceTest
{
    @Test
    public void serializesPersistentStorageEvidence()
    {
        Map<Integer, Integer> varbits = new HashMap<>();
        Map<Integer, Integer> varps = new HashMap<>();

        varbits.put(VarbitID.RUNE_POUCH_TYPE_1, 9);
        varbits.put(VarbitID.RUNE_POUCH_QUANTITY_1, 2500);
        varbits.put(VarbitID.RUNE_POUCH_TYPE_4, 13);
        varbits.put(VarbitID.RUNE_POUCH_QUANTITY_4, 900);

        varbits.put(VarbitID.SMALL_ESSENCE_POUCH, 3);
        varbits.put(VarbitID.MEDIUM_ESSENCE_POUCH, 6);
        varbits.put(VarbitID.LARGE_ESSENCE_POUCH, 9);
        varbits.put(VarbitID.GIANT_ESSENCE_POUCH, 12);
        varbits.put(VarbitID.COLOSSAL_ESSENCE_POUCH, 40);

        varbits.put(VarbitID.SMALL_ESSENCE_POUCH_TYPE, 1);
        varbits.put(VarbitID.MEDIUM_ESSENCE_POUCH_TYPE, 1);
        varbits.put(VarbitID.LARGE_ESSENCE_POUCH_TYPE, 1);
        varbits.put(VarbitID.GIANT_ESSENCE_POUCH_TYPE, 1);
        varbits.put(VarbitID.COLOSSAL_ESSENCE_POUCH_TYPE, 1);

        varps.put(VarPlayerID.RCU_POUCH_DEGRADATION_MED, 100);
        varps.put(VarPlayerID.RCU_POUCH_DEGRADATION_LARGE, 200);
        varps.put(VarPlayerID.RCU_POUCH_DEGRADATION_GIANT, 300);
        varbits.put(VarbitID.RCU_POUCH_DEGRADATION_COLOSSAL, 400);

        varbits.put(VarbitID.XBOWS_POUCH_SLOT1, 7);
        varbits.put(VarbitID.XBOWS_POUCH_NUM1, 125);

        varbits.put(VarbitID.FARMING_TOOLS_RAKE, 1);
        varbits.put(VarbitID.FARMING_TOOLS_EXTRARAKES, 2);
        varbits.put(VarbitID.FARMING_TOOLS_ULTRACOMPOST, 88);
        varbits.put(
                VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_QUANTITY,
                999
        );

        varbits.put(VarbitID.PLANK_SACK_PLAIN, 10);
        varbits.put(VarbitID.PLANK_SACK_OAK, 20);
        varbits.put(VarbitID.PLANK_SACK_MAHOGANY, 30);

        varbits.put(VarbitID.FOSSIL_STORAGE_SMALL_UNID, 4);
        varbits.put(VarbitID.FOSSIL_STORAGE_RARE_UNID, 5);
        varbits.put(VarbitID.FOSSIL_STORAGE_SMALL_1, 6);
        varbits.put(VarbitID.FOSSIL_STORAGE_RARE_6, 7);

        varbits.put(VarbitID.HALLOWED_STORAGE_TOKEN, 11);
        varbits.put(VarbitID.HALLOWED_STORAGE_GRAPPLE, 12);
        varbits.put(VarbitID.HALLOWED_STORAGE_RING, 13);

        varbits.put(VarbitID.SCROLL_CASE_BEGINNER_MINOR, 14);
        varbits.put(VarbitID.SCROLL_CASE_BEGINNER_MAJOR, 15);
        varbits.put(VarbitID.SCROLL_CASE_MIMIC, 16);
        varps.put(VarPlayerID.SCROLL_CASE_TRACK, 17);

        varps.put(VarPlayerID.TOA_PICKAXE_STORAGE, 18);

        varps.put(VarPlayerID.BOOKOFSCROLLS1, -1);
        varps.put(VarPlayerID.BOOKOFSCROLLS2, 22);
        varps.put(VarPlayerID.BOOKOFSCROLLS7, 77);

        String json = PersistentStorageEvidence.collect(
                new PersistentStorageEvidence.ValueSource()
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
                "\"runePouch\":{\"slots\":[{\"typeRaw\":9,\"quantity\":2500}"
        ));
        assertTrue(json.contains(
                "{\"typeRaw\":13,\"quantity\":900}"
        ));

        assertTrue(json.contains(
                "\"small\":{\"amount\":3,\"typeRaw\":1}"
        ));
        assertTrue(json.contains(
                "\"medium\":{\"amount\":6,\"typeRaw\":1,\"degradationRaw\":100}"
        ));
        assertTrue(json.contains(
                "\"colossal\":{\"amount\":40,\"typeRaw\":1,\"degradationRaw\":400}"
        ));

        assertTrue(json.contains(
                "\"boltPouch\":{\"slots\":[{\"typeRaw\":7,\"quantityRaw\":125}"
        ));

        assertTrue(json.contains("\"rakeRaw\":1"));
        assertTrue(json.contains("\"extraRakesRaw\":2"));
        assertTrue(json.contains("\"ultracompostRaw\":88"));
        assertTrue(json.contains("\"bottomlessBucketQuantityRaw\":999"));

        assertTrue(json.contains("\"plain\":10"));
        assertTrue(json.contains("\"oak\":20"));
        assertTrue(json.contains("\"mahogany\":30"));

        assertTrue(json.contains("\"smallUnidentified\":4"));
        assertTrue(json.contains("\"rareUnidentified\":5"));
        assertTrue(json.contains("\"small\":[6,0,0,0,0]"));
        assertTrue(json.contains("\"rare\":[0,0,0,0,0,7]"));

        assertTrue(json.contains("\"tokensRaw\":11"));
        assertTrue(json.contains("\"grappleRaw\":12"));
        assertTrue(json.contains("\"ringRaw\":13"));

        assertTrue(json.contains(
                "\"beginner\":{\"minorRaw\":14,\"majorRaw\":15}"
        ));
        assertTrue(json.contains("\"mimicRaw\":16"));
        assertTrue(json.contains("\"trackRaw\":17"));

        assertTrue(json.contains("\"storedPickaxeRaw\":18"));

        assertTrue(json.contains(
                "\"packedWords\":[4294967295,22,0,0,0,0,77]"
        ));
    }

    @Test
    public void liveItemStateContainsOnlyProvenLiveSources()
    {
        Map<Integer, Integer> varbits = new HashMap<>();

        varbits.put(VarbitID.RUNE_POUCH_TYPE_1, 9);
        varbits.put(VarbitID.RUNE_POUCH_QUANTITY_1, 2500);
        varbits.put(VarbitID.SMALL_ESSENCE_POUCH, 3);
        varbits.put(VarbitID.SMALL_ESSENCE_POUCH_TYPE, 1);

        /*
         * Deliberately populate sources which are NOT yet proven safe for
         * arbitrary live snapshots. They must not leak into this dataset.
         */
        varbits.put(VarbitID.XBOWS_POUCH_SLOT1, 7);
        varbits.put(VarbitID.FARMING_TOOLS_RAKE, 1);
        varbits.put(VarbitID.PLANK_SACK_PLAIN, 10);
        varbits.put(VarbitID.FOSSIL_STORAGE_SMALL_UNID, 4);
        varbits.put(VarbitID.HALLOWED_STORAGE_TOKEN, 11);
        varbits.put(VarbitID.SCROLL_CASE_BEGINNER_MINOR, 14);

        String json = PersistentStorageEvidence.collectLiveItemState(
                new PersistentStorageEvidence.ValueSource()
                {
                    @Override
                    public int varbit(int id)
                    {
                        return varbits.getOrDefault(id, 0);
                    }

                    @Override
                    public int varp(int id)
                    {
                        return 0;
                    }
                }
        );

        assertTrue(json.contains("\"runePouch\":"));
        assertTrue(json.contains("\"essencePouches\":"));

        assertTrue(!json.contains("\"boltPouch\":"));
        assertTrue(!json.contains("\"toolLeprechaun\":"));
        assertTrue(!json.contains("\"plankSack\":"));
        assertTrue(!json.contains("\"fossilStorage\":"));
        assertTrue(!json.contains("\"hallowedStorage\":"));
        assertTrue(!json.contains("\"clueScrollCase\":"));
        assertTrue(!json.contains("\"toa\":"));
        assertTrue(!json.contains("\"masterScrollBook\":"));
    }
    @Test
    public void preservesRawZeroValuesWithoutInventingMeaning()
    {
        String json = PersistentStorageEvidence.collect(
                new PersistentStorageEvidence.ValueSource()
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

        assertTrue(json.contains(
                "\"runePouch\":{\"slots\":[{\"typeRaw\":0,\"quantity\":0}"
        ));
        assertTrue(json.contains(
                "\"small\":{\"amount\":0,\"typeRaw\":0}"
        ));
        assertTrue(json.contains("\"storedPickaxeRaw\":0"));
        assertTrue(json.contains(
                "\"packedWords\":[0,0,0,0,0,0,0]"
        ));
    }
}