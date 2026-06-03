package com.cellsimulation.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cellsimulation.model.Grid;

/**
 * Observer of the simulation engine that records a {@link Statistics}
 * snapshot after every tick.
 *
 * <p>The service implements {@link SimulationListener} and is intended to be
 * registered on a {@code SimulationEngine} via
 * {@code engine.addListener(...)}. Each {@code onTick} notification appends
 * a new snapshot to the in-memory history; an {@code onReset} notification
 * clears the history so that the next run starts from a clean slate.
 *
 * <p>The service is deliberately <em>not</em> {@code Serializable}: the
 * history is observational data (useful for plotting live charts) rather
 * than core simulation state. When the engine is saved with the
 * {@code SaveService} the history is dropped; the caller is expected to
 * attach a fresh {@code StatisticsService} after {@code load}.
 *
 * <p>This class is not thread-safe; it assumes a single-threaded simulation
 * loop, which matches the design of {@code SimulationEngine}.
 */
public class StatisticsService implements SimulationListener {

    private final List<Statistics> history = new ArrayList<>();

    /**
     * Records a fresh snapshot of the grid for the given tick.
     *
     * @param grid      the current state of the grid
     * @param tickCount the number of ticks elapsed since the last reset
     */
    @Override
    public void onTick(Grid grid, int tickCount) {
        history.add(new Statistics(grid, tickCount));
    }

    /**
     * Clears the accumulated history. Called by the engine when it is
     * reset, so that the next series of ticks starts from an empty
     * history.
     */
    @Override
    public void onReset() {
        history.clear();
    }

    /**
     * Returns the most recent snapshot, or {@code null} if the history is
     * empty (typically because no tick has been played yet, or because the
     * service has just been reset).
     *
     * <p>Callers must handle the {@code null} case explicitly; no exception
     * is thrown.
     *
     * @return the latest {@link Statistics}, or {@code null} if none has
     *         been recorded yet
     */
    public Statistics getLatest() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    /**
     * Returns the full history of snapshots, oldest first.
     *
     * <p>The returned list is an unmodifiable view of the internal list:
     * its contents change as new ticks are recorded, but callers cannot
     * mutate it. Each {@link Statistics} element is itself immutable.
     *
     * @return an unmodifiable view of the snapshot history
     */
    public List<Statistics> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
