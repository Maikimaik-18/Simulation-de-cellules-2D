package com.cellsimulation.stats;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.cellsimulation.model.DiseaseState;
import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Person;
import com.cellsimulation.model.Position;

/**
 * Immutable snapshot of the population state at a given simulation tick.
 *
 * <p>A {@code Statistics} captures, for a single tick:
 * <ul>
 *   <li>the tick number at which the snapshot was taken;</li>
 *   <li>the count of persons currently in each {@link DiseaseState};</li>
 *   <li>the total population (sum of the four counts);</li>
 *   <li>the average infection duration among infected persons;</li>
 *   <li>the minimum and maximum immunity factor among living persons;</li>
 *   <li>the distribution of infection durations among infected persons.</li>
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
    private final double averageInfectionDays;
    private final double minImmunity;
    private final double maxImmunity;
    private final Map<Integer, Integer> infectionDaysHistogram;

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
        long infectionDaysSum = 0;
        int infectedCount = 0;
        double minImm = Double.NaN;
        double maxImm = Double.NaN;
        Map<Integer, Integer> histogram = new HashMap<>();
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                Person occupant = grid.getCell(new Position(row, col));
                if (occupant == null) {
                    continue;
                }
                DiseaseState state = occupant.getState();
                counts.put(state, counts.get(state) + 1);
                total++;
                if (state != DiseaseState.DECEASED) {
                    double immunity = occupant.getImmunityFactor();
                    if (Double.isNaN(minImm) || immunity < minImm) {
                        minImm = immunity;
                    }
                    if (Double.isNaN(maxImm) || immunity > maxImm) {
                        maxImm = immunity;
                    }
                }
                if (state == DiseaseState.INFECTED) {
                    int days = occupant.getInfectionDays();
                    infectionDaysSum += days;
                    infectedCount++;
                    histogram.merge(days, 1, Integer::sum);
                }
            }
        }
        this.tick = tick;
        this.countByState = Collections.unmodifiableMap(counts);
        this.totalPopulation = total;
        this.averageInfectionDays = infectedCount == 0
                ? 0.0
                : (double) infectionDaysSum / infectedCount;
        this.minImmunity = Double.isNaN(minImm) ? 0.0 : minImm;
        this.maxImmunity = Double.isNaN(maxImm) ? 0.0 : maxImm;
        this.infectionDaysHistogram = Collections.unmodifiableMap(histogram);
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

    /**
     * Returns the average number of infection days among the persons that are
     * currently infected.
     *
     * @return the mean infection duration of infected persons, or {@code 0.0}
     *         when no person is infected
     */
    public double getAverageInfectionDays() {
        return averageInfectionDays;
    }

    /**
     * Returns the lowest immunity factor found among the living persons
     * (every state except {@code DECEASED}).
     *
     * @return the minimum immunity factor, or {@code 0.0} when no living
     *         person is present
     */
    public double getMinImmunity() {
        return minImmunity;
    }

    /**
     * Returns the highest immunity factor found among the living persons
     * (every state except {@code DECEASED}).
     *
     * @return the maximum immunity factor, or {@code 0.0} when no living
     *         person is present
     */
    public double getMaxImmunity() {
        return maxImmunity;
    }

    /**
     * Returns the distribution of infection durations among the infected
     * persons: each key is a number of infection days and the associated
     * value is how many infected persons have been sick for exactly that many
     * days.
     *
     * <p>The returned map is an unmodifiable view and only contains entries
     * for durations that are actually present on the grid.
     *
     * @return an unmodifiable map from infection-day count to number of
     *         infected persons
     */
    public Map<Integer, Integer> getInfectionDaysHistogram() {
        return infectionDaysHistogram;
    }
}
