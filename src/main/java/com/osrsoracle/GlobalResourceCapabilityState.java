package com.osrsoracle;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

final class GlobalResourceCapabilityState
{
    interface ValueSource
    {
        int varbit(int id);
        int varp(int id);
    }

    private GlobalResourceCapabilityState()
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

    static String collect(ValueSource source)
    {
        return "{\"resources\":" + resources(source) +
                ",\"capabilities\":" + capabilities(source) +
                "}";
    }

    private static String resources(ValueSource source)
    {
        return "{" +
                "\"slayer\":{" +
                "\"points\":" + source.varbit(VarbitID.SLAYER_POINTS) +
                ",\"taskStreak\":" +
                source.varbit(VarbitID.SLAYER_TASKS_COMPLETED) +
                "}" +
                ",\"nightmareZone\":{" +
                "\"rewardPoints\":" +
                source.varp(VarPlayerID.NZONE_REWARDPOINTS) +
                "}" +
                ",\"titheFarm\":{" +
                "\"points\":" +
                source.varbit(VarbitID.HOSIDIUS_TITHE_REWARDPOINTS) +
                "}" +
                ",\"soulWars\":{" +
                "\"zeal\":" +
                source.varp(VarPlayerID.SOUL_WARS_ZEAL) +
                "}" +
                ",\"giantsFoundry\":{" +
                "\"rewardPoints\":" +
                source.varp(
                        VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS
                ) +
                "}" +
                ",\"masteringMixology\":{" +
                "\"moxPoints\":" +
                source.varp(VarPlayerID.MIXOLOGY_MOX_POINTS) +
                ",\"agaPoints\":" +
                source.varp(VarPlayerID.MIXOLOGY_AGA_POINTS) +
                ",\"lyePoints\":" +
                source.varp(VarPlayerID.MIXOLOGY_LYE_POINTS) +
                "}" +
                ",\"camdozaal\":{" +
                "\"storedBarronite\":" +
                source.varp(VarPlayerID.CAMDOZAAL_STORED_BARRONITE) +
                "}" +
                ",\"scarEssenceMine\":{" +
                "\"coffer\":" +
                source.varp(VarPlayerID.SCAR_ESSENCEMINE_COFFER) +
                "}" +
                ",\"colosseum\":{" +
                "\"glory\":" +
                source.varp(VarPlayerID.COLOSSEUM_GLORY) +
                "}" +
                ",\"bountyHunter\":{" +
                "\"points\":" +
                source.varp(VarPlayerID.BH_2023_POINTS) +
                "}" +
                ",\"barbarianAssault\":" +
                barbarianAssaultPoints(source) +
                "}";
    }

    private static String capabilities(ValueSource source)
    {
        return "{" +
                "\"slayer\":" + slayerCapabilities(source) +
                ",\"prayers\":" + prayerCapabilities(source) +
                ",\"magic\":" + magicCapabilities(source) +
                ",\"farming\":{" +
                "\"autoWeedRaw\":" +
                source.varbit(VarbitID.FARMING_BLOCKWEEDS) +
                "}" +
                ",\"travel\":" + travelCapabilities(source) +
                ",\"guardiansOfTheRift\":{" +
                "\"needleUnlockedRaw\":" +
                source.varbit(VarbitID.GOTR_UNLOCKED_NEEDLE) +
                ",\"bagObtainedRaw\":" +
                source.varbit(VarbitID.GOTR_BAG_OBTAINED) +
                "}" +
                ",\"giantsFoundryMouldUnlocks\":" +
                giantsFoundryMouldUnlocks(source) +
                ",\"motherlodeMine\":{" +
                "\"biggerSackRaw\":" +
                source.varbit(VarbitID.MOTHERLODE_BIGGERSACK) +
                "}" +
                "}";
    }

    private static String slayerCapabilities(ValueSource source)
    {
        return "{" +
                "\"unlockWord0\":" +
                unsigned(source.varp(VarPlayerID.SLAYER_REWARDS_UNLOCKS)) +
                ",\"unlockWord1\":" +
                unsigned(source.varp(VarPlayerID.SLAYER_REWARDS_UNLOCKS1)) +
                ",\"unlockWord2\":" +
                unsigned(source.varp(VarPlayerID.SLAYER_REWARDS_UNLOCKS2)) +
                ",\"storageUnlockedRaw\":" +
                source.varbit(VarbitID.SLAYER_UNLOCK_STORAGE) +
                ",\"storedTaskRaw\":" +
                source.varp(VarPlayerID.SLAYER_STORED_VARP) +
                "}";
    }

    private static String prayerCapabilities(ValueSource source)
    {
        return "{" +
                "\"currentBookRaw\":" +
                source.varbit(VarbitID.PRAYERBOOK) +
                ",\"knightWavesStateRaw\":" +
                source.varbit(VarbitID.KR_KNIGHTWAVES_STATE) +
                ",\"rigourUnlockedRaw\":" +
                source.varbit(VarbitID.PRAYER_RIGOUR_UNLOCKED) +
                ",\"auguryUnlockedRaw\":" +
                source.varbit(VarbitID.PRAYER_AUGURY_UNLOCKED) +
                ",\"preserveUnlockedRaw\":" +
                source.varbit(VarbitID.PRAYER_PRESERVE_UNLOCKED) +
                ",\"deadeyeUnlockedRaw\":" +
                source.varbit(VarbitID.PRAYER_DEADEYE_UNLOCKED) +
                ",\"mysticVigourUnlockedRaw\":" +
                source.varbit(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) +
                "}";
    }

    private static String magicCapabilities(ValueSource source)
    {
        return "{" +
                "\"currentSpellbookRaw\":" +
                source.varbit(VarbitID.SPELLBOOK) +
                ",\"currentSpellbookSublistRaw\":" +
                source.varbit(VarbitID.SPELLBOOK_SUBLIST) +
                ",\"arceuusSpellbookUnlockedRaw\":" +
                source.varbit(VarbitID.ARCEUUS_SPELLBOOK_UNLOCKED) +
                ",\"bonesToPeachesUnlockedRaw\":" +
                source.varbit(VarbitID.MAGICTRAINING_BONESPEACHES) +
                "}";
    }

    private static String travelCapabilities(ValueSource source)
    {
        return "{" +
                "\"lovakengjMinecartsRaw\":" +
                unsigned(
                        source.varbit(
                                VarbitID.LOVAKENGJ_MINECARTS_STATUS
                        )
                ) +
                ",\"fairyRingPermissionRaw\":" +
                source.varbit(VarbitID.FAIRYRING_PERMISSION) +
                ",\"fairyRingCisUnlockedRaw\":" +
                source.varbit(VarbitID.ZEAH_FAIRYRING_CIS_UNLOCKED) +
                ",\"quetzalUnlockWordRaw\":" +
                unsigned(source.varp(VarPlayerID.QUETZALS_UNLOCKED)) +
                ",\"quetzalDestinations\":" +
                quetzalDestinations(source) +
                "}";
    }

    private static String quetzalDestinations(ValueSource source)
    {
        return "{" +
                "\"fortis\":" +
                source.varbit(VarbitID.QUETZAL_FORTIS) +
                ",\"teomat\":" +
                source.varbit(VarbitID.QUETZAL_TEOMAT) +
                ",\"sunsetCoast\":" +
                source.varbit(VarbitID.QUETZAL_SUNSETCOAST) +
                ",\"hunterGuild\":" +
                source.varbit(VarbitID.QUETZAL_HUNTERGUILD) +
                ",\"camTorum\":" +
                source.varbit(VarbitID.QUETZAL_CAMTORUM) +
                ",\"colossalWyrm\":" +
                source.varbit(VarbitID.QUETZAL_COLOSSALWYRM) +
                ",\"outerFortis\":" +
                source.varbit(VarbitID.QUETZAL_OUTERFORTIS) +
                ",\"colosseum\":" +
                source.varbit(VarbitID.QUETZAL_COLOSSEUM) +
                ",\"aldarin\":" +
                source.varbit(VarbitID.QUETZAL_ALDARIN) +
                ",\"quetzacalliGorge\":" +
                source.varbit(VarbitID.QUETZAL_QUETZACALLIGORGE) +
                ",\"salvagerOverlook\":" +
                source.varbit(VarbitID.QUETZAL_SALVAGEROVERLOOK) +
                ",\"talTeklan\":" +
                source.varbit(VarbitID.QUETZAL_TALTEKLAN) +
                ",\"auburnValley\":" +
                source.varbit(VarbitID.QUETZAL_AUBURNVALLEY) +
                ",\"kastori\":" +
                source.varbit(VarbitID.QUETZAL_KASTORI) +
                "}";
    }

    private static String barbarianAssaultPoints(ValueSource source)
    {
        return "{" +
                "\"attackerBase\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_ATTACKER_BASE) +
                ",\"attackerExtra\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_ATTACKER_EXTRA) +
                ",\"collectorBase\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_COLLECTOR_BASE) +
                ",\"collectorExtra\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_COLLECTOR_EXTRA) +
                ",\"healerBase\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_HEALER_BASE) +
                ",\"healerExtra\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_HEALER_EXTRA) +
                ",\"defenderBase\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_DEFENDER_BASE) +
                ",\"defenderExtra\":" +
                source.varbit(VarbitID.BARBASSAULT_POINTS_DEFENDER_EXTRA) +
                "}";
    }

    private static String giantsFoundryMouldUnlocks(ValueSource source)
    {
        return "{" +
                "\"ricasso7\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_7) +
                ",\"ricasso8\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_8) +
                ",\"ricasso9\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_9) +
                ",\"ricasso10\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_10) +
                ",\"ricasso11\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_11) +
                ",\"blade7\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_7) +
                ",\"blade8\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_8) +
                ",\"blade9\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_9) +
                ",\"blade10\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_10) +
                ",\"blade11\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_11) +
                ",\"tip7\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_7) +
                ",\"tip8\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_8) +
                ",\"tip9\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_9) +
                ",\"tip10\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_10) +
                ",\"tip11\":" +
                source.varbit(VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_11) +
                "}";
    }

    private static String unsigned(int value)
    {
        return Integer.toUnsignedString(value);
    }
}