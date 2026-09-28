package com.osrsoracle;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.runelite.client.RuneLite;

final class Slice5StateFileStore
{
    private static final String DIRECTORY_NAME =
            "oracle";

    private static final String STATE_FILE_NAME =
            "slice5-state.bin";

    private final Path stateFile;
    private final Slice5StateCodec codec;

    Slice5StateFileStore()
    {
        this(
                RuneLite.RUNELITE_DIR
                        .toPath()
                        .resolve(DIRECTORY_NAME)
                        .resolve(STATE_FILE_NAME),
                new Slice5StateCodec()
        );
    }

    Slice5StateFileStore(
            Path stateFile,
            Slice5StateCodec codec
    )
    {
        if (stateFile == null)
        {
            throw new IllegalArgumentException("stateFile");
        }

        if (codec == null)
        {
            throw new IllegalArgumentException("codec");
        }

        this.stateFile = stateFile;
        this.codec = codec;
    }

    boolean exists()
    {
        return Files.exists(stateFile);
    }

    Slice5OutboxState load()
            throws IOException
    {
        if (!Files.exists(stateFile))
        {
            return new Slice5OutboxState();
        }

        byte[] bytes =
                Files.readAllBytes(stateFile);

        return codec.decode(bytes);
    }

    void save(Slice5OutboxState state)
            throws IOException
    {
        if (state == null)
        {
            throw new IllegalArgumentException("state");
        }

        Path parent =
                stateFile.getParent();

        if (parent == null)
        {
            throw new IOException(
                    "Slice 5 state file has no parent directory"
            );
        }

        Files.createDirectories(parent);

        byte[] bytes =
                codec.encode(state);

        Path temporary =
                parent.resolve(
                        STATE_FILE_NAME + ".tmp"
                );

        try
        {
            Files.write(
                    temporary,
                    bytes
            );

            moveIntoPlace(
                    temporary,
                    stateFile
            );
        }
        finally
        {
            Files.deleteIfExists(
                    temporary
            );
        }
    }

    Path getStateFile()
    {
        return stateFile;
    }

    private static void moveIntoPlace(
            Path source,
            Path destination
    )
            throws IOException
    {
        try
        {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}