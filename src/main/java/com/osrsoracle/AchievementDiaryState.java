package com.osrsoracle;

import java.util.StringJoiner;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

final class AchievementDiaryState
{
    private static final int COMPLETION_INFO_SCRIPT = 2200;

    private static final String[] TIER_NAMES = {
        "easy", "medium", "hard", "elite"
    };

    private static final String[] REGION_NAMES = {
        "ardougne",
        "desert",
        "falador",
        "fremennik",
        "kandarin",
        "karamja",
        "kourend",
        "lumbridge",
        "morytania",
        "varrock",
        "western",
        "wilderness"
    };

    private static final int[] REGION_SCRIPT_IDS = {
        1, 5, 2, 3, 4, 0, 11, 6, 7, 8, 10, 9
    };


    private static final int[][] PACKED_REGION_VARPS = {
        {VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2},
        {VarPlayerID.DESERT_ACHIEVEMENT_DIARY, VarPlayerID.DESERT_ACHIEVEMENT_DIARY2},
        {VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2},
        {VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2},
        {VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2},
        {VarPlayerID.ATJUN_TASKS_4},
        {VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2},
        {VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2},
        {VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2},
        {VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2},
        {VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2},
        {VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2}
    };

    private static final int[][] KARAMJA_TASK_VARBITS = {
        {
            VarbitID.ATJUN_EASY_BANANA,
            VarbitID.ATJUN_EASY_SWING,
            VarbitID.ATJUN_EASY_GOLD,
            VarbitID.ATJUN_EASY_BOAT_SARIM,
            VarbitID.ATJUN_EASY_BOAT_ARDY,
            VarbitID.ATJUN_EASY_CAIRN,
            VarbitID.ATJUN_EASY_FISHING,
            VarbitID.ATJUN_EASY_SEAWEED,
            VarbitID.ATJUN_EASY_TZHAAR,
            VarbitID.ATJUN_EASY_JOGRE
        },
        {
            VarbitID.ATJUN_MED_AGILITY,
            VarbitID.ATJUN_MED_VOLCANO,
            VarbitID.ATJUN_MED_CRANDOR,
            VarbitID.ATJUN_MED_CART,
            VarbitID.ATJUN_MED_CLEANUP,
            VarbitID.ATJUN_MED_SPIDER,
            VarbitID.ATJUN_MED_KHAZARD,
            VarbitID.ATJUN_MED_TEAK,
            VarbitID.ATJUN_MED_MAHOGANY,
            VarbitID.ATJUN_MED_KARAMBWAN,
            VarbitID.ATJUN_MED_MACHETTE,
            VarbitID.ATJUN_MED_GLIDER,
            VarbitID.ATJUN_MED_FARMING,
            VarbitID.ATJUN_MED_GRAAHK,
            VarbitID.ATJUN_MED_SHILO_VINES,
            VarbitID.ATJUN_MED_SHILO_LAVA,
            VarbitID.ATJUN_MED_SHILO_STAIRS,
            VarbitID.ATJUN_MED_CHARTER,
            VarbitID.ATJUN_MED_TOPAZ
        },
        {
            VarbitID.ATJUN_HARD_FIGHTPITS,
            VarbitID.ATJUN_HARD_FIGHTCAVE,
            VarbitID.ATJUN_HARD_OOMLIE,
            VarbitID.ATJUN_HARD_NATURE,
            VarbitID.ATJUN_HARD_KARAMBWAN,
            VarbitID.ATJUN_HARD_DEATHWING,
            VarbitID.ATJUN_HARD_XBOW,
            VarbitID.ATJUN_HARD_PALM,
            VarbitID.ATJUN_HARD_DURADEL,
            VarbitID.ATJUN_HARD_DRAGON
        }
    };

    private AchievementDiaryState()
    {
    }

    static String collect(Client client)
    {
        return "{\"packedCompletionWords\":" + packedCompletionWords(client) +
                ",\"karamjaTaskValues\":" + karamjaTaskValues(client) +
                ",\"kourendMultistage\":" +
                Integer.toUnsignedString(
                        client.getVarpValue(
                                VarPlayerID.KOUREND_ACHIEVEMENT_DIARY_MULTISTAGE
                        )
                ) +
                ",\"tierCounts\":" + tierCounts(client) +
                "}";
    }

    private static String packedCompletionWords(Client client)
    {
        StringJoiner regions = new StringJoiner(",", "{", "}");

        for (int region = 0; region < REGION_NAMES.length; region++)
        {
            StringJoiner words = new StringJoiner(",", "[", "]");

            for (int varp : PACKED_REGION_VARPS[region])
            {
                words.add(
                        Integer.toUnsignedString(
                                client.getVarpValue(varp)
                        )
                );
            }

            regions.add(
                    "\"" + REGION_NAMES[region] + "\":" + words
            );
        }

        return regions.toString();
    }

    private static String karamjaTaskValues(Client client)
    {
        StringJoiner tiers = new StringJoiner(",", "{", "}");

        for (int tier = 0; tier < KARAMJA_TASK_VARBITS.length; tier++)
        {
            StringJoiner values = new StringJoiner(",", "[", "]");

            for (int varbit : KARAMJA_TASK_VARBITS[tier])
            {
                values.add(
                        Integer.toString(
                                client.getVarbitValue(varbit)
                        )
                );
            }

            tiers.add(
                    "\"" + TIER_NAMES[tier] + "\":" + values
            );
        }

        return tiers.toString();
    }

    private static String tierCounts(Client client)
    {
        StringJoiner regions = new StringJoiner(",", "{", "}");

        for (int region = 0; region < REGION_NAMES.length; region++)
        {
            String regionJson = "null";

            try
            {
                client.runScript(
                        COMPLETION_INFO_SCRIPT,
                        REGION_SCRIPT_IDS[region]
                );

                int[] stack = client.getIntStack();

                if (
                        client.getIntStackSize() >= 12 &&
                                stack != null &&
                                stack.length >= 12
                )
                {
                    StringJoiner tiers = new StringJoiner(",", "{", "}");

                    for (int tier = 0; tier < TIER_NAMES.length; tier++)
                    {
                        int offset = tier * 3;

                        tiers.add(
                                "\"" + TIER_NAMES[tier] + "\":{" +
                                        "\"completed\":" +
                                        Math.max(0, stack[offset]) +
                                        ",\"total\":" +
                                        Math.max(0, stack[offset + 1]) +
                                        "}"
                        );
                    }

                    regionJson = tiers.toString();
                }
            }
            catch (RuntimeException ignored)
            {
                /*
                 * Counts are validation evidence. Failure must not block the
                 * raw task state or an otherwise valid Oracle snapshot.
                 */
            }

            regions.add(
                    "\"" + REGION_NAMES[region] + "\":" + regionJson
            );
        }

        return regions.toString();
    }
}
