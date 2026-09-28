package com.osrsoracle;

import com.google.gson.Gson;
import java.util.concurrent.CompletableFuture;

final class Slice5AckProcessor
{
    static final class Result
    {
        private final boolean validAck;
        private final boolean requestFollowUp;

        private Result(
                boolean validAck,
                boolean requestFollowUp
        )
        {
            this.validAck = validAck;
            this.requestFollowUp = requestFollowUp;
        }

        boolean isValidAck()
        {
            return validAck;
        }

        boolean shouldRequestFollowUp()
        {
            return requestFollowUp;
        }
    }

    private Slice5AckProcessor()
    {
    }

    static CompletableFuture<Result> apply(
            Gson gson,
            String account,
            String responseBody,
            Slice5PersistenceCoordinator coordinator,
            String acknowledgedAt
    )
    {
        final Slice5Ack ack;

        try
        {
            ack =
                    Slice5Ack.parse(
                            gson,
                            responseBody
                    );
        }
        catch (RuntimeException e)
        {
            return CompletableFuture.completedFuture(
                    new Result(
                            false,
                            false
                    )
            );
        }

        CompletableFuture<Void> chain =
                CompletableFuture.completedFuture(null);

        for (Slice5Ack.Conflict conflict : ack.getEventConflicts())
        {
            chain =
                    chain.thenCompose(
                            ignored ->
                                    coordinator.quarantineConflict(
                                            account,
                                            conflict.getEventId(),
                                            conflict.getCode(),
                                            acknowledgedAt
                                    ).thenApply(
                                            quarantineStatus ->
                                                    null
                                    )
                    );
        }

        if (!ack.getAcknowledgedEventIds().isEmpty())
        {
            chain =
                    chain.thenCompose(
                            ignored ->
                                    coordinator.acknowledgeEvents(
                                            account,
                                            ack.getAcknowledgedEventIds(),
                                            acknowledgedAt
                                    )
                    );
        }

        if (!ack.getAcknowledgedCoverageGapIds().isEmpty())
        {
            chain =
                    chain.thenCompose(
                            ignored ->
                                    coordinator.acknowledgeCoverageGaps(
                                            account,
                                            ack.getAcknowledgedCoverageGapIds()
                                    )
                    );
        }

        boolean requestFollowUp =
                !ack.getAcknowledgedEventIds().isEmpty() ||
                        !ack.getEventConflicts().isEmpty();

        return chain.thenApply(
                ignored ->
                        new Result(
                                true,
                                requestFollowUp
                        )
        );
    }
}