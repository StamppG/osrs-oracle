package com.osrsoracle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Slice5StateFileStoreTest
{
    @Rule
    public TemporaryFolder temporaryFolder =
            new TemporaryFolder();

    @Test
    public void missingFileLoadsEmptyState()
            throws Exception
    {
        Path stateFile =
                temporaryFolder
                        .getRoot()
                        .toPath()
                        .resolve("oracle")
                        .resolve("slice5-state.bin");

        Slice5StateFileStore store =
                new Slice5StateFileStore(
                        stateFile,
                        new Slice5StateCodec()
                );

        assertFalse(store.exists());

        Slice5OutboxState loaded =
                store.load();

        assertEquals(
                0,
                loaded.pendingCount()
        );

        assertEquals(
                0,
                loaded.quarantineCount()
        );
    }

    @Test
    public void saveThenNewStoreReloadsDurableState()
            throws Exception
    {
        Path stateFile =
                temporaryFolder
                        .getRoot()
                        .toPath()
                        .resolve("oracle")
                        .resolve("slice5-state.bin");

        Slice5StateFileStore firstStore =
                new Slice5StateFileStore(
                        stateFile,
                        new Slice5StateCodec()
                );

        Slice5OutboxState original =
                new Slice5OutboxState();

        String pendingId =
                "90000000-0000-4000-8000-000000000001";

        original.captureEvent(
                "Blue Helm",
                pendingId,
                "CHARACTER_DEATH",
                "2026-09-27T15:00:00Z",
                "{\"eventId\":\"durable-pending\"}"
        );

        original.suppressEvent(
                "Blue Helm",
                pendingId
        );

        original.ensureOpenGap(
                "Blue Helm",
                "2026-09-27T15:00:01Z",
                Slice5OutboxState.GapReason.OUTBOX_CORRUPTION
        );

        String quarantineId =
                "90000000-0000-4000-8000-000000000002";

        original.captureEvent(
                "Red Helm",
                quarantineId,
                "CHARACTER_DEATH",
                "2026-09-27T15:01:00Z",
                "{\"eventId\":\"durable-quarantine\"}"
        );

        original.quarantineConflict(
                "Red Helm",
                quarantineId,
                "IMMUTABLE_EVENT_CONFLICT",
                "2026-09-27T15:01:01Z"
        );

        firstStore.save(original);

        assertTrue(firstStore.exists());
        assertTrue(Files.size(stateFile) > 0);

        /*
         * Simulate restart:
         * discard both the in-memory state and the store instance.
         */
        Slice5StateFileStore restartedStore =
                new Slice5StateFileStore(
                        stateFile,
                        new Slice5StateCodec()
                );

        Slice5OutboxState restored =
                restartedStore.load();

        assertEquals(
                1,
                restored.pendingCount("Blue Helm")
        );

        assertTrue(
                restored.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        assertNotNull(
                restored.getOpenGap("Blue Helm")
        );

        assertEquals(
                Slice5OutboxState.GapReason.OUTBOX_CORRUPTION,
                restored.getOpenGap("Blue Helm")
                        .getReason()
        );

        assertEquals(
                1,
                restored.quarantineCount()
        );

        assertEquals(
                quarantineId,
                restored.quarantineSnapshot()
                        .get(0)
                        .getEventId()
        );
    }

    @Test
    public void laterSaveAtomicallyReplacesEarlierState()
            throws Exception
    {
        Path stateFile =
                temporaryFolder
                        .getRoot()
                        .toPath()
                        .resolve("oracle")
                        .resolve("slice5-state.bin");

        Slice5StateFileStore store =
                new Slice5StateFileStore(
                        stateFile,
                        new Slice5StateCodec()
                );

        Slice5OutboxState first =
                new Slice5OutboxState();

        first.captureEvent(
                "Blue Helm",
                "90000000-0000-4000-8000-000000000003",
                "CHARACTER_DEATH",
                "2026-09-27T15:02:00Z",
                "{\"eventId\":\"first\"}"
        );

        store.save(first);

        Slice5OutboxState second =
                first.copy();

        second.acknowledgeEvents(
                "Blue Helm",
                java.util.Collections.singletonList(
                        "90000000-0000-4000-8000-000000000003"
                ),
                "2026-09-27T15:02:01Z"
        );

        second.captureEvent(
                "Blue Helm",
                "90000000-0000-4000-8000-000000000004",
                "CHARACTER_DEATH",
                "2026-09-27T15:02:02Z",
                "{\"eventId\":\"second\"}"
        );

        store.save(second);

        Slice5OutboxState restored =
                store.load();

        assertEquals(
                1,
                restored.pendingCount("Blue Helm")
        );

        assertEquals(
                "90000000-0000-4000-8000-000000000004",
                restored.pendingSnapshot()
                        .get(0)
                        .getEventId()
        );

        assertFalse(
                Files.exists(
                        stateFile.getParent()
                                .resolve(
                                        "slice5-state.bin.tmp"
                                )
                )
        );
    }

    @Test
    public void corruptFileIsNotSilentlyTreatedAsEmpty()
            throws Exception
    {
        Path stateFile =
                temporaryFolder
                        .getRoot()
                        .toPath()
                        .resolve("oracle")
                        .resolve("slice5-state.bin");

        Files.createDirectories(
                stateFile.getParent()
        );

        Files.write(
                stateFile,
                new byte[] {
                        1, 2, 3, 4, 5
                }
        );

        Slice5StateFileStore store =
                new Slice5StateFileStore(
                        stateFile,
                        new Slice5StateCodec()
                );

        try
        {
            store.load();
            fail("Expected IOException");
        }
        catch (IOException expected)
        {
            assertTrue(
                    expected.getMessage()
                            .contains(
                                    "Slice 5 state"
                            )
            );
        }
    }
}