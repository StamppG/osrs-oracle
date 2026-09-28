package com.osrsoracle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class Slice5StateCodec
{
    private static final int MAGIC = 0x53473535;
    private static final int FORMAT_VERSION = 1;

    private static final int MAX_STRING_BYTES =
            Slice5OutboxState.MAX_PENDING_BYTES + 4096;

    private static final int MAX_GAP_RECORDS = 4096;

    byte[] encode(Slice5OutboxState state)
            throws IOException
    {
        if (state == null)
        {
            throw new IllegalArgumentException("state");
        }

        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        try (
                DataOutputStream out =
                        new DataOutputStream(bytes)
        )
        {
            out.writeInt(MAGIC);
            out.writeInt(FORMAT_VERSION);

            List<Slice5OutboxState.PendingEvent> pending =
                    state.pendingSnapshot();

            out.writeInt(pending.size());

            for (Slice5OutboxState.PendingEvent event : pending)
            {
                writeString(out, event.getAccount());
                writeString(out, event.getEventId());
                writeString(out, event.getEventType());
                writeString(out, event.getObservedAt());
                writeString(out, event.getEventJson());
                out.writeBoolean(event.isSuppressed());
            }

            List<Slice5OutboxState.CoverageGap> gaps =
                    state.coverageGapSnapshot();

            out.writeInt(gaps.size());

            for (Slice5OutboxState.CoverageGap gap : gaps)
            {
                writeString(out, gap.getAccount());
                writeString(out, gap.getGapId());
                writeString(out, gap.getStartedAt());
                writeNullableString(out, gap.getEndedAt());
                writeString(out, gap.getReason().name());
            }

            List<Slice5OutboxState.QuarantineRecord> quarantine =
                    state.quarantineSnapshot();

            out.writeInt(quarantine.size());

            for (
                    Slice5OutboxState.QuarantineRecord record :
                            quarantine
            )
            {
                writeString(out, record.getAccount());
                writeString(out, record.getEventId());
                writeString(out, record.getEventType());
                writeString(out, record.getErrorCode());
                writeString(out, record.getFirstObservedAt());
                writeString(out, record.getQuarantinedAt());
            }
        }

        return bytes.toByteArray();
    }

    Slice5OutboxState decode(byte[] bytes)
            throws IOException
    {
        if (bytes == null)
        {
            throw new IllegalArgumentException("bytes");
        }

        Slice5OutboxState state =
                new Slice5OutboxState();

        try (
                DataInputStream in =
                        new DataInputStream(
                                new ByteArrayInputStream(bytes)
                        )
        )
        {
            if (in.readInt() != MAGIC)
            {
                throw new IOException(
                        "invalid Slice 5 state magic"
                );
            }

            if (in.readInt() != FORMAT_VERSION)
            {
                throw new IOException(
                        "unsupported Slice 5 state version"
                );
            }

            int pendingCount =
                    readCount(
                            in,
                            Slice5OutboxState.MAX_PENDING_EVENTS,
                            "pending"
                    );

            for (int i = 0; i < pendingCount; i++)
            {
                state.restorePending(
                        new Slice5OutboxState.PendingEvent(
                                readString(in),
                                readString(in),
                                readString(in),
                                readString(in),
                                readString(in),
                                in.readBoolean()
                        )
                );
            }

            int gapCount =
                    readCount(
                            in,
                            MAX_GAP_RECORDS,
                            "coverage gaps"
                    );

            for (int i = 0; i < gapCount; i++)
            {
                String account =
                        readString(in);

                String gapId =
                        readString(in);

                String startedAt =
                        readString(in);

                String endedAt =
                        readNullableString(in);

                String reason =
                        readString(in);

                final Slice5OutboxState.GapReason gapReason;

                try
                {
                    gapReason =
                            Slice5OutboxState.GapReason.valueOf(
                                    reason
                            );
                }
                catch (IllegalArgumentException e)
                {
                    throw new IOException(
                            "invalid coverage gap reason",
                            e
                    );
                }

                state.restoreCoverageGap(
                        new Slice5OutboxState.CoverageGap(
                                account,
                                gapId,
                                startedAt,
                                endedAt,
                                gapReason
                        )
                );
            }

            int quarantineCount =
                    readCount(
                            in,
                            Slice5OutboxState.MAX_QUARANTINE_RECORDS,
                            "quarantine"
                    );

            for (int i = 0; i < quarantineCount; i++)
            {
                state.restoreQuarantine(
                        new Slice5OutboxState.QuarantineRecord(
                                readString(in),
                                readString(in),
                                readString(in),
                                readString(in),
                                readString(in),
                                readString(in)
                        )
                );
            }

            if (in.read() != -1)
            {
                throw new IOException(
                        "unexpected trailing Slice 5 state bytes"
                );
            }
        }
        catch (EOFException e)
        {
            throw new IOException(
                    "truncated Slice 5 state",
                    e
            );
        }
        catch (IllegalStateException | IllegalArgumentException e)
        {
            throw new IOException(
                    "invalid Slice 5 state",
                    e
            );
        }

        return state;
    }

    private static int readCount(
            DataInputStream in,
            int maximum,
            String label
    )
            throws IOException
    {
        int count =
                in.readInt();

        if (count < 0 || count > maximum)
        {
            throw new IOException(
                    "invalid " + label + " count"
            );
        }

        return count;
    }

    private static void writeNullableString(
            DataOutputStream out,
            String value
    )
            throws IOException
    {
        out.writeBoolean(value != null);

        if (value != null)
        {
            writeString(out, value);
        }
    }

    private static String readNullableString(
            DataInputStream in
    )
            throws IOException
    {
        return in.readBoolean()
                ? readString(in)
                : null;
    }

    private static void writeString(
            DataOutputStream out,
            String value
    )
            throws IOException
    {
        if (value == null)
        {
            throw new IllegalArgumentException(
                    "persistent string"
            );
        }

        byte[] bytes =
                value.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > MAX_STRING_BYTES)
        {
            throw new IOException(
                    "persistent string exceeds bound"
            );
        }

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(
            DataInputStream in
    )
            throws IOException
    {
        int length =
                in.readInt();

        if (length < 0 || length > MAX_STRING_BYTES)
        {
            throw new IOException(
                    "invalid persistent string length"
            );
        }

        byte[] bytes =
                new byte[length];

        in.readFully(bytes);

        return new String(
                bytes,
                StandardCharsets.UTF_8
        );
    }
}