package com.cellsimulation.neighborhood;

import java.io.Serializable;
import java.util.List;

import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Position;

/**
 * Contract for a neighborhood computation strategy (Strategy pattern).
 *
 * <p>An implementation defines which cells around a given position are
 * considered its neighbors on the simulation grid. Four implementations are
 * planned: orthogonal (4-neighbors), Moore (8-neighbors), Euclidean
 * (distance-based) and Manhattan (taxicab distance).
 *
 * <p>Implementations must be stateless (or carry only immutable primitive
 * state, such as a radius) and safe to share across simulation ticks.
 *
 * <p>The interface extends {@link Serializable} so that the currently
 * selected strategy can be persisted with the rest of the simulation state
 * by the {@code SaveService}. Implementations therefore must not hold any
 * non-serializable field.
 */
public interface NeighborhoodStrategy extends Serializable {

    /**
     * Returns the positions of the neighbors of the given position on the
     * provided grid, according to this strategy.
     *
     * @param grid     the grid on which the lookup is performed
     * @param position the position whose neighbors are requested
     * @return the list of neighbor positions; never {@code null}, possibly
     *         empty if the position has no valid neighbor
     */
    List<Position> getNeighbors(Grid grid, Position position);

    /**
     * Returns the human-readable name of this strategy, suitable for display
     * in the UI selector.
     *
     * @return the strategy name
     */
    String getName();
}
