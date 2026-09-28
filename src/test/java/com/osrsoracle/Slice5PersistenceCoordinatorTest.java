package com.osrsoracle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Slice5PersistenceCoordinatorTest
{
    @Rule
    public TemporaryFolder temporaryFolder =
            new TemporaryFolder();

    @Test
    public void startupLoadsPreviouslyDurableState()
            throws Exception
    {
        Path stateFile =
                stateFile("load");

        Slice5StateFileStore store =
                store(stateFile);

        Slice5OutboxState durable =
                new Slice5OutboxState();

        durable.captureEvent(
                "Blue Helm",
                "a0000000-0000-4000-8000-000000000001",
                "CHARACTER_DEATH",
                "2026-09-27T16:00:00Z",
                "{\"eventId\":\"restart\"}"
        );

        store.save(durable);

        Slice5PersistenceCoordinator coordinator =
                coordinator(store);

        coordinator.start();

        coordinator.readyFuture()
                .get(5, TimeUnit.SECONDS);

        assertTrue(coordinator.isReady());

        assertEquals(
                1,
                coordinator.snapshotState()
                        .pendingCount("Blue Helm")
        );

        coordinator.shutDown();
    }

    @Test
    public void captureIsDurableBeforeItBecomesPublished()
            throws Exception
    {
        Path stateFile =
                stateFile("capture");

        Slice5StateFileStore store =
                store(stateFile);

        Slice5PersistenceCoordinator coordinator =
                coordinator(store);

        coordinator.start();

        coordinator.readyFuture()
                .get(5, TimeUnit.SECONDS);

        assertEquals(
                Slice5OutboxState.CaptureStatus.QUEUED,
                coordinator.captureEvent(
                        "Blue Helm",
                        "a0000000-0000-4000-8000-000000000002",
                        "CHARACTER_DEATH",
                        "2026-09-27T16:01:00Z",
                        "{\"eventId\":\"durable-before-publish\"}"
                ).get(5, TimeUnit.SECONDS)
        );

        assertEquals(
                1,
                coordinator.snapshotState()
                        .pendingCount("Blue Helm")
        );

        /*
         * Independent reload proves the published event is already durable.
         */
        Slice5OutboxState reloaded =
                store.load();

        assertEquals(
                1,
                reloaded.pendingCount("Blue Helm")
        );

        coordinator.shutDown();
    }

    @Test
    public void failedSaveDoesNotPublishCandidate()
            throws Exception
    {
        Path blocker =
                temporaryFolder
                        .getRoot()
                        .toPath()
                        .resolve("not-a-directory");

        Files.write(
                blocker,
                new byte[] { 1 }
        );

        Path impossibleStateFile =
                blocker.resolve(
                        "slice5-state.bin"
                );

        Slice5StateFileStore store =
                store(impossibleStateFile);

        Slice5PersistenceCoordinator coordinator =
                coordinator(store);

        /*
         * load() succeeds as empty because the state file itself does not
         * exist. save() then fails because its parent is a regular file.
         */
        coordinator.start();

        coordinator.readyFuture()
                .get(5, TimeUnit.SECONDS);

        try
        {
            coordinator.captureEvent(
                    "Blue Helm",
                    "a0000000-0000-4000-8000-000000000003",
                    "CHARACTER_DEATH",
                    "2026-09-27T16:02:00Z",
                    "{\"eventId\":\"must-not-publish\"}"
            ).get(5, TimeUnit.SECONDS);

            fail("Expected persistence failure");
        }
        catch (java.util.concurrent.ExecutionException expected)
        {
            assertTrue(
                    expected.getCause() != null
            );
        }

        assertTrue(
                coordinator.hasPersistenceFailure()
        );

        assertFalse(
                coordinator.isReady()
        );

        try
        {
            coordinator.snapshotState();
            fail("Expected fail-closed read rejection");
        }
        catch (IllegalStateException expected)
        {
            assertTrue(
                    expected.getCause() != null
            );
        }

        coordinator.shutDown();
    }

    @Test
    public void failedConflictQuarantineSavePausesCoordinatorAndPreservesDurablePending()
            throws Exception
    {
        Path stateFile =
                stateFile("conflict-save-failure");

        Slice5StateFileStore store =
                store(stateFile);

        Slice5PersistenceCoordinator coordinator =
                coordinator(store);

        coordinator.start();

        coordinator.readyFuture()
                .get(5, TimeUnit.SECONDS);

        String eventId =
                "a1000000-0000-4000-8000-000000000001";

        coordinator.captureEvent(
                "Blue Helm",
                eventId,
                "CHARACTER_DEATH",
                "2026-09-27T16:02:30Z",
                "{\"eventId\":\"conflict-before-save-failure\"}"
        ).get(5, TimeUnit.SECONDS);

        assertEquals(
                1,
                store.load().pendingCount("Blue Helm")
        );

        /*
         * Block only the atomic temporary file used by the next save.
         * The already-durable state file remains intact.
         */
        Path blockedTemporary =
                stateFile.resolveSibling(
                        "slice5-state.bin.tmp"
                );

        Files.createDirectory(
                blockedTemporary
        );

        try
        {
            coordinator.quarantineConflict(
                    "Blue Helm",
                    eventId,
                    "IMMUTABLE_EVENT_CONFLICT",
                    "2026-09-27T16:02:31Z"
            ).get(5, TimeUnit.SECONDS);

            fail("Expected quarantine persistence failure");
        }
        catch (java.util.concurrent.ExecutionException expected)
        {
            assertTrue(
                    expected.getCause() != null
            );
        }

        assertTrue(
                coordinator.hasPersistenceFailure()
        );

        assertFalse(
                coordinator.isReady()
        );

        /*
         * Read/send paths are now paused, so the known conflict cannot
         * automatically be batched and resent in this session.
         */
        try
        {
            coordinator.snapshotView(
                    "Blue Helm",
                    "2026-09-27T16:02:32Z"
            );

            fail("Expected fail-closed snapshot rejection");
        }
        catch (IllegalStateException expected)
        {
            assertTrue(
                    expected.getCause() != null
            );
        }

        /*
         * The failed candidate was never published or written.
         * Independent reload still contains the original durable event
         * and no undurable quarantine claim.
         */
        Slice5OutboxState durable =
                store.load();

        assertEquals(
                1,
                durable.pendingCount("Blue Helm")
        );

        assertEquals(
                0,
                durable.quarantineCount()
        );

        /*
         * Later mutations also remain rejected until restart.
         */
        try
        {
            coordinator.captureEvent(
                    "Blue Helm",
                    "a1000000-0000-4000-8000-000000000002",
                    "CHARACTER_DEATH",
                    "2026-09-27T16:02:33Z",
                    "{\"eventId\":\"must-not-publish\"}"
            ).get(5, TimeUnit.SECONDS);

            fail("Expected mutation rejection after save failure");
        }
        catch (java.util.concurrent.ExecutionException expected)
        {
            assertTrue(
                    expected.getCause()
                            instanceof IllegalStateException
            );
        }

        coordinator.shutDown();
    }

    @Test
    public void durableAckDoesNotResurrectAfterRestart()
            throws Exception
    {
        Path stateFile =
                stateFile("ack");

        Slice5StateFileStore store =
                store(stateFile);

        Slice5PersistenceCoordinator coordinator =
                coordinator(store);

        coordinator.start();

        coordinator.readyFuture()
                .get(5, TimeUnit.SECONDS);

        String eventId =
                "a0000000-0000-4000-8000-000000000004";

        coordinator.captureEvent(
                "Blue Helm",
                eventId,
                "CHARACTER_DEATH",
                "2026-09-27T16:03:00Z",
                "{\"eventId\":\"ack-me\"}"
        ).get(5, TimeUnit.SECONDS);

        coordinator.acknowledgeEvents(
                "Blue Helm",
                Collections.singletonList(
                        eventId
                ),
                "2026-09-27T16:03:01Z"
        ).get(5, TimeUnit.SECONDS);

        assertEquals(
                0,
                coordinator.snapshotState()
                        .pendingCount("Blue Helm")
        );

        coordinator.shutDown();

        Slice5OutboxState restarted =
                store.load();

        assertEquals(
                0,
                restarted.pendingCount("Blue Helm")
        );
    }

    @Test
    public void corruptStartupStateIsNeverSilentlyReplaced()
            throws Exception
    {
        Path stateFile =
                stateFile("corrupt");

        Files.createDirectories(
                stateFile.getParent()
        );

        Files.write(
                stateFile,
                new byte[] { 9, 8, 7, 6 }
        );

        Slice5PersistenceCoordinator coordinator =
                coordinator(
                        store(stateFile)
                );

        coordinator.start();

        try
        {
            coordinator.readyFuture()
                    .get(5, TimeUnit.SECONDS);

            fail("Expected startup load failure");
        }
        catch (java.util.concurrent.ExecutionException expected)
        {
            assertTrue(
                    coordinator.hasLoadFailure()
            );
        }

        assertFalse(
                coordinator.isReady()
        );

        try
        {
            coordinator.captureEvent(
                    "Blue Helm",
                    "a0000000-0000-4000-8000-000000000005",
                    "CHARACTER_DEATH",
                    "2026-09-27T16:04:00Z",
                    "{\"eventId\":\"do-not-overwrite-corrupt-state\"}"
            ).get(5, TimeUnit.SECONDS);

            fail("Expected mutation rejection");
        }
        catch (java.util.concurrent.ExecutionException expected)
        {
            assertTrue(
                    expected.getCause()
                            instanceof IllegalStateException
            );
        }

        /*
         * The original corrupt file remains present rather than being
         * silently replaced with an empty state.
         */
        assertTrue(
                Files.size(stateFile) == 4
        );

        coordinator.shutDown();
    }

    private Path stateFile(String name)
    {
        return temporaryFolder
                .getRoot()
                .toPath()
                .resolve(name)
                .resolve("oracle")
                .resolve("slice5-state.bin");
    }

    private static Slice5StateFileStore store(
            Path stateFile
    )
    {
        return new Slice5StateFileStore(
                stateFile,
                new Slice5StateCodec()
        );
    }

    private static Slice5PersistenceCoordinator coordinator(
            Slice5StateFileStore store
    )
    {
        return new Slice5PersistenceCoordinator(
                store,
                Executors.newSingleThreadExecutor(
                        runnable ->
                        {
                            Thread thread =
                                    new Thread(
                                            runnable,
                                            "slice5-test-persistence"
                                    );

                            thread.setDaemon(true);
                            return thread;
                        }
                )
        );
    }
}
