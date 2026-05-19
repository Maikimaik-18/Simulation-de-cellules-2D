/**
 * Live statistics collection and history.
 *
 * <p>Implements the Observer pattern: a {@code StatisticsService} subscribes
 * to the simulation engine and records a {@code Statistics} snapshot after
 * every tick. The history then feeds the live charts in the UI.
 */
package com.cellsimulation.stats;
