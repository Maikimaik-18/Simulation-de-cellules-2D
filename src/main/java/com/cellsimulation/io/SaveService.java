package com.cellsimulation.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.cellsimulation.model.SimulationEngine;

/**
 * Persistence service that saves and loads a {@link SimulationEngine} to
 * and from a binary file on disk.
 *
 * <p>The service uses Java's native binary serialization
 * ({@link ObjectOutputStream} and {@link ObjectInputStream}), as required
 * by the project specification. No external library is involved and the
 * on-disk format is the standard Java serialization stream.
 *
 * <p>{@code SaveService} is stateless and lightweight: a single instance can
 * be reused for any number of save and load operations. The class is not
 * itself {@code Serializable}: it represents a service, not data.
 *
 * <p>Errors are surfaced as standard checked exceptions
 * ({@link IOException}, {@link ClassNotFoundException}); they are not
 * wrapped into a custom exception type. Callers are expected to handle
 * them.
 */
public class SaveService {

    /**
     * Creates a new {@code SaveService}. The service holds no state, so
     * any number of instances can coexist.
     */
    public SaveService() {
        // no state to initialize
    }

    /**
     * Writes the given simulation engine to the file at {@code filePath}
     * using Java binary serialization. If the file already exists it is
     * overwritten.
     *
     * <p>The engine is serialized with all of its reachable state (grid,
     * settings, neighborhood strategy, tick counter, running flag). The
     * listener list is {@code transient} in {@code SimulationEngine} and
     * is therefore <em>not</em> persisted; a freshly loaded engine starts
     * with an empty listener list.
     *
     * @param engine   the engine to save; must not be {@code null}
     * @param filePath the path of the file to write; must not be
     *                 {@code null} nor blank
     * @throws IllegalArgumentException if {@code engine} is {@code null} or
     *                                  if {@code filePath} is {@code null}
     *                                  or blank
     * @throws IOException              if an I/O error occurs while
     *                                  writing the file
     */
    public void save(SimulationEngine engine, String filePath) throws IOException {
        if (engine == null) {
            throw new IllegalArgumentException("engine must not be null");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be blank");
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(engine);
        }
    }

    /**
     * Reads a simulation engine previously written by {@link #save} from
     * the file at {@code filePath}.
     *
     * <p>If the file does not contain a {@link SimulationEngine}
     * (for example because the file is corrupt or was written by a
     * different program) an {@link IOException} is thrown with a message
     * describing the situation.
     *
     * <p>The returned engine has an empty listener list (listeners are
     * {@code transient}); the caller is responsible for re-attaching any
     * listener it needs, in particular a fresh {@code StatisticsService}.
     *
     * @param filePath the path of the file to read; must not be
     *                 {@code null} nor blank
     * @return the deserialized {@link SimulationEngine}
     * @throws IllegalArgumentException if {@code filePath} is {@code null}
     *                                  or blank
     * @throws IOException              if an I/O error occurs while
     *                                  reading the file, or if the file
     *                                  does not contain a
     *                                  {@link SimulationEngine}
     * @throws ClassNotFoundException   if a class needed to deserialize
     *                                  the engine cannot be found on the
     *                                  class path
     */
    public SimulationEngine load(String filePath) throws IOException, ClassNotFoundException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be blank");
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            Object obj = in.readObject();
            if (!(obj instanceof SimulationEngine)) {
                throw new IOException("file does not contain a SimulationEngine: " + filePath);
            }
            return (SimulationEngine) obj;
        }
    }
}
