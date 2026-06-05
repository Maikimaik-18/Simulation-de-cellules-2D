package com.cellsimulation.model;

import java.io.Serializable;

/**
 * Centralizes the six tunable parameters of the simulation.
 *
 * <p>Both the {@code SimulationEngine} and the {@code Person} instances read
 * their configuration from a single {@code SimulationSettings} instance, so
 * that probabilities, durations and speed remain consistent across the
 * whole simulation.
 *
 * <p>The nine parameters are:
 * <ul>
 *   <li>{@code simulationSpeed} - ticks per second.</li>
 *   <li>{@code transmissionProbability} - chance of contaminating a
 *       susceptible neighbor per tick.</li>
 *   <li>{@code recoveryProbability} - chance of recovering naturally per
 *       tick.</li>
 *   <li>{@code mortalityProbability} - chance of dying from the disease per
 *       tick.</li>
 *   <li>{@code mobilityRate} - chance of attempting a move per tick.</li>
 *   <li>{@code maxInfectionDays} - hard cap of infection duration after
 *       which the person recovers automatically.</li>
 *   <li>{@code meanImmunity} - mean of the immunity factor drawn for each
 *       new person.</li>
 *   <li>{@code immunityVariance} - spread of the truncated-normal
 *       distribution used to draw each immunity factor around
 *       {@code meanImmunity}.</li>
 *   <li>{@code vaccineEfficacy} - minimum immunity granted to a vaccinated
 *       person.</li>
 * </ul>
 *
 * <p>This class is {@link Serializable} so that it can be embedded in a
 * serialized {@code SimulationEngine} when the user saves the simulation
 * state to disk.
 */
public class SimulationSettings implements Serializable {

    private int simulationSpeed;
    private double transmissionProbability;
    private double recoveryProbability;
    private double mortalityProbability;
    private double mobilityRate;
    private int maxInfectionDays;
    private double meanImmunity;
    private double immunityVariance;
    private double vaccineEfficacy;

    /**
     * Creates a new {@code SimulationSettings} populated with the default
     * values:
     * <ul>
     *   <li>{@code simulationSpeed = 1}</li>
     *   <li>{@code transmissionProbability = 0.30}</li>
     *   <li>{@code recoveryProbability = 0.05}</li>
     *   <li>{@code mortalityProbability = 0.01}</li>
     *   <li>{@code mobilityRate = 0.20}</li>
     *   <li>{@code maxInfectionDays = 14}</li>
     *   <li>{@code meanImmunity = 0.0}</li>
     *   <li>{@code immunityVariance = 0.0}</li>
     *   <li>{@code vaccineEfficacy = 0.85}</li>
     * </ul>
     */
    public SimulationSettings() {
        this.simulationSpeed = 1;
        this.transmissionProbability = 0.30;
        this.recoveryProbability = 0.05;
        this.mortalityProbability = 0.01;
        this.mobilityRate = 0.20;
        this.maxInfectionDays = 14;
        this.meanImmunity = 0.0;
        this.immunityVariance = 0.0;
        this.vaccineEfficacy = 0.85;
    }

    /**
     * @return the simulation speed, in ticks per second
     */
    public int getSimulationSpeed() {
        return simulationSpeed;
    }

    /**
     * @return the probability of transmission to a susceptible neighbor
     *         per tick
     */
    public double getTransmissionProbability() {
        return transmissionProbability;
    }

    /**
     * @return the probability of natural recovery per tick
     */
    public double getRecoveryProbability() {
        return recoveryProbability;
    }

    /**
     * @return the probability of dying from the disease per tick
     */
    public double getMortalityProbability() {
        return mortalityProbability;
    }

    /**
     * @return the probability of attempting a move per tick
     */
    public double getMobilityRate() {
        return mobilityRate;
    }

    /**
     * @return the maximum number of ticks a person stays infected before
     *         being forcibly recovered
     */
    public int getMaxInfectionDays() {
        return maxInfectionDays;
    }

    /**
     * Sets the simulation speed.
     *
     * @param simulationSpeed the number of ticks per second; must be
     *                        greater than or equal to {@code 1}
     * @throws IllegalArgumentException if {@code simulationSpeed < 1}
     */
    public void setSimulationSpeed(int simulationSpeed) {
        if (simulationSpeed < 1) {
            throw new IllegalArgumentException(
                    "simulationSpeed must be >= 1, got " + simulationSpeed);
        }
        this.simulationSpeed = simulationSpeed;
    }

    /**
     * Sets the transmission probability.
     *
     * @param transmissionProbability the probability, in {@code [0.0, 1.0]}
     * @throws IllegalArgumentException if the value is outside
     *                                  {@code [0.0, 1.0]}
     */
    public void setTransmissionProbability(double transmissionProbability) {
        if (transmissionProbability < 0.0 || transmissionProbability > 1.0) {
            throw new IllegalArgumentException(
                    "transmissionProbability must be in [0.0, 1.0], got "
                            + transmissionProbability);
        }
        this.transmissionProbability = transmissionProbability;
    }

    /**
     * Sets the recovery probability.
     *
     * @param recoveryProbability the probability, in {@code [0.0, 1.0]}
     * @throws IllegalArgumentException if the value is outside
     *                                  {@code [0.0, 1.0]}
     */
    public void setRecoveryProbability(double recoveryProbability) {
        if (recoveryProbability < 0.0 || recoveryProbability > 1.0) {
            throw new IllegalArgumentException(
                    "recoveryProbability must be in [0.0, 1.0], got "
                            + recoveryProbability);
        }
        this.recoveryProbability = recoveryProbability;
    }

    /**
     * Sets the mortality probability.
     *
     * @param mortalityProbability the probability, in {@code [0.0, 1.0]}
     * @throws IllegalArgumentException if the value is outside
     *                                  {@code [0.0, 1.0]}
     */
    public void setMortalityProbability(double mortalityProbability) {
        if (mortalityProbability < 0.0 || mortalityProbability > 1.0) {
            throw new IllegalArgumentException(
                    "mortalityProbability must be in [0.0, 1.0], got "
                            + mortalityProbability);
        }
        this.mortalityProbability = mortalityProbability;
    }

    /**
     * Sets the mobility rate.
     *
     * @param mobilityRate the probability, in {@code [0.0, 1.0]}, of
     *                     attempting a move per tick
     * @throws IllegalArgumentException if the value is outside
     *                                  {@code [0.0, 1.0]}
     */
    public void setMobilityRate(double mobilityRate) {
        if (mobilityRate < 0.0 || mobilityRate > 1.0) {
            throw new IllegalArgumentException(
                    "mobilityRate must be in [0.0, 1.0], got " + mobilityRate);
        }
        this.mobilityRate = mobilityRate;
    }

    /**
     * Sets the maximum infection duration.
     *
     * @param maxInfectionDays the cap, in ticks; must be greater than or
     *                         equal to {@code 1}
     * @throws IllegalArgumentException if {@code maxInfectionDays < 1}
     */
    public void setMaxInfectionDays(int maxInfectionDays) {
        if (maxInfectionDays < 1) {
            throw new IllegalArgumentException(
                    "maxInfectionDays must be >= 1, got " + maxInfectionDays);
        }
        this.maxInfectionDays = maxInfectionDays;
    }

    /**
     * @return the mean immunity factor drawn for each new person
     */
    public double getMeanImmunity() {
        return meanImmunity;
    }

    /**
     * Sets the mean immunity factor used when drawing the immunity of each
     * new person.
     *
     * @param meanImmunity the mean immunity, in {@code [0.0, 1.0]}
     * @throws IllegalArgumentException if the value is outside
     *                                  {@code [0.0, 1.0]}
     */
    public void setMeanImmunity(double meanImmunity) {
        if (meanImmunity < 0.0 || meanImmunity > 1.0) {
            throw new IllegalArgumentException(
                    "meanImmunity must be in [0.0, 1.0], got " + meanImmunity);
        }
        this.meanImmunity = meanImmunity;
    }

    /**
     * @return the spread of the truncated-normal distribution used to draw
     *         each immunity factor
     */
    public double getImmunityVariance() {
        return immunityVariance;
    }

    /**
     * Sets the spread of the truncated-normal distribution used to draw each
     * immunity factor.
     *
     * <p>The value is capped at {@code 0.5} because beyond that the
     * truncated normal collapses against the {@code [0.0, 1.0]} bounds and
     * loses its meaning.
     *
     * @param immunityVariance the spread, in {@code [0.0, 0.5]}
     * @throws IllegalArgumentException if the value is outside
     *                                  {@code [0.0, 0.5]}
     */
    public void setImmunityVariance(double immunityVariance) {
        if (immunityVariance < 0.0 || immunityVariance > 0.5) {
            throw new IllegalArgumentException(
                    "immunityVariance must be in [0.0, 0.5], got " + immunityVariance);
        }
        this.immunityVariance = immunityVariance;
    }

    /**
     * @return the minimum immunity granted to a vaccinated person
     */
    public double getVaccineEfficacy() {
        return vaccineEfficacy;
    }

    /**
     * Sets the vaccine efficacy, i.e. the minimum immunity granted to a
     * vaccinated person.
     *
     * @param vaccineEfficacy the efficacy, in {@code [0.0, 1.0]}
     * @throws IllegalArgumentException if the value is outside
     *                                  {@code [0.0, 1.0]}
     */
    public void setVaccineEfficacy(double vaccineEfficacy) {
        if (vaccineEfficacy < 0.0 || vaccineEfficacy > 1.0) {
            throw new IllegalArgumentException(
                    "vaccineEfficacy must be in [0.0, 1.0], got " + vaccineEfficacy);
        }
        this.vaccineEfficacy = vaccineEfficacy;
    }
}
