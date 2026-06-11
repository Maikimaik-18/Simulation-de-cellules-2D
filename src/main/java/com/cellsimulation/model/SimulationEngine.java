package com.cellsimulation.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.cellsimulation.neighborhood.NeighborhoodStrategy;
import com.cellsimulation.stats.SimulationListener;

/**
 * Orchestrator of the disease-propagation simulation.
 *
 * <p>The engine owns the {@link Grid} (the playground), the
 * {@link SimulationSettings} (the probabilities and durations) and the
 * current {@link NeighborhoodStrategy}. It is intentionally <em>passive</em>:
 * it knows how to play exactly one tick when {@link #step()} is called, but
 * it never spawns a thread or schedules a timer. The simulation cadence is
 * driven from the outside, by the JavaFX window or by the CLI runner.
 *
 * <p>A single tick proceeds as follows:
 * <ol>
 *   <li>increment {@code tickCount};</li>
 *   <li>snapshot every {@link Person} currently on the grid;</li>
 *   <li>for each snapshotted person, call
 *       {@link Person#update update} then {@link Person#move move} then
 *       {@link Person#spread spread};</li>
 *   <li>notify every {@link SimulationListener} via
 *       {@link SimulationListener#onTick onTick}.</li>
 * </ol>
 *
 * <p>The snapshot is critical: without it, a person that has just moved to
 * a yet-unvisited cell would be processed twice in the same tick.
 *
 * <p>The engine is {@link Serializable} so that the whole simulation state
 * can be persisted to disk through the {@code SaveService}. The
 * {@code listeners} field is {@code transient} because listeners (such as a
 * UI controller) are not necessarily serializable; after deserialization
 * the engine starts with an empty listener list and callers are expected to
 * re-attach their listeners.
 */
public class SimulationEngine implements Serializable {

    private static final Random RANDOM = new Random();

    private final Grid grid;
    private final SimulationSettings settings;
    private NeighborhoodStrategy neighborhood;
    private boolean running;
    private int tickCount;
    private transient List<SimulationListener> listeners;

    /**
     * Creates a new simulation engine wired to the given grid, settings and
     * neighborhood strategy.
     *
     * <p>The engine starts paused ({@code running == false}) with a tick
     * count of zero and no registered listener.
     *
     * @param grid         the playground; must not be {@code null}
     * @param settings     the simulation settings; must not be {@code null}
     * @param neighborhood the initial neighborhood strategy; must not be
     *                     {@code null}
     * @throws IllegalArgumentException if any argument is {@code null}
     */
    public SimulationEngine(Grid grid,
                            SimulationSettings settings,
                            NeighborhoodStrategy neighborhood) {
        if (grid == null) {
            throw new IllegalArgumentException("grid must not be null");
        }
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }
        if (neighborhood == null) {
            throw new IllegalArgumentException("neighborhood must not be null");
        }
        this.grid = grid;
        this.settings = settings;
        this.neighborhood = neighborhood;
        this.running = false;
        this.tickCount = 0;
        this.listeners = new ArrayList<>();
    }

    /**
     * Plays a single simulation tick.
     *
     * <p>The method first increments the tick counter, then snapshots every
     * person currently on the grid. Each snapshotted person is processed in
     * the strict order {@code update -> move -> spread}; deceased persons
     * are no-ops because their three methods short-circuit on the deceased
     * state. Finally every listener is notified through {@code onTick}; the
     * notification iterates over a defensive copy of the listener list, so
     * that a listener that unsubscribes itself during {@code onTick} does
     * not break the iteration.
     *
     * <p>{@code step()} does not modify the {@code running} flag: the engine
     * can be advanced one tick at a time even when paused.
     */
    public void step() {
        tickCount++;

        List<Person> snapshot = new ArrayList<>();
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                Person person = grid.getCell(new Position(row, col));
                if (person != null) {
                    snapshot.add(person);
                }
            }
        }

        for (Person person : snapshot) {
            person.update(settings);
            person.move(grid, neighborhood, settings);
            person.spread(grid, neighborhood, settings,tickCount);
        }

        for (SimulationListener listener : new ArrayList<>(listeners)) {
            listener.onTick(grid, tickCount);
        }
    }

    /**
     * Switches the engine to its "running" state.
     *
     * <p>No thread is started: the engine remains passive. The caller is
     * responsible for repeatedly invoking {@link #step()} at the desired
     * cadence as long as {@link #isRunning()} returns {@code true}.
     */
    public void start() {
        running = true;
    }

    /**
     * Switches the engine to its "paused" state.
     *
     * <p>The tick counter is preserved; only the {@code running} flag flips
     * to {@code false}.
     */
    public void pause() {
        running = false;
    }

    /**
     * Resets the simulation to an empty state.
     *
     * <p>Concretely the method pauses the engine, resets the tick counter to
     * zero, empties every cell of the grid (the grid dimensions and toric
     * mode are preserved) and finally notifies every listener through
     * {@link SimulationListener#onReset()}. The notification iterates over a
     * defensive copy of the listener list.
     *
     * <p>The current {@link SimulationSettings} and
     * {@link NeighborhoodStrategy} are kept; only the inhabitants are
     * removed.
     */
    public void reset() {
        running = false;
        tickCount = 0;

        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                Position position = new Position(row, col);
                if (grid.getCell(position) != null) {
                    grid.removeCell(position);
                }
            }
        }

        for (SimulationListener listener : new ArrayList<>(listeners)) {
            listener.onReset();
        }
    }

    /**
     * Creates a new {@link Person} with the given initial state and places
     * it on the grid at the specified position.
     *
     * <p>In bounded mode an out-of-bounds {@code position} causes
     * {@link Grid#setCell(Position, Person)} to throw; the exception is
     * propagated unchanged.
     *
     * @param position the target position; must not be {@code null}
     * @param state    the initial health state of the new person; must not
     *                 be {@code null}
     * @throws IllegalArgumentException if {@code position} or {@code state}
     *                                  is {@code null}, or (in bounded mode)
     *                                  if {@code position} lies outside the
     *                                  grid
     */
    public void addCell(Position position, DiseaseState state) {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        double immunity = drawImmunity();
        Person person = new Person(state, position, immunity);
        if (state == DiseaseState.INFECTED) {
            person.setInfectedAtTick(tickCount);
        }
        grid.setCell(position, person);


    }

    /**
     * Draws an immunity factor according to the current settings.
     *
     * <p>Returns {@code meanImmunity} when {@code immunityVariance == 0.0}.
     * Otherwise samples a value from a Gaussian distribution centered on
     * {@code meanImmunity} with the configured variance, clamped to
     * {@code [0.0, 1.0]} (truncated normal).
     *
     * @return the drawn immunity factor in {@code [0.0, 1.0]}
     */
    private double drawImmunity() {
        double mean = settings.getMeanImmunity();
        double variance = settings.getImmunityVariance();
        if (variance == 0.0) {
            return mean;
        }
        double draw = mean + RANDOM.nextGaussian() * variance;
        return Math.max(0.0, Math.min(1.0, draw));
    }

    /**
     * Removes the inhabitant of the given cell, if any.
     *
     * <p>Delegates to {@link Grid#removeCell(Position)}. Provided on the
     * engine so that the UI layer can manipulate the simulation through a
     * single API.
     *
     * @param position the position to clear; must not be {@code null}
     * @throws IllegalArgumentException if {@code position} is {@code null},
     *                                  or (in bounded mode) if it lies
     *                                  outside the grid
     */
    public void removeCell(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        grid.removeCell(position);
    }

    /**
     * Registers a listener that will be notified at every tick and on every
     * reset.
     *
     * @param listener the listener to register; must not be {@code null}
     * @throws IllegalArgumentException if {@code listener} is {@code null}
     */
    public void addListener(SimulationListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Unregisters a previously registered listener.
     *
     * <p>The operation is idempotent: removing a listener that is not in
     * the list has no effect and does not throw.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Replaces the current neighborhood strategy with a new one.
     *
     * <p>This is intended to be called by the UI when the user switches the
     * neighborhood mode from the selector. The change takes effect on the
     * very next tick.
     *
     * @param neighborhood the new strategy; must not be {@code null}
     * @throws IllegalArgumentException if {@code neighborhood} is
     *                                  {@code null}
     */
    public void setNeighborhood(NeighborhoodStrategy neighborhood) {
        if (neighborhood == null) {
            throw new IllegalArgumentException("neighborhood must not be null");
        }
        this.neighborhood = neighborhood;
    }

    /**
     * @return the grid owned by this engine
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * @return the simulation settings owned by this engine
     */
    public SimulationSettings getSettings() {
        return settings;
    }

    /**
     * @return the neighborhood strategy currently in use
     */
    public NeighborhoodStrategy getNeighborhood() {
        return neighborhood;
    }

    /**
     * @return {@code true} if the engine is in its "running" state,
     *         {@code false} if it is paused
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return the number of ticks played since the last reset
     */
    public int getTickCount() {
        return tickCount;
    }

    /**
     * Restores the engine after deserialization.
     *
     * <p>The {@code listeners} field is {@code transient} and therefore
     * arrives as {@code null} after the default read; this method
     * re-initializes it with an empty {@link ArrayList} so that subsequent
     * calls to {@link #addListener(SimulationListener)} work normally. The
     * caller (typically the UI controller) is responsible for re-attaching
     * the listeners it needs.
     *
     * @param in the input stream from which to read the engine state
     * @throws IOException            if an I/O error occurs while reading
     * @throws ClassNotFoundException if a class needed to deserialize a
     *                                field cannot be found
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listeners = new ArrayList<>();
    }
}
