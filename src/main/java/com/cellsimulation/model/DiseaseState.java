package com.cellsimulation.model;

/**
 * Health state of a {@code Person} in the disease-propagation model
 * (a simple SIR variant extended with a terminal {@code DECEASED} state).
 *
 * <p>The four possible states are:
 * <ul>
 *   <li>{@code SUSCEPTIBLE} - healthy but vulnerable to infection.</li>
 *   <li>{@code INFECTED} - currently carrying the disease and able to
 *       contaminate susceptible neighbors.</li>
 *   <li>{@code RECOVERED} - has overcome the disease and is no longer
 *       infectious nor susceptible.</li>
 *   <li>{@code DECEASED} - has died from the disease; the cell is terminal
 *       and no longer participates in the simulation.</li>
 * </ul>
 *
 * <p>This enum carries no behavior and no UI-related data on purpose: the
 * {@code com.cellsimulation.model} package must remain independent from
 * JavaFX. The mapping from a state to a display color belongs to the UI
 * layer.
 */
public enum DiseaseState {
    SUSCEPTIBLE,
    INFECTED,
    RECOVERED,
    DECEASED
}
