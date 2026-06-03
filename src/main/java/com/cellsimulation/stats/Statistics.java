package com.cellsimulation.stats;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.cellsimulation.model.DiseaseState;
import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Person;
import com.cellsimulation.model.Position;

/**
 * Immutable snapshot of the population state at a given simulation tick.
 *
 * <p>A {@code Statistics} captures three pieces of information:
 * <ul>
 *   <li>the tick number at which the snapshot was taken;</li>
 *   <li>the count of persons currently in each {@link DiseaseState};</li>
 *   <li>the total population (sum of the four counts).</li>
 * </ul>
 *
 * <p>The class is intentionally immutable: once built, none of its fields can
 * be modified, and {@link #getCountByState()} exposes an unmodifiable view of
 * the internal map. This makes it safe to share a snapshot across observers
 * and threads without defensive copies.
 *
 * <p>{@code Statistics} is {@link Serializable} so that it can be stored in
 * the history kept by {@link StatisticsService} and, if needed in the future,
 * persisted to disk alongside other model objects.
 */
public final class Statistics implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int tick;
    private final Map<DiseaseState, Integer> countByState;
    private final int totalPopulation;

    /**
     * Builds a snapshot of the given grid at the given tick.
     *
     * <p>The grid is traversed once in row-major order; every non-empty cell
     * increments the counter of its current {@link DiseaseState}. The
     * resulting map always contains an entry for each of the four states
     * ({@code SUSCEPTIBLE}, {@code INFECTED}, {@code RECOVERED},
     * {@code DECEASED}), with a value of {@code 0} when no person is in that
     * state. The map is wrapped with
     * {@link Collections#unmodifiableMap(Map)} to guarantee immutability.
     *
     * @param grid the grid to scan; must not be {@code null}
     * @param tick the tick number at which the snapshot is taken; must be
     *             greater than or equal to {@code 0}
     * @throws IllegalArgumentException if {@code grid} is {@code null} or if
     *                                  {@code tick} is negative
     */
    public Statistics(Grid grid, int tick) {
        if (grid == null) {
            throw new IllegalArgumentException("grid must not be null");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be >= 0, got " + tick);
        }
        EnumMap<DiseaseState, Integer> counts = new EnumMap<>(DiseaseState.class);
        for (DiseaseState state : DiseaseState.values()) {
            counts.put(state, 0);
        }
        int total = 0;
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                Person occupant = grid.getCell(new Position(row, col));
                if (occupant == null) {
                    continue;
                }
                DiseaseState state = occupant.getState();
                counts.put(state, counts.get(state) + 1);
                total++;
            }
        }
        this.tick = tick;
        this.countByState = Collections.unmodifiableMap(counts);
        this.totalPopulation = total;
    }

    /**
     * @return the tick number at which this snapshot was taken
     */
    public int getTick() {
        return tick;
    }

    /**
     * Returns the number of persons in the given state at the time of the
     * snapshot.
     *
     * @param state the disease state to look up
     * @return the count for that state, or {@code 0} if no entry exists
     *         (which should not happen, since the constructor populates all
     *         four states)
     */
    public int getCount(DiseaseState state) {
        Integer value = countByState.get(state);
        return value == null ? 0 : value;
    }

    /**
     * @return the total number of persons present on the grid at the time of
     *         the snapshot (sum of the four state counts)
     */
    public int getTotalPopulation() {
        return totalPopulation;
    }

    /**
     * Returns the full count-by-state map.
     *
     * <p>The returned map is already an unmodifiable view of the internal
     * map; it is safe to share without further copying. It always contains
     * an entry for each of the four {@link DiseaseState} values.
     *
     * @return an unmodifiable map from disease state to population count
     */
    public Map<DiseaseState, Integer> getCountByState() {
        return countByState;
    }
}
