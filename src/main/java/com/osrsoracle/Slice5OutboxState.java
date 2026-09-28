package com.osrsoracle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.UUID;

final class Slice5OutboxState
{
    static final int MAX_PENDING_EVENTS = 64;
    static final int MAX_PENDING_BYTES = 8 * 1024 * 1024;
    static final int MAX_SINGLE_EVENT_BYTES = 2 * 1024 * 1024;
    static final int MAX_EVENTS_PER_REQUEST = 16;
    static final int MAX_EVENT_BYTES_PER_REQUEST = 2 * 1024 * 1024;
    static final int MAX_QUARANTINE_RECORDS = 64;
    static final int MAX_QUARANTINE_BYTES = 256 * 1024;

    enum CaptureStatus
    {
        QUEUED,
        ALREADY_QUEUED,
        EVENT_TOO_LARGE,
        OUTBOX_CAPACITY
    }

    enum QuarantineStatus
    {
        QUARANTINED,
        ALREADY_QUARANTINED,
        QUARANTINE_CAPACITY
    }

    enum GapReason
    {
        OUTBOX_CAPACITY,
        EVENT_TOO_LARGE,
        OUTBOX_CORRUPTION,
        QUARANTINE_CAPACITY
    }

    static final class PendingEvent
    {
        private final String account;
        private final String eventId;
        private final String eventType;
        private final String observedAt;
        private final String eventJson;
        private final int serializedBytes;
        private boolean suppressed;

        PendingEvent(
                String account,
                String eventId,
                String eventType,
                String observedAt,
                String eventJson,
                boolean suppressed
        )
        {
            this.account = normalizeAccount(account);
            this.eventId = requireText(eventId, "eventId");
            this.eventType = requireText(eventType, "eventType");
            this.observedAt = requireText(observedAt, "observedAt");
            this.eventJson = requireText(eventJson, "eventJson");
            this.serializedBytes = utf8Length(eventJson);
            this.suppressed = suppressed;
        }

        String getAccount()
        {
            return account;
        }

        String getEventId()
        {
            return eventId;
        }

        String getEventType()
        {
            return eventType;
        }

        String getObservedAt()
        {
            return observedAt;
        }

        String getEventJson()
        {
            return eventJson;
        }

        int getSerializedBytes()
        {
            return serializedBytes;
        }

        boolean isSuppressed()
        {
            return suppressed;
        }

        void suppress()
        {
            suppressed = true;
        }
    }

    static final class CoverageGap
    {
        private final String account;
        private final String gapId;
        private final String startedAt;
        private final GapReason reason;
        private String endedAt;

        CoverageGap(
                String account,
                String gapId,
                String startedAt,
                String endedAt,
                GapReason reason
        )
        {
            this.account = normalizeAccount(account);
            this.gapId = requireText(gapId, "gapId");
            this.startedAt = requireText(startedAt, "startedAt");

            if (reason == null)
            {
                throw new IllegalArgumentException("reason");
            }

            this.reason = reason;
            this.endedAt = endedAt;
        }

        String getAccount()
        {
            return account;
        }

        String getGapId()
        {
            return gapId;
        }

        String getStartedAt()
        {
            return startedAt;
        }

        String getEndedAt()
        {
            return endedAt;
        }

        GapReason getReason()
        {
            return reason;
        }

        boolean isOpen()
        {
            return endedAt == null;
        }

        void close(String closedAt)
        {
            if (!isOpen())
            {
                return;
            }

            endedAt = requireText(closedAt, "closedAt");
        }

        String toJson()
        {
            return "{\"gapId\":" + jsonString(gapId) +
                    ",\"startedAt\":" + jsonString(startedAt) +
                    ",\"endedAt\":" + jsonString(endedAt) +
                    ",\"reason\":" + jsonString(reason.name()) +
                    "}";
        }
    }

    static final class QuarantineRecord
    {
        private final String account;
        private final String eventId;
        private final String eventType;
        private final String errorCode;
        private final String firstObservedAt;
        private final String quarantinedAt;
        private final int serializedBytes;

        QuarantineRecord(
                String account,
                String eventId,
                String eventType,
                String errorCode,
                String firstObservedAt,
                String quarantinedAt
        )
        {
            this.account = normalizeAccount(account);
            this.eventId = requireText(eventId, "eventId");
            this.eventType = requireText(eventType, "eventType");
            this.errorCode = requireText(errorCode, "errorCode");
            this.firstObservedAt =
                    requireText(
                            firstObservedAt,
                            "firstObservedAt"
                    );
            this.quarantinedAt =
                    requireText(
                            quarantinedAt,
                            "quarantinedAt"
                    );

            this.serializedBytes =
                    utf8Length(toJson());
        }

        String getAccount()
        {
            return account;
        }

        String getEventId()
        {
            return eventId;
        }

        String getEventType()
        {
            return eventType;
        }

        String getErrorCode()
        {
            return errorCode;
        }

        String getFirstObservedAt()
        {
            return firstObservedAt;
        }

        String getQuarantinedAt()
        {
            return quarantinedAt;
        }

        int getSerializedBytes()
        {
            return serializedBytes;
        }

        String toJson()
        {
            return "{\"eventId\":" + jsonString(eventId) +
                    ",\"eventType\":" + jsonString(eventType) +
                    ",\"errorCode\":" + jsonString(errorCode) +
                    ",\"firstObservedAt\":" +
                    jsonString(firstObservedAt) +
                    ",\"quarantinedAt\":" +
                    jsonString(quarantinedAt) +
                    "}";
        }
    }

    static final class Batch
    {
        private final List<PendingEvent> events;

        Batch(List<PendingEvent> events)
        {
            this.events =
                    Collections.unmodifiableList(
                            new ArrayList<>(events)
                    );
        }

        List<PendingEvent> getEvents()
        {
            return events;
        }

        List<String> getEventIds()
        {
            List<String> ids =
                    new ArrayList<>();

            for (PendingEvent event : events)
            {
                ids.add(event.getEventId());
            }

            return ids;
        }

        String toJson()
        {
            StringJoiner json =
                    new StringJoiner(",", "[", "]");

            for (PendingEvent event : events)
            {
                json.add(event.getEventJson());
            }

            return json.toString();
        }

        int getSerializedEventBytes()
        {
            int total = 0;

            for (PendingEvent event : events)
            {
                total += event.getSerializedBytes();
            }

            return total;
        }
    }

    private final List<PendingEvent> pending =
            new ArrayList<>();

    private final List<CoverageGap> coverageGaps =
            new ArrayList<>();

    private final List<QuarantineRecord> quarantine =
            new ArrayList<>();

    CaptureStatus captureEvent(
            String account,
            String eventId,
            String eventType,
            String observedAt,
            String eventJson
    )
    {
        String normalized =
                normalizeAccount(account);

        if (
                findQuarantine(
                        normalized,
                        eventId
                ) != null
        )
        {
            return CaptureStatus.ALREADY_QUEUED;
        }

        PendingEvent existing =
                findPending(normalized, eventId);

        if (existing != null)
        {
            if (!existing.getEventJson().equals(eventJson))
            {
                throw new IllegalStateException(
                        "eventId already queued with different immutable content"
                );
            }

            return CaptureStatus.ALREADY_QUEUED;
        }

        int eventBytes =
                utf8Length(eventJson);

        if (eventBytes > MAX_SINGLE_EVENT_BYTES)
        {
            ensureOpenGap(
                    normalized,
                    observedAt,
                    GapReason.EVENT_TOO_LARGE
            );

            return CaptureStatus.EVENT_TOO_LARGE;
        }

        if (
                pending.size() >= MAX_PENDING_EVENTS ||
                        totalPendingBytes() + eventBytes >
                                MAX_PENDING_BYTES
        )
        {
            ensureOpenGap(
                    normalized,
                    observedAt,
                    GapReason.OUTBOX_CAPACITY
            );

            return CaptureStatus.OUTBOX_CAPACITY;
        }

        pending.add(
                new PendingEvent(
                        normalized,
                        eventId,
                        eventType,
                        observedAt,
                        eventJson,
                        false
                )
        );

        CoverageGap openGap =
                getOpenGap(normalized);

        if (
                openGap != null &&
                        openGap.getReason() ==
                                GapReason.EVENT_TOO_LARGE
        )
        {
            openGap.close(observedAt);
        }

        if (hasOutboxCapacity())
        {
            closeRecoveredOutboxCapacityGaps(
                    observedAt
            );
        }

        return CaptureStatus.QUEUED;
    }

    Batch oldestBatch(String account)
    {
        String normalized =
                normalizeAccount(account);

        List<PendingEvent> selected =
                new ArrayList<>();

        int bytes = 0;

        for (PendingEvent event : pending)
        {
            if (
                    !event.getAccount().equals(normalized) ||
                            event.isSuppressed()
            )
            {
                continue;
            }

            if (
                    selected.size() >=
                            MAX_EVENTS_PER_REQUEST
            )
            {
                break;
            }

            if (
                    bytes + event.getSerializedBytes() >
                            MAX_EVENT_BYTES_PER_REQUEST
            )
            {
                break;
            }

            selected.add(event);
            bytes += event.getSerializedBytes();
        }

        return new Batch(selected);
    }

    void acknowledgeEvents(
            String account,
            Collection<String> acknowledgedEventIds,
            String recoveredAt
    )
    {
        String normalized =
                normalizeAccount(account);

        if (
                acknowledgedEventIds == null ||
                        acknowledgedEventIds.isEmpty()
        )
        {
            return;
        }

        Iterator<PendingEvent> iterator =
                pending.iterator();

        boolean removed = false;

        while (iterator.hasNext())
        {
            PendingEvent event =
                    iterator.next();

            if (
                    event.getAccount().equals(normalized) &&
                            acknowledgedEventIds.contains(
                                    event.getEventId()
                            )
            )
            {
                iterator.remove();
                removed = true;
            }
        }

        if (!removed)
        {
            return;
        }

        if (hasOutboxCapacity())
        {
            closeRecoveredOutboxCapacityGaps(
                    recoveredAt
            );
        }
    }

    void acknowledgeCoverageGaps(
            String account,
            Collection<String> acknowledgedGapIds
    )
    {
        String normalized =
                normalizeAccount(account);

        if (
                acknowledgedGapIds == null ||
                        acknowledgedGapIds.isEmpty()
        )
        {
            return;
        }

        coverageGaps.removeIf(
                gap ->
                        gap.getAccount().equals(normalized) &&
                                !gap.isOpen() &&
                                acknowledgedGapIds.contains(
                                        gap.getGapId()
                                )
        );
    }

    void suppressEvent(
            String account,
            String eventId
    )
    {
        PendingEvent event =
                findPending(
                        normalizeAccount(account),
                        eventId
                );

        if (event != null)
        {
            event.suppress();
        }
    }

    QuarantineStatus quarantineConflict(
            String account,
            String eventId,
            String errorCode,
            String quarantinedAt
    )
    {
        String normalized =
                normalizeAccount(account);

        QuarantineRecord existing =
                findQuarantine(
                        normalized,
                        eventId
                );

        if (existing != null)
        {
            return QuarantineStatus.ALREADY_QUARANTINED;
        }

        PendingEvent pendingEvent =
                findPending(
                        normalized,
                        eventId
                );

        if (pendingEvent == null)
        {
            throw new IllegalArgumentException(
                    "pending event not found"
            );
        }

        QuarantineRecord record =
                new QuarantineRecord(
                        normalized,
                        pendingEvent.getEventId(),
                        pendingEvent.getEventType(),
                        errorCode,
                        pendingEvent.getObservedAt(),
                        quarantinedAt
                );

        if (
                quarantine.size() >=
                        MAX_QUARANTINE_RECORDS ||
                        totalQuarantineBytes() +
                                record.getSerializedBytes() >
                                MAX_QUARANTINE_BYTES
        )
        {
            /*
             * Do not silently discard the conflicting immutable payload.
             * Keep it locally, but permanently suppress it from automatic
             * resend until an operator/recovery path resolves the condition.
             */
            pendingEvent.suppress();

            ensureOpenGap(
                    normalized,
                    quarantinedAt,
                    GapReason.QUARANTINE_CAPACITY
            );

            return QuarantineStatus.QUARANTINE_CAPACITY;
        }

        /*
         * Ordering is intentional:
         *
         * quarantine metadata becomes represented first; only then may the
         * full conflicting pending payload leave the active outbox.
         *
         * The persistence layer must preserve this ordering durably.
         */
        quarantine.add(record);

        removePending(
                normalized,
                eventId
        );

        CoverageGap openGap =
                getOpenGap(normalized);

        if (
                openGap != null &&
                        openGap.getReason() ==
                                GapReason.QUARANTINE_CAPACITY
        )
        {
            openGap.close(quarantinedAt);
        }

        if (hasOutboxCapacity())
        {
            closeRecoveredOutboxCapacityGaps(
                    quarantinedAt
            );
        }

        return QuarantineStatus.QUARANTINED;
    }

    CoverageGap ensureOpenGap(
            String account,
            String startedAt,
            GapReason reason
    )
    {
        String normalized =
                normalizeAccount(account);

        CoverageGap existing =
                getOpenGap(normalized);

        if (existing != null)
        {
            return existing;
        }

        CoverageGap gap =
                new CoverageGap(
                        normalized,
                        UUID.randomUUID().toString(),
                        startedAt,
                        null,
                        reason
                );

        coverageGaps.add(gap);
        return gap;
    }

    CoverageGap getOpenGap(String account)
    {
        String normalized =
                normalizeAccount(account);

        for (CoverageGap gap : coverageGaps)
        {
            if (
                    gap.getAccount().equals(normalized) &&
                            gap.isOpen()
            )
            {
                return gap;
            }
        }

        return null;
    }

    String eventCoverageJson(
            String account,
            String observedAt
    )
    {
        CoverageGap gap =
                getOpenGap(account);

        if (gap == null)
        {
            return "{\"status\":\"COMPLETE\",\"observedAt\":" +
                    jsonString(observedAt) +
                    ",\"gap\":null}";
        }

        return "{\"status\":\"DEGRADED\",\"observedAt\":" +
                jsonString(observedAt) +
                ",\"gap\":" +
                gap.toJson() +
                "}";
    }

    String coverageGapsJson(String account)
    {
        String normalized =
                normalizeAccount(account);

        StringJoiner json =
                new StringJoiner(",", "[", "]");

        int count = 0;

        for (CoverageGap gap : coverageGaps)
        {
            if (!gap.getAccount().equals(normalized))
            {
                continue;
            }

            if (count >= 64)
            {
                break;
            }

            json.add(gap.toJson());
            count++;
        }

        return json.toString();
    }

    int quarantineCount()
    {
        return quarantine.size();
    }

    int totalQuarantineBytes()
    {
        int total = 0;

        for (QuarantineRecord record : quarantine)
        {
            total += record.getSerializedBytes();
        }

        return total;
    }

    List<QuarantineRecord> quarantineSnapshot()
    {
        return new ArrayList<>(quarantine);
    }

    int pendingCount()
    {
        return pending.size();
    }

    int pendingCount(String account)
    {
        String normalized =
                normalizeAccount(account);

        int count = 0;

        for (PendingEvent event : pending)
        {
            if (event.getAccount().equals(normalized))
            {
                count++;
            }
        }

        return count;
    }

    int totalPendingBytes()
    {
        int total = 0;

        for (PendingEvent event : pending)
        {
            total += event.getSerializedBytes();
        }

        return total;
    }

    int coverageGapCount(String account)
    {
        String normalized =
                normalizeAccount(account);

        int count = 0;

        for (CoverageGap gap : coverageGaps)
        {
            if (gap.getAccount().equals(normalized))
            {
                count++;
            }
        }

        return count;
    }

    List<PendingEvent> pendingSnapshot()
    {
        return new ArrayList<>(pending);
    }

    List<CoverageGap> coverageGapSnapshot()
    {
        return new ArrayList<>(coverageGaps);
    }

    Slice5OutboxState copy()
    {
        Slice5OutboxState copy =
                new Slice5OutboxState();

        for (PendingEvent event : pending)
        {
            copy.restorePending(
                    new PendingEvent(
                            event.getAccount(),
                            event.getEventId(),
                            event.getEventType(),
                            event.getObservedAt(),
                            event.getEventJson(),
                            event.isSuppressed()
                    )
            );
        }

        for (CoverageGap gap : coverageGaps)
        {
            copy.restoreCoverageGap(
                    new CoverageGap(
                            gap.getAccount(),
                            gap.getGapId(),
                            gap.getStartedAt(),
                            gap.getEndedAt(),
                            gap.getReason()
                    )
            );
        }

        for (QuarantineRecord record : quarantine)
        {
            copy.restoreQuarantine(
                    new QuarantineRecord(
                            record.getAccount(),
                            record.getEventId(),
                            record.getEventType(),
                            record.getErrorCode(),
                            record.getFirstObservedAt(),
                            record.getQuarantinedAt()
                    )
            );
        }

        return copy;
    }

    void restorePending(PendingEvent event)
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event");
        }

        PendingEvent existing =
                findPending(
                        event.getAccount(),
                        event.getEventId()
                );

        if (existing != null)
        {
            if (!existing.getEventJson().equals(event.getEventJson()))
            {
                throw new IllegalStateException(
                        "duplicate restored eventId with different content"
                );
            }

            return;
        }

        if (
                event.getSerializedBytes() >
                        MAX_SINGLE_EVENT_BYTES ||
                        pending.size() >=
                                MAX_PENDING_EVENTS ||
                        totalPendingBytes() +
                                event.getSerializedBytes() >
                                MAX_PENDING_BYTES
        )
        {
            throw new IllegalStateException(
                    "persisted pending outbox exceeds bounds"
            );
        }

        pending.add(event);
    }

    void restoreCoverageGap(CoverageGap gap)
    {
        if (gap == null)
        {
            throw new IllegalArgumentException("gap");
        }

        for (CoverageGap existing : coverageGaps)
        {
            if (
                    existing.getAccount().equals(gap.getAccount()) &&
                            existing.getGapId().equals(gap.getGapId())
            )
            {
                if (
                        !existing.getStartedAt().equals(gap.getStartedAt()) ||
                                existing.getReason() != gap.getReason() ||
                                !java.util.Objects.equals(
                                        existing.getEndedAt(),
                                        gap.getEndedAt()
                                )
                )
                {
                    throw new IllegalStateException(
                            "duplicate restored gapId with different content"
                    );
                }

                return;
            }
        }

        coverageGaps.add(gap);
    }

    void restoreQuarantine(QuarantineRecord record)
    {
        if (record == null)
        {
            throw new IllegalArgumentException("record");
        }

        QuarantineRecord existing =
                findQuarantine(
                        record.getAccount(),
                        record.getEventId()
                );

        if (existing != null)
        {
            if (
                    !existing.toJson().equals(
                            record.toJson()
                    )
            )
            {
                throw new IllegalStateException(
                        "duplicate restored quarantine eventId with different metadata"
                );
            }

            return;
        }

        if (
                quarantine.size() >=
                        MAX_QUARANTINE_RECORDS ||
                        totalQuarantineBytes() +
                                record.getSerializedBytes() >
                                MAX_QUARANTINE_BYTES
        )
        {
            throw new IllegalStateException(
                    "persisted quarantine exceeds bounds"
            );
        }

        quarantine.add(record);
    }

    private void removePending(
            String account,
            String eventId
    )
    {
        pending.removeIf(
                event ->
                        event.getAccount().equals(account) &&
                                event.getEventId().equals(eventId)
        );
    }

    private QuarantineRecord findQuarantine(
            String account,
            String eventId
    )
    {
        for (QuarantineRecord record : quarantine)
        {
            if (
                    record.getAccount().equals(account) &&
                            record.getEventId().equals(eventId)
            )
            {
                return record;
            }
        }

        return null;
    }

    private void closeRecoveredOutboxCapacityGaps(
            String recoveredAt
    )
    {
        for (CoverageGap gap : coverageGaps)
        {
            if (
                    gap.isOpen() &&
                            gap.getReason() ==
                                    GapReason.OUTBOX_CAPACITY
            )
            {
                gap.close(recoveredAt);
            }
        }
    }

    private boolean hasOutboxCapacity()
    {
        return pending.size() < MAX_PENDING_EVENTS &&
                totalPendingBytes() < MAX_PENDING_BYTES;
    }

    private PendingEvent findPending(
            String account,
            String eventId
    )
    {
        for (PendingEvent event : pending)
        {
            if (
                    event.getAccount().equals(account) &&
                            event.getEventId().equals(eventId)
            )
            {
                return event;
            }
        }

        return null;
    }

    private static int utf8Length(String value)
    {
        return value
                .getBytes(StandardCharsets.UTF_8)
                .length;
    }

    private static String normalizeAccount(String account)
    {
        String value =
                requireText(account, "account")
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", "");

        if (value.isEmpty())
        {
            throw new IllegalArgumentException("account");
        }

        return value;
    }

    private static String requireText(
            String value,
            String label
    )
    {
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException(label);
        }

        return value;
    }

    private static String jsonString(String value)
    {
        if (value == null)
        {
            return "null";
        }

        return "\"" +
                value
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r") +
                "\"";
    }
}
