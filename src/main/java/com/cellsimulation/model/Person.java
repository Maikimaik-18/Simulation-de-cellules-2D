package com.cellsimulation.model;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

import com.cellsimulation.neighborhood.NeighborhoodStrategy;

/**
 * An individual living on the simulation grid.
 *
 * <p>A {@code Person} owns a current health {@link DiseaseState}, a
 * {@link Position} on the grid, a counter of how many ticks have been
 * spent in the {@code INFECTED} state and an individual
 * {@code immunityFactor} that linearly lowers its probability of being
 * contaminated. It may also be {@code vaccinated}, in which case its
 * effective immunity is raised to at least the configured vaccine efficacy.
 * Its three behavioral methods
 * ({@link #update}, {@link #move} and {@link #spread}) implement the SIR +
 * Deceased disease propagation model:
 * <ul>
 *   <li>{@code update} ages the infection and resolves
 *       death / natural recovery / forced recovery.</li>
 *   <li>{@code move} optionally relocates the person to a free neighboring
 *       cell.</li>
 *   <li>{@code spread} contaminates susceptible neighbors using a
 *       case-contact model.</li>
 * </ul>
 *
 * <p>The class is {@link Serializable} so that the whole simulation state
 * can be persisted to disk through the {@code SaveService}.
 */
public class Person implements Serializable {

    private static final Random RANDOM = new Random();

    private DiseaseState state;
    private Position position;
    private int infectionDays;
    private double immunityFactor;
    private boolean vaccinated;
    private int infectedAtTick;

    /**
     * Creates a new {@code Person} with the given initial state, position
     * and immunity factor. The infection counter starts at zero.
     *
     * @param state          the initial health state
     * @param position       the initial position on the grid
     * @param immunityFactor the individual immunity factor in
     *                       {@code [0.0, 1.0]}; it linearly lowers the
     *                       probability of being contaminated
     */
    public Person(DiseaseState state, Position position, double immunityFactor) {
        this.state = state;
        this.position = position;
        this.infectionDays = 0;
        this.immunityFactor = immunityFactor;
        this.vaccinated = false;
        this.infectedAtTick = -1;
    }

    /**
     * Creates a new {@code Person} with no individual immunity
     * ({@code immunityFactor == 0.0}).
     *
     * <p>Kept as a convenience so that call sites that do not deal with
     * immunity keep compiling and behaving exactly as before this feature
     * was introduced.
     *
     * @param state    the initial health state
     * @param position the initial position on the grid
     */
    public Person(DiseaseState state, Position position) {
        this(state, position, 0.0);
    }

    /**
     * @return the current health state of this person
     */
    public DiseaseState getState() {
        return state;
    }

    /**
     * @return the current position of this person on the grid
     */
    public Position getPosition() {
        return position;
    }

    /**
     * @return the number of ticks elapsed since this person became infected
     */
    public int getInfectionDays() {
        return infectionDays;
    }

    /**
     * @return the individual immunity factor in {@code [0.0, 1.0]}
     */
    public double getImmunityFactor() {
        return immunityFactor;
    }

    /**
     * Sets the health state of this person.
     *
     * @param state the new state
     */
    public void setState(DiseaseState state) {
        this.state = state;
    }

    /**
     * Sets the position of this person on the grid.
     *
     * @param position the new position
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Sets the individual immunity factor of this person.
     *
     * @param immunityFactor the new immunity factor in {@code [0.0, 1.0]}
     */
    public void setImmunityFactor(double immunityFactor) {
        this.immunityFactor = immunityFactor;
    }

    /**
     * @return {@code true} if this person has been vaccinated
     */
    public boolean isVaccinated() {
        return vaccinated;
    }

    /**
     * Sets the vaccination flag of this person.
     *
     * <p>Vaccination is a permanent attribute: a vaccinated person that
     * still gets infected (the vaccine is not necessarily perfect) keeps its
     * vaccinated flag.
     *
     * @param vaccinated {@code true} to mark the person as vaccinated
     */
    public void setVaccinated(boolean vaccinated) {
        this.vaccinated = vaccinated;
    }

    /**
     * @return {@code true} if this person is currently infected
     */
    public boolean isInfected() {
        return state == DiseaseState.INFECTED;
    }

    /**
     * @return {@code true} if this person has died from the disease
     */
    public boolean isDead() {
        return state == DiseaseState.DECEASED;
    }

    /**
     * Advances the disease for one tick.
     *
     * <p>Only infected persons evolve. Each tick increments the infection
     * counter and then resolves transitions in this strict order:
     * <ol>
     *   <li>mortality check (probability {@code mortalityProbability});</li>
     *   <li>natural recovery (probability {@code recoveryProbability});</li>
     *   <li>forced recovery once the counter reaches
     *       {@code maxInfectionDays}.</li>
     * </ol>
     *
     * @param settings the simulation settings supplying the probabilities
     *                 and the infection duration cap
     */
    public void update(SimulationSettings settings) {
        if (state != DiseaseState.INFECTED) {
            return;
        }
        infectionDays++;

        if (RANDOM.nextDouble() < settings.getMortalityProbability()) {
            state = DiseaseState.DECEASED;
            return;
        }

        if (RANDOM.nextDouble() < settings.getRecoveryProbability()) {
            state = DiseaseState.RECOVERED;
            return;
        }

        if (infectionDays >= settings.getMaxInfectionDays()) {
            state = DiseaseState.RECOVERED;
        }
    }

    /**
     * Optionally relocates this person to a random free neighboring cell.
     *
     * <p>A deceased person never moves. Otherwise a single uniform draw
     * decides whether the person attempts a move during this tick: the
     * probability of attempting is {@code mobilityRate}. When the attempt
     * succeeds and at least one neighboring cell is empty, a target is
     * chosen uniformly at random among the empty neighbors and the grid is
     * updated accordingly.
     *
     * @param grid         the grid on which this person lives
     * @param neighborhood the neighborhood strategy used to identify the
     *                     candidate cells
     * @param settings     the simulation settings supplying the mobility
     *                     rate
     */
    public void move(Grid grid, NeighborhoodStrategy neighborhood, SimulationSettings settings) {
        if (state == DiseaseState.DECEASED) {
            return;
        }
        if (RANDOM.nextDouble() >= settings.getMobilityRate()) {
            return;
        }

        List<Position> emptyNeighbors = grid.getEmptyNeighbors(position, neighborhood);
        if (emptyNeighbors.isEmpty()) {
            return;
        }

        Position target = emptyNeighbors.get(RANDOM.nextInt(emptyNeighbors.size()));
        grid.removeCell(position);
        this.position = target;
        grid.setCell(target, this);
    }

    /**
     * Spreads the disease to susceptible neighbors using a case-contact
     * model.
     *
     * <p>Only an infected person can transmit. For every neighbor returned
     * by the strategy, a susceptible occupant becomes infected with an
     * effective probability of
     * {@code transmissionProbability * (1 - effectiveImmunity)}. The
     * neighbor's effective immunity is its individual {@code immunityFactor},
     * raised to the configured {@code vaccineEfficacy} when the neighbor is
     * vaccinated. Empty cells and non-susceptible neighbors are ignored.
     * This method never mutates {@code this}: only neighbors may change
     * state.
     *
     * @param grid         the grid on which this person lives
     * @param neighborhood the neighborhood strategy used to identify the
     *                     candidate cells
     * @param settings     the simulation settings supplying the
     *                     transmission probability
     * @param currentTick  the index of the tick being played; a person that
     *                     became infected during this very tick does not
     *                     propagate until the next one, which keeps the
     *                     cellular-automaton update generational and the
     *                     spread strictly limited to the chosen neighborhood
     */
    public void spread(Grid grid, NeighborhoodStrategy neighborhood,
                       SimulationSettings settings, int currentTick) {
        if (state != DiseaseState.INFECTED) {
            return;
        }
        if (infectedAtTick == currentTick) {
            return;
        }

        List<Position> neighbors = neighborhood.getNeighbors(grid, position);
        for (Position neighborPos : neighbors) {
            Person neighbor = grid.getCell(neighborPos);
            if (neighbor == null) {
                continue;
            }
            if (neighbor.state != DiseaseState.SUSCEPTIBLE) {
                continue;
            }
            double effectiveImmunity = neighbor.vaccinated
                    ? Math.max(neighbor.immunityFactor, settings.getVaccineEfficacy())
                    : neighbor.immunityFactor;
            double effectiveProb = settings.getTransmissionProbability()
                    * (1.0 - effectiveImmunity);
            if (RANDOM.nextDouble() < effectiveProb) {
                neighbor.state = DiseaseState.INFECTED;
                neighbor.infectionDays = 0;
                neighbor.infectedAtTick = currentTick;
            }
        }
    }
}
