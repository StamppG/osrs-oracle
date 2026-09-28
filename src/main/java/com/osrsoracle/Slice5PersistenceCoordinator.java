package com.osrsoracle;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

final class Slice5PersistenceCoordinator
{
    private final Slice5StateFileStore store;
    private final ExecutorService executor;

    private final AtomicBoolean started =
            new AtomicBoolean();

    private final AtomicBoolean stopped =
            new AtomicBoolean();

    private final CompletableFuture<Void> readyFuture =
            new CompletableFuture<>();

    /*
     * Published instances are never mutated again after assignment.
     * Mutations happen against a copy on the persistence executor, are
     * durably saved first, and only then replace this reference.
     */
    private volatile Slice5OutboxState publishedState =
            new Slice5OutboxState();

    private volatile boolean loadFinished = false;
    private volatile Throwable loadFailure = null;
    private volatile Throwable persistenceFailure = null;

    Slice5PersistenceCoordinator()
    {
        this(
                new Slice5StateFileStore(),
                Executors.newSingleThreadExecutor(
                        persistenceThreadFactory()
                )
        );
    }

    Slice5PersistenceCoordinator(
            Slice5StateFileStore store,
            ExecutorService executor
    )
    {
        if (store == null)
        {
            throw new IllegalArgumentException("store");
        }

        if (executor == null)
        {
            throw new IllegalArgumentException("executor");
        }

        this.store = store;
        this.executor = executor;
    }

    static final class SnapshotView
    {
        private final String eventsJson;
        private final String eventCoverageJson;
        private final String coverageGapsJson;

        SnapshotView(
                String eventsJson,
                String eventCoverageJson,
                String coverageGapsJson
        )
        {
            this.eventsJson = eventsJson;
            this.eventCoverageJson = eventCoverageJson;
            this.coverageGapsJson = coverageGapsJson;
        }

        String getEventsJson()
        {
            return eventsJson;
        }

        String getEventCoverageJson()
        {
            return eventCoverageJson;
        }

        String getCoverageGapsJson()
        {
            return coverageGapsJson;
        }
    }

    void start()
    {
        if (!started.compareAndSet(false, true))
        {
            return;
        }

        executor.execute(
                () ->
                {
                    try
                    {
                        Slice5OutboxState loaded =
                                store.load();

                        publishedState = loaded;
                        loadFinished = true;
                        readyFuture.complete(null);
                    }
                    catch (Throwable t)
                    {
                        /*
                         * Never silently replace an unreadable durable
                         * outbox with an empty one.
                         */
                        loadFailure = t;
                        loadFinished = true;
                        readyFuture.completeExceptionally(t);
                    }
                }
        );
    }

    void shutDown()
    {
        if (!stopped.compareAndSet(false, true))
        {
            return;
        }

        /*
         * RuneLite shutdown must not block waiting for disk work.
         */
        executor.shutdownNow();
    }

    boolean isReady()
    {
        return loadFinished &&
                loadFailure == null &&
                persistenceFailure == null &&
                !stopped.get();
    }

    boolean hasLoadFailure()
    {
        return loadFinished &&
                loadFailure != null;
    }

    Throwable getLoadFailure()
    {
        return loadFailure;
    }

    boolean hasPersistenceFailure()
    {
        return persistenceFailure != null;
    }

    Throwable getPersistenceFailure()
    {
        return persistenceFailure;
    }

    CompletableFuture<Void> readyFuture()
    {
        return readyFuture;
    }

    SnapshotView snapshotView(
            String account,
            String observedAt
    )
    {
        requireReadable();

        /*
         * Capture one immutable published-state reference so events,
         * current coverage, and transported gaps all describe the same
         * local persistence generation.
         */
        Slice5OutboxState state =
                publishedState;

        return new SnapshotView(
                state.oldestBatch(account).toJson(),
                state.eventCoverageJson(
                        account,
                        observedAt
                ),
                state.coverageGapsJson(account)
        );
    }

    Slice5OutboxState snapshotState()
    {
        requireReadable();
        return publishedState.copy();
    }

    Slice5OutboxState.Batch oldestBatch(
            String account
    )
    {
        requireReadable();

        return publishedState.oldestBatch(
                account
        );
    }

    String eventCoverageJson(
            String account,
            String observedAt
    )
    {
        requireReadable();

        return publishedState.eventCoverageJson(
                account,
                observedAt
        );
    }

    String coverageGapsJson(
            String account
    )
    {
        requireReadable();

        return publishedState.coverageGapsJson(
                account
        );
    }

    CompletableFuture<Slice5OutboxState.CaptureStatus>
    captureEvent(
            String account,
            String eventId,
            String eventType,
            String observedAt,
            String eventJson
    )
    {
        CompletableFuture<Slice5OutboxState.CaptureStatus> future =
                new CompletableFuture<>();

        submit(
                future,
                () ->
                {
                    Slice5OutboxState candidate =
                            publishedState.copy();

                    Slice5OutboxState.CaptureStatus result =
                            candidate.captureEvent(
                                    account,
                                    eventId,
                                    eventType,
                                    observedAt,
                                    eventJson
                            );

                    if (
                            result ==
                                    Slice5OutboxState.CaptureStatus
                                            .ALREADY_QUEUED
                    )
                    {
                        return result;
                    }

                    persistAndPublish(candidate);
                    return result;
                }
        );

        return future;
    }

    CompletableFuture<Void> acknowledgeEvents(
            String account,
            Collection<String> acknowledgedEventIds,
            String recoveredAt
    )
    {
        CompletableFuture<Void> future =
                new CompletableFuture<>();

        submit(
                future,
                () ->
                {
                    Slice5OutboxState candidate =
                            publishedState.copy();

                    candidate.acknowledgeEvents(
                            account,
                            acknowledgedEventIds,
                            recoveredAt
                    );

                    persistAndPublish(candidate);
                    return null;
                }
        );

        return future;
    }

    CompletableFuture<Void> acknowledgeCoverageGaps(
            String account,
            Collection<String> acknowledgedGapIds
    )
    {
        CompletableFuture<Void> future =
                new CompletableFuture<>();

        submit(
                future,
                () ->
                {
                    Slice5OutboxState candidate =
                            publishedState.copy();

                    candidate.acknowledgeCoverageGaps(
                            account,
                            acknowledgedGapIds
                    );

                    persistAndPublish(candidate);
                    return null;
                }
        );

        return future;
    }

    CompletableFuture<Slice5OutboxState.QuarantineStatus>
    quarantineConflict(
            String account,
            String eventId,
            String errorCode,
            String quarantinedAt
    )
    {
        CompletableFuture<Slice5OutboxState.QuarantineStatus> future =
                new CompletableFuture<>();

        submit(
                future,
                () ->
                {
                    Slice5OutboxState candidate =
                            publishedState.copy();

                    Slice5OutboxState.QuarantineStatus result =
                            candidate.quarantineConflict(
                                    account,
                                    eventId,
                                    errorCode,
                                    quarantinedAt
                            );

                    if (
                            result ==
                                    Slice5OutboxState.QuarantineStatus
                                            .ALREADY_QUARANTINED
                    )
                    {
                        return result;
                    }

                    persistAndPublish(candidate);
                    return result;
                }
        );

        return future;
    }

    private void persistAndPublish(
            Slice5OutboxState candidate
    )
            throws Exception
    {
        /*
         * Transactional publication:
         *
         * if save fails, the previously published in-memory state remains
         * authoritative. An ACK or conflict can therefore never disappear
         * from memory before its replacement state is durable.
         */
        try
        {
            store.save(candidate);
        }
        catch (Exception e)
        {
            persistenceFailure = e;
            throw e;
        }
        publishedState = candidate;
    }

    private <T> void submit(
            CompletableFuture<T> future,
            Work<T> work
    )
    {
        if (!started.get())
        {
            future.completeExceptionally(
                    new IllegalStateException(
                            "Slice 5 persistence not started"
                    )
            );
            return;
        }

        if (stopped.get())
        {
            future.completeExceptionally(
                    new IllegalStateException(
                            "Slice 5 persistence stopped"
                    )
            );
            return;
        }

        executor.execute(
                () ->
                {
                    if (loadFailure != null)
                    {
                        future.completeExceptionally(
                                new IllegalStateException(
                                        "Slice 5 persistent state failed to load",
                                        loadFailure
                                )
                        );
                        return;
                    }

                    if (persistenceFailure != null)
                    {
                        future.completeExceptionally(
                                new IllegalStateException(
                                        "Slice 5 persistent state is unavailable after save failure",
                                        persistenceFailure
                                )
                        );
                        return;
                    }

                    try
                    {
                        future.complete(
                                work.run()
                        );
                    }
                    catch (Throwable t)
                    {
                        future.completeExceptionally(t);
                    }
                }
        );
    }

    private void requireReadable()
    {
        if (!loadFinished)
        {
            throw new IllegalStateException(
                    "Slice 5 persistence is still loading"
            );
        }

        if (loadFailure != null)
        {
            throw new IllegalStateException(
                    "Slice 5 persistent state failed to load",
                    loadFailure
            );
        }

        if (persistenceFailure != null)
        {
            throw new IllegalStateException(
                    "Slice 5 persistent state is unavailable after save failure",
                    persistenceFailure
            );
        }

        if (stopped.get())
        {
            throw new IllegalStateException(
                    "Slice 5 persistence stopped"
            );
        }
    }

    private static ThreadFactory persistenceThreadFactory()
    {
        return runnable ->
        {
            Thread thread =
                    new Thread(
                            runnable,
                            "oracle-slice5-persistence"
                    );

            thread.setDaemon(true);
            return thread;
        };
    }

    private interface Work<T>
    {
        T run() throws Exception;
    }
}
