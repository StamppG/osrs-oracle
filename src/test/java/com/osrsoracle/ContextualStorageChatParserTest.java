package com.osrsoracle;

import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContextualStorageChatParserTest
{
    @Test
    public void parsesPopulatedHerbSackAcrossOneTick()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Herb sack");

        assertNull(parser.accept(
                ChatMessageType.GAMEMESSAGE,
                "You look in your herb sack and see:"
        ));

        assertNull(parser.accept(
                ChatMessageType.GAMEMESSAGE,
                "8 x Grimy avantoe"
        ));

        assertNull(parser.accept(
                ChatMessageType.GAMEMESSAGE,
                "9 x Grimy torstol"
        ));

        ContextualStorageChatParser.Observation observation =
                parser.finishTick();

        assertNotNull(observation);
        assertEquals(
                ContextualStorageChatParser.Dataset.HERB_SACK,
                observation.getDataset()
        );
        assertEquals(2, observation.getEntries().length);
        assertEquals("Grimy avantoe", observation.getEntries()[0].getName());
        assertEquals(8, observation.getEntries()[0].getQuantity());
        assertEquals("Grimy torstol", observation.getEntries()[1].getName());
        assertEquals(9, observation.getEntries()[1].getQuantity());
    }

    @Test
    public void parsesEmptyHerbSack()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Herb sack");
        parser.accept(
                ChatMessageType.GAMEMESSAGE,
                "You look in your herb sack and see:"
        );

        ContextualStorageChatParser.Observation observation =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "The herb sack is empty."
                );

        assertNotNull(observation);
        assertEquals(0, observation.getEntries().length);
    }

    @Test
    public void parsesClassicGemBagIncludingMissingSeparator()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Gem bag");

        ContextualStorageChatParser.Observation observation =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "Sapphires: 0 / Emeralds: 8 / Rubies: 0Diamonds: 3 / Dragonstones: 0"
                );

        assertNotNull(observation);
        assertEquals(
                ContextualStorageChatParser.Dataset.GEM_BAG,
                observation.getDataset()
        );
        assertEquals(2, observation.getEntries().length);
        assertEquals("Emeralds", observation.getEntries()[0].getName());
        assertEquals(8, observation.getEntries()[0].getQuantity());
        assertEquals("Diamonds", observation.getEntries()[1].getName());
        assertEquals(3, observation.getEntries()[1].getQuantity());
    }

    @Test
    public void parsesEmptyClassicGemBag()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Gem bag");

        ContextualStorageChatParser.Observation observation =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "Sapphires: 0 / Emeralds: 0 / Rubies: 0Diamonds: 0 / Dragonstones: 0"
                );

        assertNotNull(observation);
        assertEquals(0, observation.getEntries().length);
    }

    @Test
    public void parsesGemSatchelPopulatedAndEmpty()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Gem satchel");

        ContextualStorageChatParser.Observation populated =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "Opal: 3 / Jade: 2 / Red Topaz: 0"
                );

        assertNotNull(populated);
        assertEquals(2, populated.getEntries().length);
        assertEquals("Opal", populated.getEntries()[0].getName());
        assertEquals(3, populated.getEntries()[0].getQuantity());

        parser.armCheck("Gem satchel");

        ContextualStorageChatParser.Observation empty =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "Opal: 0 / Jade: 0 / Red Topaz: 0"
                );

        assertNotNull(empty);
        assertEquals(0, empty.getEntries().length);
    }

    @Test
    public void parsesCoalBagCountAndEmpty()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Coal bag");

        ContextualStorageChatParser.Observation populated =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "The coal bag contains 15 pieces of coal."
                );

        assertNotNull(populated);
        assertEquals(1, populated.getEntries().length);
        assertEquals(15, populated.getEntries()[0].getQuantity());

        parser.armCheck("Coal bag");

        ContextualStorageChatParser.Observation empty =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "The coal bag is now empty."
                );

        assertNotNull(empty);
        assertEquals(0, empty.getEntries().length);
    }

    @Test
    public void parsesFishBarrelPopulatedAndEmpty()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Fish barrel");

        ContextualStorageChatParser.Observation populated =
                parser.accept(
                        ChatMessageType.MESBOX,
                        "The barrel contains: 5\u00A0x\u00A0Raw anglerfish, 10\u00A0x\u00A0Raw lobster, 5\u00A0x\u00A0Raw sea turtle"
                );

        assertNotNull(populated);
        assertEquals(3, populated.getEntries().length);
        assertEquals("Raw lobster", populated.getEntries()[1].getName());
        assertEquals(10, populated.getEntries()[1].getQuantity());

        parser.armCheck("Fish barrel");

        ContextualStorageChatParser.Observation empty =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "Your barrel is empty."
                );

        assertNotNull(empty);
        assertEquals(0, empty.getEntries().length);
    }

    @Test
    public void parsesLiveFishBarrelMesboxEmpty()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Fish barrel");

        ContextualStorageChatParser.Observation empty =
                parser.accept(
                        ChatMessageType.MESBOX,
                        "The barrel is empty."
                );

        assertNotNull(empty);
        assertEquals(
                ContextualStorageChatParser.Dataset.FISH_BARREL,
                empty.getDataset()
        );
        assertEquals(0, empty.getEntries().length);
    }

    @Test
    public void parsesLogBasketPopulatedAndEmpty()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        parser.armCheck("Log basket");

        ContextualStorageChatParser.Observation populated =
                parser.accept(
                        ChatMessageType.MESBOX,
                        "The basket contains: 10\u00A0x\u00A0Yew logs, 5\u00A0x\u00A0Magic logs, 5\u00A0x\u00A0Teak logs, 5\u00A0x\u00A0Camphor logs"
                );

        assertNotNull(populated);
        assertEquals(4, populated.getEntries().length);
        assertEquals("Yew logs", populated.getEntries()[0].getName());
        assertEquals(10, populated.getEntries()[0].getQuantity());

        parser.armCheck("Log basket");

        ContextualStorageChatParser.Observation empty =
                parser.accept(
                        ChatMessageType.MESBOX,
                        "The basket is empty."
                );

        assertNotNull(empty);
        assertEquals(0, empty.getEntries().length);
    }

    @Test
    public void refusesUntestedVariantsAndUnarmedMessages()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();

        assertNull(parser.accept(
                ChatMessageType.GAMEMESSAGE,
                "Opal: 3 / Jade: 2 / Red Topaz: 0"
        ));

        String[] unsupported = {
            "Gem pouch",
            "Gem tote",
            "Gem sack",
            "Silklined herb sack",
            "Fish sack barrel",
            "Forestry basket"
        };

        for (String target : unsupported)
        {
            parser.armCheck(target);

            assertNull(parser.accept(
                    ChatMessageType.GAMEMESSAGE,
                    "Opal: 3 / Jade: 2 / Red Topaz: 0"
            ));

            assertNull(parser.finishTick());
        }
    }
    @Test
    public void retainsCheckContextAcrossTicks()
    {
        ContextualStorageChatParser parser =
                new ContextualStorageChatParser();
        parser.armCheck("Gem bag");
        assertNull(parser.finishTick());
        ContextualStorageChatParser.Observation observation =
                parser.accept(
                        ChatMessageType.GAMEMESSAGE,
                        "Sapphires: 0 / Emeralds: 8 / Rubies: 0Diamonds: 3 / Dragonstones: 0"
                );
        assertNotNull(observation);
        assertEquals(
                ContextualStorageChatParser.Dataset.GEM_BAG,
                observation.getDataset()
        );
    }
}
