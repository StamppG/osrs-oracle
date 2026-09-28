package com.osrsoracle;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Slice5FinalLivingStateCandidateTest
{
    @Test
    public void candidateContainsResidualFactsButNotDeathOwnedFields()
    {
        AccountTypeResolution resolution =
                AccountTypeResolution.resolve(
                        3,
                        12850,
                        "2026-09-27T19:00:00Z"
                );

        SnapshotLiveStateCollector.State liveState =
                new SnapshotLiveStateCollector.State(
                        "\"HARDCORE_IRONMAN\"",
                        resolution,
                        "true",
                        "27",
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        "[10,11]",
                        "{\"ardougne\":{\"easy\":true}}",
                        "Abyssal demons",
                        42,
                        "{\"attack\":{\"level\":99}}",
                        "{\"quests\":\"fixture\"}"
                );

        SnapshotEnvelopeSerializer.Parts parts =
                new SnapshotEnvelopeSerializer.Parts();

        /*
         * These canonical death-time / transport fields deliberately contain
         * sentinel values. None may appear in the candidate.
         */
        parts.account = "DO_NOT_COPY_ACCOUNT";
        parts.clientTime = "DO_NOT_COPY_CLIENT_TIME";
        parts.snapshotReason = "DO_NOT_COPY_REASON";
        parts.evidenceJson =
                "{\"sentinel\":\"DO_NOT_COPY_EVIDENCE\"}";
        parts.inventoryJson =
                "{\"sentinel\":\"DO_NOT_COPY_INVENTORY\"}";
        parts.equipmentJson =
                "{\"sentinel\":\"DO_NOT_COPY_EQUIPMENT\"}";
        parts.instantCollectionLogJson =
                "{\"sentinel\":\"DO_NOT_COPY_INSTANT_CLOG\"}";
        parts.eventsJson =
                "[{\"sentinel\":\"DO_NOT_COPY_EVENTS\"}]";
        parts.slice5EventCoverageJson =
                "{\"sentinel\":\"DO_NOT_COPY_EVENT_COVERAGE\"}";
        parts.slice5CoverageGapsJson =
                "[{\"sentinel\":\"DO_NOT_COPY_GAPS\"}]";

        parts.liveState = liveState;

        parts.diaryTaskStateJson =
                "{\"diaryTask\":1}";
        parts.globalResourceCapabilityStateJson =
                "{\"resource\":2}";
        parts.persistentStorageLiveItemStateJson =
                "{\"storage\":3}";
        parts.collectionLogJson =
                "{\"page\":4}";
        parts.bankJson =
                "[{\"bank\":5}]";
        parts.seedVaultJson =
                "[{\"seed\":6}]";
        parts.gimStorageJson =
                "{\"gim\":7}";
        parts.coxPrivateStorageJson =
                "{\"coxPrivate\":8}";
        parts.coxSharedStorageJson =
                "{\"coxShared\":9}";
        parts.gravestoneStorageJson =
                "{\"grave\":10}";
        parts.deathsOfficeStorageJson =
                "{\"deathsOffice\":11}";
        parts.potionStorageJson =
                "{\"potions\":12}";
        parts.motherlodeSackJson =
                "{\"mlm\":13}";
        parts.plankSackJson =
                "{\"planks\":14}";
        parts.herbSackJson =
                "{\"herbs\":15}";
        parts.gemBagJson =
                "{\"gems\":16}";
        parts.gemSatchelJson =
                "{\"satchel\":17}";
        parts.coalBagJson =
                "{\"coal\":18}";
        parts.fishBarrelJson =
                "{\"fish\":19}";
        parts.logBasketJson =
                "{\"logs\":20}";
        parts.lootingBagJson =
                "{\"looting\":21}";
        parts.seedBoxJson =
                "{\"seedBox\":22}";
        parts.tackleBoxJson =
                "{\"tackle\":23}";
        parts.forestryKitJson =
                "{\"forestry\":24}";
        parts.huntsmansKitJson =
                "{\"huntsman\":25}";
        parts.barbarianKnapsackJson =
                "{\"knapsack\":26}";
        parts.dizanasQuiverAmmoJson =
                "{\"quiver\":27}";
        parts.stashUnitsJson =
                "{\"stash\":28}";

        String coverage =
                "{" +
                        "\"liveState\":{" +
                        "\"status\":\"OBSERVED\"," +
                        "\"observedAt\":\"2026-09-27T19:00:00Z\"," +
                        "\"datasets\":[\"membership\",\"slayer\",\"skills\"]}," +
                        "\"bank\":{\"status\":\"OBSERVED\",\"observedAt\":\"2026-09-27T18:00:00Z\"}" +
                        "}";

        String json =
                Slice5FinalLivingStateCandidate.serialize(
                        parts,
                        coverage
                );

        assertTrue(
                json.startsWith(
                        "{\"schema\":2,\"projection\":{"
                )
        );

        assertTrue(
                json.contains(
                        "\"membershipActive\":true"
                )
        );

        assertTrue(
                json.contains(
                        "\"slayerTask\":\"Abyssal demons\""
                )
        );

        assertTrue(
                json.contains(
                        "\"bank\":[{\"bank\":5}]"
                )
        );

        assertTrue(
                json.contains(
                        "\"collectionLog\":{\"page\":4}"
                )
        );

        assertTrue(
                json.contains(
                        "\"coverage\":" + coverage
                )
        );

        assertFalse(json.contains("DO_NOT_COPY_ACCOUNT"));
        assertFalse(json.contains("DO_NOT_COPY_CLIENT_TIME"));
        assertFalse(json.contains("DO_NOT_COPY_REASON"));
        assertFalse(json.contains("DO_NOT_COPY_EVIDENCE"));
        assertFalse(json.contains("DO_NOT_COPY_INVENTORY"));
        assertFalse(json.contains("DO_NOT_COPY_EQUIPMENT"));
        assertFalse(json.contains("DO_NOT_COPY_INSTANT_CLOG"));
        assertFalse(json.contains("DO_NOT_COPY_EVENTS"));
        assertFalse(json.contains("DO_NOT_COPY_EVENT_COVERAGE"));
        assertFalse(json.contains("DO_NOT_COPY_GAPS"));

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
    }
}