package com.osrsoracle;

import com.google.gson.Gson;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Slice5AckProcessorTest
{
    @Rule
    public TemporaryFolder temporaryFolder =
            new TemporaryFolder();

    private final Gson gson =
            new Gson();

    @Test
    public void malformedSuccessfulAckRetainsPendingEvidence()
            throws Exception
    {
        Path stateFile =
                stateFile("malformed");

        Slice5StateFileStore store =
                store(stateFile);

        Slice5PersistenceCoordinator coordinator =
                coordinator(store);

        coordinator.start();

        coordinator.readyFuture()
                .get(5, TimeUnit.SECONDS);

        String eventId =
                "a0000000-0000-4000-8000-000000000010";

        coordinator.captureEvent(
                "Blue Helm",
                eventId,
                "CHARACTER_DEATH",
                "2026-09-27T22:00:00Z",
                "{\"eventId\":\"must-survive-malformed-ack\"}"
        ).get(5, TimeUnit.SECONDS);

        Slice5AckProcessor.Result result =
                Slice5AckProcessor.apply(
                        gson,
                        "Blue Helm",
                        "{\"success\":true}",
                        coordinator,
                        "2026-09-27T22:00:01Z"
                ).get(5, TimeUnit.SECONDS);

        assertFalse(
                result.isValidAck()
        );

        assertFalse(
                result.shouldRequestFollowUp()
        );

        assertEquals(
                1,
                coordinator.snapshotState()
                        .pendingCount("Blue Helm")
        );

        coordinator.shutDown();

        Slice5OutboxState restarted =
                store.load();

        assertEquals(
                1,
                restarted.pendingCount("Blue Helm")
        );
    }

    @Test
    public void conflictQuarantinePersistsAcrossRestartAndNeverRebatches()
            throws Exception
    {
        Path stateFile =
                stateFile("conflict-restart");

        Slice5StateFileStore store =
                store(stateFile);

        Slice5PersistenceCoordinator coordinator =
                coordinator(store);

        coordinator.start();

        coordinator.readyFuture()
                .get(5, TimeUnit.SECONDS);

        String eventId =
                "a0000000-0000-4000-8000-000000000011";

        coordinator.captureEvent(
                "Blue Helm",
                eventId,
                "CHARACTER_DEATH",
                "2026-09-27T22:01:00Z",
                "{" +
                        "\"eventId\":\"" +
                        eventId +
                        "\"," +
                        "\"secretPayload\":\"must-not-survive-quarantine\"" +
                        "}"
        ).get(5, TimeUnit.SECONDS);

        String ackJson =
                "{" +
                        "\"success\":true," +
                        "\"characterRecordId\":\"11111111-2222-4333-8444-555555555555\"," +
                        "\"eventIngest\":{" +
                        "\"status\":\"PARTIAL_CONFLICT\"," +
                        "\"acknowledgedEventIds\":[]," +
                        "\"eventConflicts\":[{" +
                        "\"eventId\":\"" +
                        eventId +
                        "\"," +
                        "\"code\":\"IMMUTABLE_EVENT_CONFLICT\"" +
                        "}]," +
                        "\"acknowledgedCoverageGapIds\":[]" +
                        "}" +
                        "}";

        Slice5AckProcessor.Result result =
                Slice5AckProcessor.apply(
                        gson,
                        "Blue Helm",
                        ackJson,
                        coordinator,
                        "2026-09-27T22:01:01Z"
                ).get(5, TimeUnit.SECONDS);

        assertTrue(
                result.isValidAck()
        );

        assertTrue(
                result.shouldRequestFollowUp()
        );

        assertEquals(
                0,
                coordinator.snapshotState()
                        .pendingCount("Blue Helm")
        );

        assertEquals(
                1,
                coordinator.snapshotState()
                        .quarantineCount()
        );

        assertTrue(
                coordinator.snapshotState()
                        .oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        coordinator.shutDown();

        Slice5PersistenceCoordinator restarted =
                coordinator(store);

        restarted.start();

        restarted.readyFuture()
                .get(5, TimeUnit.SECONDS);

        Slice5OutboxState restartedState =
                restarted.snapshotState();

        assertEquals(
                0,
                restartedState.pendingCount("Blue Helm")
        );

        assertEquals(
                1,
                restartedState.quarantineCount()
        );

        assertEquals(
                eventId,
                restartedState.quarantineSnapshot()
                        .get(0)
                        .getEventId()
        );

        assertFalse(
                restartedState.quarantineSnapshot()
                        .get(0)
                        .toJson()
                        .contains(
                                "must-not-survive-quarantine"
                        )
        );

        assertTrue(
                restartedState.oldestBatch("Blue Helm")
                        .getEvents()
                        .isEmpty()
        );

        restarted.shutDown();
    }

    private Path stateFile(
            String name
    )
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
                                            "slice5-ack-test-persistence"
                                    );

                            thread.setDaemon(true);
                            return thread;
                        }
                )
        );
    }
}