package com.cellsimulation.stats;

import com.cellsimulation.model.Grid;

/**
 * Contract for objects that observe the simulation engine (Observer pattern).
 *
 * <p>Listeners are notified at every simulation tick and whenever the engine
 * is reset. A typical implementation is {@code StatisticsService}, which
 * records a snapshot of the population state after each tick.
 */
public interface SimulationListener {

    /**
     * Invoked by the simulation engine after each tick has been computed.
     *
     * @param grid      the current state of the grid
     * @param tickCount the number of ticks elapsed since the last reset
     */
    void onTick(Grid grid, int tickCount);

    /**
     * Invoked by the simulation engine whenever it is reset to its initial
     * state, so that listeners can clear any accumulated data.
     */
    void onReset();
}
