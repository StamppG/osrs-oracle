package com.osrsoracle;

import java.util.StringJoiner;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

final class PersistentStorageEvidence
{
    interface ValueSource
    {
        int varbit(int id);
        int varp(int id);
    }

    private PersistentStorageEvidence()
    {
    }

    static String collect(Client client)
    {
        return collect(new ValueSource()
        {
            @Override
            public int varbit(int id)
            {
                return client.getVarbitValue(id);
            }

            @Override
            public int varp(int id)
            {
                return client.getVarpValue(id);
            }
        });
    }

    static String collectLiveItemState(Client client)
    {
        return collectLiveItemState(new ValueSource()
        {
            @Override
            public int varbit(int id)
            {
                return client.getVarbitValue(id);
            }

            @Override
            public int varp(int id)
            {
                return client.getVarpValue(id);
            }
        });
    }

    static String collectLiveItemState(ValueSource source)
    {
        return "{" +
                "\"runePouch\":" + runePouch(source) +
                ",\"essencePouches\":" + essencePouches(source) +
                ",\"boltPouch\":" + boltPouch(source) +
                ",\"clueScrollCaseProgression\":" + clueScrollCase(source) +
                ",\"masterScrollBook\":" + masterScrollBook(source) +
                "}";
    }
    static String collect(ValueSource source)
    {
        return "{" +
                "\"runePouch\":" + runePouch(source) +
                ",\"essencePouches\":" + essencePouches(source) +
                ",\"boltPouch\":" + boltPouch(source) +
                ",\"toolLeprechaun\":" + toolLeprechaun(source) +
                ",\"plankSack\":" + plankSack(source) +
                ",\"fossilStorage\":" + fossilStorage(source) +
                ",\"hallowedStorage\":" + hallowedStorage(source) +
                ",\"clueScrollCase\":" + clueScrollCase(source) +
                ",\"toa\":{\"storedPickaxeRaw\":" +
                source.varp(VarPlayerID.TOA_PICKAXE_STORAGE) +
                "}" +
                ",\"masterScrollBook\":" + masterScrollBook(source) +
                "}";
    }

    private static String runePouch(ValueSource source)
    {
        int[] types = {
            VarbitID.RUNE_POUCH_TYPE_1,
            VarbitID.RUNE_POUCH_TYPE_2,
            VarbitID.RUNE_POUCH_TYPE_3,
            VarbitID.RUNE_POUCH_TYPE_4,
            VarbitID.RUNE_POUCH_TYPE_5,
            VarbitID.RUNE_POUCH_TYPE_6
        };

        int[] quantities = {
            VarbitID.RUNE_POUCH_QUANTITY_1,
            VarbitID.RUNE_POUCH_QUANTITY_2,
            VarbitID.RUNE_POUCH_QUANTITY_3,
            VarbitID.RUNE_POUCH_QUANTITY_4,
            VarbitID.RUNE_POUCH_QUANTITY_5,
            VarbitID.RUNE_POUCH_QUANTITY_6
        };

        StringJoiner slots = new StringJoiner(",", "[", "]");

        for (int i = 0; i < types.length; i++)
        {
            slots.add(
                    "{\"typeRaw\":" + source.varbit(types[i]) +
                    ",\"quantity\":" + source.varbit(quantities[i]) +
                    "}"
            );
        }

        return "{\"slots\":" + slots + "}";
    }

    private static String essencePouches(ValueSource source)
    {
        return "{" +
                "\"small\":{" +
                "\"amount\":" +
                source.varbit(VarbitID.SMALL_ESSENCE_POUCH) +
                ",\"typeRaw\":" +
                source.varbit(VarbitID.SMALL_ESSENCE_POUCH_TYPE) +
                "}" +
                ",\"medium\":{" +
                "\"amount\":" +
                source.varbit(VarbitID.MEDIUM_ESSENCE_POUCH) +
                ",\"typeRaw\":" +
                source.varbit(VarbitID.MEDIUM_ESSENCE_POUCH_TYPE) +
                ",\"degradationRaw\":" +
                source.varp(VarPlayerID.RCU_POUCH_DEGRADATION_MED) +
                "}" +
                ",\"large\":{" +
                "\"amount\":" +
                source.varbit(VarbitID.LARGE_ESSENCE_POUCH) +
                ",\"typeRaw\":" +
                source.varbit(VarbitID.LARGE_ESSENCE_POUCH_TYPE) +
                ",\"degradationRaw\":" +
                source.varp(VarPlayerID.RCU_POUCH_DEGRADATION_LARGE) +
                "}" +
                ",\"giant\":{" +
                "\"amount\":" +
                source.varbit(VarbitID.GIANT_ESSENCE_POUCH) +
                ",\"typeRaw\":" +
                source.varbit(VarbitID.GIANT_ESSENCE_POUCH_TYPE) +
                ",\"degradationRaw\":" +
                source.varp(VarPlayerID.RCU_POUCH_DEGRADATION_GIANT) +
                "}" +
                ",\"colossal\":{" +
                "\"amount\":" +
                source.varbit(VarbitID.COLOSSAL_ESSENCE_POUCH) +
                ",\"typeRaw\":" +
                source.varbit(VarbitID.COLOSSAL_ESSENCE_POUCH_TYPE) +
                ",\"degradationRaw\":" +
                source.varbit(VarbitID.RCU_POUCH_DEGRADATION_COLOSSAL) +
                "}" +
                "}";
    }

    private static String boltPouch(ValueSource source)
    {
        int[] quantities = {
            VarbitID.XBOWS_POUCH_NUM1,
            VarbitID.XBOWS_POUCH_NUM2,
            VarbitID.XBOWS_POUCH_NUM3,
            VarbitID.XBOWS_POUCH_NUM4
        };

        int[] types = {
            VarbitID.XBOWS_POUCH_SLOT1,
            VarbitID.XBOWS_POUCH_SLOT2,
            VarbitID.XBOWS_POUCH_SLOT3,
            VarbitID.XBOWS_POUCH_SLOT4
        };

        StringJoiner slots = new StringJoiner(",", "[", "]");

        for (int i = 0; i < quantities.length; i++)
        {
            slots.add(
                    "{\"typeRaw\":" + source.varbit(types[i]) +
                    ",\"quantityRaw\":" + source.varbit(quantities[i]) +
                    "}"
            );
        }

        return "{\"slots\":" + slots + "}";
    }

    private static String toolLeprechaun(ValueSource source)
    {
        return "{" +
                "\"rakeRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_RAKE) +
                ",\"extraRakesRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRARAKES) +
                ",\"seedDibberRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_DIBBER) +
                ",\"extraSeedDibbersRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRADIBBERS) +
                ",\"spadeRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_SPADE) +
                ",\"extraSpadesRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRASPADES) +
                ",\"secateursRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_SECATEURS) +
                ",\"extraSecateursRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRASECATEURS) +
                ",\"magicSecateursRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_FAIRYSECATEURS) +
                ",\"wateringCanRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_WATERINGCAN) +
                ",\"trowelRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_TROWEL) +
                ",\"extraTrowelsRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRATROWELS) +
                ",\"bucketsRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_BUCKETS) +
                ",\"extraBucketsRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRABUCKETS) +
                ",\"extraBuckets2Raw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRA2BUCKETS) +
                ",\"compostRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_COMPOST) +
                ",\"extraCompostRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRACOMPOST) +
                ",\"supercompostRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_SUPERCOMPOST) +
                ",\"extraSupercompostRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_EXTRASUPERCOMPOST) +
                ",\"ultracompostRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_ULTRACOMPOST) +
                ",\"plantCureRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_PLANTCURE) +
                ",\"bottomlessBucketTypeRaw\":" +
                source.varbit(VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_TYPE) +
                ",\"bottomlessBucketQuantityRaw\":" +
                source.varbit(
                        VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_QUANTITY
                ) +
                "}";
    }

    private static String plankSack(ValueSource source)
    {
        return "{" +
                "\"plain\":" +
                source.varbit(VarbitID.PLANK_SACK_PLAIN) +
                ",\"oak\":" +
                source.varbit(VarbitID.PLANK_SACK_OAK) +
                ",\"teak\":" +
                source.varbit(VarbitID.PLANK_SACK_TEAK) +
                ",\"mahogany\":" +
                source.varbit(VarbitID.PLANK_SACK_MAHOGANY) +
                ",\"camphor\":" +
                source.varbit(VarbitID.PLANK_SACK_CAMPHOR) +
                ",\"ironwood\":" +
                source.varbit(VarbitID.PLANK_SACK_IRONWOOD) +
                ",\"rosewood\":" +
                source.varbit(VarbitID.PLANK_SACK_ROSEWOOD) +
                "}";
    }

    private static String fossilStorage(ValueSource source)
    {
        StringJoiner values = new StringJoiner(",", "{", "}");

        add(values, "smallUnidentified", source.varbit(VarbitID.FOSSIL_STORAGE_SMALL_UNID));
        add(values, "mediumUnidentified", source.varbit(VarbitID.FOSSIL_STORAGE_MEDIUM_UNID));
        add(values, "largeUnidentified", source.varbit(VarbitID.FOSSIL_STORAGE_LARGE_UNID));
        add(values, "rareUnidentified", source.varbit(VarbitID.FOSSIL_STORAGE_RARE_UNID));

        int[] small = {
            VarbitID.FOSSIL_STORAGE_SMALL_1,
            VarbitID.FOSSIL_STORAGE_SMALL_2,
            VarbitID.FOSSIL_STORAGE_SMALL_3,
            VarbitID.FOSSIL_STORAGE_SMALL_4,
            VarbitID.FOSSIL_STORAGE_SMALL_5
        };

        int[] medium = {
            VarbitID.FOSSIL_STORAGE_MEDIUM_1,
            VarbitID.FOSSIL_STORAGE_MEDIUM_2,
            VarbitID.FOSSIL_STORAGE_MEDIUM_3,
            VarbitID.FOSSIL_STORAGE_MEDIUM_4,
            VarbitID.FOSSIL_STORAGE_MEDIUM_5
        };

        int[] large = {
            VarbitID.FOSSIL_STORAGE_LARGE_1,
            VarbitID.FOSSIL_STORAGE_LARGE_2,
            VarbitID.FOSSIL_STORAGE_LARGE_3,
            VarbitID.FOSSIL_STORAGE_LARGE_4,
            VarbitID.FOSSIL_STORAGE_LARGE_5
        };

        int[] plants = {
            VarbitID.FOSSIL_STORAGE_PLANT_1,
            VarbitID.FOSSIL_STORAGE_PLANT_2,
            VarbitID.FOSSIL_STORAGE_PLANT_3,
            VarbitID.FOSSIL_STORAGE_PLANT_4,
            VarbitID.FOSSIL_STORAGE_PLANT_5
        };

        int[] rare = {
            VarbitID.FOSSIL_STORAGE_RARE_1,
            VarbitID.FOSSIL_STORAGE_RARE_2,
            VarbitID.FOSSIL_STORAGE_RARE_3,
            VarbitID.FOSSIL_STORAGE_RARE_4,
            VarbitID.FOSSIL_STORAGE_RARE_5,
            VarbitID.FOSSIL_STORAGE_RARE_6
        };

        values.add("\"small\":" + varbitArray(source, small));
        values.add("\"medium\":" + varbitArray(source, medium));
        values.add("\"large\":" + varbitArray(source, large));
        values.add("\"plants\":" + varbitArray(source, plants));
        values.add("\"rare\":" + varbitArray(source, rare));

        return values.toString();
    }

    private static String hallowedStorage(ValueSource source)
    {
        return "{" +
                "\"tokensRaw\":" +
                source.varbit(VarbitID.HALLOWED_STORAGE_TOKEN) +
                ",\"grappleRaw\":" +
                source.varbit(VarbitID.HALLOWED_STORAGE_GRAPPLE) +
                ",\"focusRaw\":" +
                source.varbit(VarbitID.HALLOWED_STORAGE_FOCUS) +
                ",\"symbolRaw\":" +
                source.varbit(VarbitID.HALLOWED_STORAGE_SYMBOL) +
                ",\"hammerRaw\":" +
                source.varbit(VarbitID.HALLOWED_STORAGE_HAMMER) +
                ",\"ringRaw\":" +
                source.varbit(VarbitID.HALLOWED_STORAGE_RING) +
                "}";
    }

    private static String clueScrollCase(ValueSource source)
    {
        return "{" +
                "\"beginner\":{\"minorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_BEGINNER_MINOR) +
                ",\"majorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_BEGINNER_MAJOR) +
                "}" +
                ",\"easy\":{\"minorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_EASY_MINOR) +
                ",\"majorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_EASY_MAJOR) +
                "}" +
                ",\"medium\":{\"minorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_MEDIUM_MINOR) +
                ",\"majorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_MEDIUM_MAJOR) +
                "}" +
                ",\"hard\":{\"minorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_HARD_MINOR) +
                ",\"majorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_HARD_MAJOR) +
                "}" +
                ",\"elite\":{\"minorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_ELITE_MINOR) +
                ",\"majorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_ELITE_MAJOR) +
                "}" +
                ",\"master\":{\"minorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_MASTER_MINOR) +
                ",\"majorRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_MASTER_MAJOR) +
                "}" +
                ",\"mimicRaw\":" +
                source.varbit(VarbitID.SCROLL_CASE_MIMIC) +
                ",\"trackRaw\":" +
                source.varp(VarPlayerID.SCROLL_CASE_TRACK) +
                "}";
    }

    private static String masterScrollBook(ValueSource source)
    {
        int watson =
                source.varbit(VarbitID.BOOKOFSCROLLS_WATSON_HIGHBITS) * 256 +
                source.varbit(VarbitID.BOOKOFSCROLLS_WATSON_LOWBITS);

        return "{" +
                "\"nardah\":" + source.varbit(VarbitID.BOOKOFSCROLLS_NARDAH) +
                ",\"digsite\":" + source.varbit(VarbitID.BOOKOFSCROLLS_DIGSITE) +
                ",\"feldip\":" + source.varbit(VarbitID.BOOKOFSCROLLS_FELDIP) +
                ",\"lunarIsle\":" + source.varbit(VarbitID.BOOKOFSCROLLS_LUNARISLE) +
                ",\"mortton\":" + source.varbit(VarbitID.BOOKOFSCROLLS_MORTTON) +
                ",\"pestControl\":" + source.varbit(VarbitID.BOOKOFSCROLLS_PESTCONTROL) +
                ",\"piscatoris\":" + source.varbit(VarbitID.BOOKOFSCROLLS_PISCATORIS) +
                ",\"taiBwo\":" + source.varbit(VarbitID.BOOKOFSCROLLS_TAIBWO) +
                ",\"elf\":" + source.varbit(VarbitID.BOOKOFSCROLLS_ELF) +
                ",\"mosLes\":" + source.varbit(VarbitID.BOOKOFSCROLLS_MOSLES) +
                ",\"lumberyard\":" + source.varbit(VarbitID.BOOKOFSCROLLS_LUMBERYARD) +
                ",\"zulAndra\":" + source.varbit(VarbitID.BOOKOFSCROLLS_ZULANDRA) +
                ",\"cerberus\":" + source.varbit(VarbitID.BOOKOFSCROLLS_CERBERUS) +
                ",\"revenants\":" + source.varbit(VarbitID.BOOKOFSCROLLS_REVENANTS) +
                ",\"watson\":" + watson +
                ",\"guthixianTemple\":" + source.varbit(VarbitID.BOOKOFSCROLLS_GUTHIXIAN_TEMPLE) +
                ",\"spiderCave\":" + source.varbit(VarbitID.BOOKOFSCROLLS_SPIDERCAVE) +
                ",\"colossalWyrm\":" + source.varbit(VarbitID.BOOKOFSCROLLS_COLOSSAL_WYRM) +
                ",\"chasmOfFire\":" + source.varbit(VarbitID.BOOKOFSCROLLS_CHASMOFFIRE) +
                ",\"ardeaglais\":" + source.varbit(VarbitID.BOOKOFSCROLLS_ARDEAGLAIS) +
                "}";
    }

    private static String varbitArray(
            ValueSource source,
            int[] ids
    )
    {
        StringJoiner values = new StringJoiner(",", "[", "]");

        for (int id : ids)
        {
            values.add(Integer.toString(source.varbit(id)));
        }

        return values.toString();
    }

    private static void add(
            StringJoiner target,
            String name,
            int value
    )
    {
        target.add("\"" + name + "\":" + value);
    }
}