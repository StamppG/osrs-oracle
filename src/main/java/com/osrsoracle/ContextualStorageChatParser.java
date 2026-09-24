package com.osrsoracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;

final class ContextualStorageChatParser
{
    private static final int CHECK_CONTEXT_TICKS = 5;

    enum Dataset
    {
        HERB_SACK("herbSack"),
        GEM_BAG("gemBag"),
        GEM_SATCHEL("gemSatchel"),
        COAL_BAG("coalBag"),
        FISH_BARREL("fishBarrel"),
        LOG_BASKET("logBasket");

        private final String jsonField;

        Dataset(String jsonField)
        {
            this.jsonField = jsonField;
        }

        String getJsonField()
        {
            return jsonField;
        }
    }

    static final class Observation
    {
        private final Dataset dataset;
        private final ObservedContextualStorageState.Entry[] entries;

        Observation(
                Dataset dataset,
                ObservedContextualStorageState.Entry[] entries
        )
        {
            this.dataset = dataset;
            this.entries = entries.clone();
        }

        Dataset getDataset()
        {
            return dataset;
        }

        ObservedContextualStorageState.Entry[] getEntries()
        {
            return entries.clone();
        }
    }

    private static final Pattern ITEM_COUNT =
            Pattern.compile("^(\\d+)\\s*x\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern COAL_COUNT =
            Pattern.compile(
                    "^The coal bag (?:still )?contains (\\d+|one) pieces? of coal\\.$",
                    Pattern.CASE_INSENSITIVE
            );

    private Dataset armedDataset;
    private int ticksRemaining;
    private boolean herbCheckActive;
    private final List<ObservedContextualStorageState.Entry> pendingHerbs =
            new ArrayList<>();

    void armCheck(String menuTarget)
    {
        clearTransient();
        armedDataset = datasetForTarget(menuTarget);

        if (armedDataset != null)
        {
            ticksRemaining = CHECK_CONTEXT_TICKS;
        }
    }

    void reset()
    {
        clearTransient();
    }

    Observation accept(
            ChatMessageType type,
            String rawMessage
    )
    {
        if (
                armedDataset == null ||
                        type == null ||
                        rawMessage == null
        )
        {
            return null;
        }

        String message = normalize(rawMessage);

        switch (armedDataset)
        {
            case HERB_SACK:
                return acceptHerbSack(type, message);
            case GEM_BAG:
                return acceptGemBag(type, message);
            case GEM_SATCHEL:
                return acceptGemSatchel(type, message);
            case COAL_BAG:
                return acceptCoalBag(type, message);
            case FISH_BARREL:
                return acceptFishBarrel(type, message);
            case LOG_BASKET:
                return acceptLogBasket(type, message);
            default:
                return null;
        }
    }

    Observation finishTick()
    {
        if (
                armedDataset == Dataset.HERB_SACK &&
                        herbCheckActive &&
                        !pendingHerbs.isEmpty()
        )
        {
            Observation observation =
                    observation(
                            Dataset.HERB_SACK,
                            pendingHerbs
                    );

            clearTransient();
            return observation;
        }

        if (armedDataset != null && ticksRemaining > 0)
        {
            ticksRemaining--;

            if (ticksRemaining > 0)
            {
                return null;
            }
        }

        clearTransient();
        return null;
    }

    private Observation acceptHerbSack(
            ChatMessageType type,
            String message
    )
    {
        if (type != ChatMessageType.GAMEMESSAGE)
        {
            return null;
        }

        if (message.equalsIgnoreCase(
                "You look in your herb sack and see:"
        ))
        {
            herbCheckActive = true;
            pendingHerbs.clear();
            return null;
        }

        if (message.equalsIgnoreCase("The herb sack is empty."))
        {
            Observation observation =
                    empty(Dataset.HERB_SACK);

            clearTransient();
            return observation;
        }

        if (!herbCheckActive)
        {
            return null;
        }

        ObservedContextualStorageState.Entry entry =
                parseItemCount(message);

        if (entry != null)
        {
            pendingHerbs.add(entry);
        }

        return null;
    }

    private Observation acceptGemBag(
            ChatMessageType type,
            String message
    )
    {
        if (type != ChatMessageType.GAMEMESSAGE)
        {
            return null;
        }

        Observation observation =
                parseLabeledCounts(
                        Dataset.GEM_BAG,
                        message,
                        new String[] {
                            "Sapphires",
                            "Emeralds",
                            "Rubies",
                            "Diamonds",
                            "Dragonstones"
                        }
                );

        if (observation != null)
        {
            clearTransient();
        }

        return observation;
    }

    private Observation acceptGemSatchel(
            ChatMessageType type,
            String message
    )
    {
        if (type != ChatMessageType.GAMEMESSAGE)
        {
            return null;
        }

        Observation observation =
                parseLabeledCounts(
                        Dataset.GEM_SATCHEL,
                        message,
                        new String[] {
                            "Opal",
                            "Jade",
                            "Red Topaz"
                        }
                );

        if (observation != null)
        {
            clearTransient();
        }

        return observation;
    }

    private Observation acceptCoalBag(
            ChatMessageType type,
            String message
    )
    {
        if (type != ChatMessageType.GAMEMESSAGE)
        {
            return null;
        }

        if (
                message.equalsIgnoreCase("The coal bag is empty.") ||
                        message.equalsIgnoreCase("The coal bag is now empty.")
        )
        {
            Observation observation = empty(Dataset.COAL_BAG);
            clearTransient();
            return observation;
        }

        Matcher matcher = COAL_COUNT.matcher(message);

        if (!matcher.matches())
        {
            return null;
        }

        int quantity =
                matcher.group(1).equalsIgnoreCase("one")
                        ? 1
                        : Integer.parseInt(matcher.group(1));

        Observation observation =
                new Observation(
                        Dataset.COAL_BAG,
                        new ObservedContextualStorageState.Entry[] {
                            new ObservedContextualStorageState.Entry(
                                    "Coal",
                                    quantity
                            )
                        }
                );

        clearTransient();
        return observation;
    }

    private Observation acceptFishBarrel(
            ChatMessageType type,
            String message
    )
    {
        if (
                ((type == ChatMessageType.GAMEMESSAGE &&
                        message.equalsIgnoreCase("Your barrel is empty.")) ||
                        (type == ChatMessageType.MESBOX &&
                                message.equalsIgnoreCase("The barrel is empty.")))
        )
        {
            Observation observation = empty(Dataset.FISH_BARREL);
            clearTransient();
            return observation;
        }

        if (
                type != ChatMessageType.MESBOX ||
                        !message.regionMatches(
                                true,
                                0,
                                "The barrel contains:",
                                0,
                                "The barrel contains:".length()
                        )
        )
        {
            return null;
        }

        Observation observation =
                parseCommaSeparatedItems(
                        Dataset.FISH_BARREL,
                        message.substring(
                                "The barrel contains:".length()
                        )
                );

        if (observation != null)
        {
            clearTransient();
        }

        return observation;
    }

    private Observation acceptLogBasket(
            ChatMessageType type,
            String message
    )
    {
        if (
                type == ChatMessageType.MESBOX &&
                        message.equalsIgnoreCase("The basket is empty.")
        )
        {
            Observation observation = empty(Dataset.LOG_BASKET);
            clearTransient();
            return observation;
        }

        if (
                type != ChatMessageType.MESBOX ||
                        !message.regionMatches(
                                true,
                                0,
                                "The basket contains:",
                                0,
                                "The basket contains:".length()
                        )
        )
        {
            return null;
        }

        Observation observation =
                parseCommaSeparatedItems(
                        Dataset.LOG_BASKET,
                        message.substring(
                                "The basket contains:".length()
                        )
                );

        if (observation != null)
        {
            clearTransient();
        }

        return observation;
    }

    private static Observation parseLabeledCounts(
            Dataset dataset,
            String message,
            String[] labels
    )
    {
        List<ObservedContextualStorageState.Entry> entries =
                new ArrayList<>();

        for (String label : labels)
        {
            Matcher matcher =
                    Pattern.compile(
                            Pattern.quote(label) + "\\s*:\\s*(\\d+)",
                            Pattern.CASE_INSENSITIVE
                    ).matcher(message);

            if (!matcher.find())
            {
                return null;
            }

            int quantity = Integer.parseInt(matcher.group(1));

            if (quantity > 0)
            {
                entries.add(
                        new ObservedContextualStorageState.Entry(
                                label,
                                quantity
                        )
                );
            }
        }

        return observation(dataset, entries);
    }

    private static Observation parseCommaSeparatedItems(
            Dataset dataset,
            String body
    )
    {
        String normalized = normalize(body);

        if (normalized.isBlank())
        {
            return null;
        }

        List<ObservedContextualStorageState.Entry> entries =
                new ArrayList<>();

        for (String part : normalized.split("\\s*,\\s*"))
        {
            ObservedContextualStorageState.Entry entry =
                    parseItemCount(part);

            if (entry == null)
            {
                return null;
            }

            entries.add(entry);
        }

        return observation(dataset, entries);
    }

    private static ObservedContextualStorageState.Entry parseItemCount(
            String message
    )
    {
        Matcher matcher =
                ITEM_COUNT.matcher(normalize(message));

        if (!matcher.matches())
        {
            return null;
        }

        int quantity = Integer.parseInt(matcher.group(1));

        if (quantity <= 0)
        {
            return null;
        }

        return new ObservedContextualStorageState.Entry(
                matcher.group(2).trim(),
                quantity
        );
    }

    private static Observation observation(
            Dataset dataset,
            List<ObservedContextualStorageState.Entry> entries
    )
    {
        return new Observation(
                dataset,
                entries.toArray(
                        new ObservedContextualStorageState.Entry[0]
                )
        );
    }

    private static Observation empty(Dataset dataset)
    {
        return new Observation(
                dataset,
                new ObservedContextualStorageState.Entry[0]
        );
    }

    private static Dataset datasetForTarget(String rawTarget)
    {
        if (rawTarget == null)
        {
            return null;
        }

        String target =
                normalize(rawTarget).toLowerCase(Locale.ROOT);

        switch (target)
        {
            case "herb sack":
            case "open herb sack":
                return Dataset.HERB_SACK;
            case "gem bag":
            case "open gem bag":
                return Dataset.GEM_BAG;
            case "gem satchel":
            case "open gem satchel":
                return Dataset.GEM_SATCHEL;
            case "coal bag":
            case "open coal bag":
                return Dataset.COAL_BAG;
            case "fish barrel":
            case "open fish barrel":
                return Dataset.FISH_BARREL;
            case "log basket":
            case "open log basket":
                return Dataset.LOG_BASKET;
            default:
                return null;
        }
    }

    private void clearTransient()
    {
        armedDataset = null;
        ticksRemaining = 0;
        herbCheckActive = false;
        pendingHerbs.clear();
    }

    private static String normalize(String text)
    {
        return text
                .replace("\u00A0", " ")
                .replace("\u00D7", "x")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
