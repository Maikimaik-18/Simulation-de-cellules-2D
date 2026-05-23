package com.cellsimulation.neighborhood;

import java.util.ArrayList;
import java.util.List;

import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Position;

/**
 * Diamond-shaped implementation of {@link NeighborhoodStrategy} using the
 * Manhattan (taxicab) distance.
 *
 * <p>For a given position the strategy returns every cell whose Manhattan
 * distance ({@code |dRow| + |dCol|}) to the reference is less than or
 * equal to a configurable {@code distance}. The reference position itself
 * is excluded.
 *
 * <p>The strategy is stateless apart from the immutable {@code distance}
 * and is safe to share across simulation ticks. In toric mode coordinates
 * wrap around the grid edges using {@link Math#floorMod(int, int)}; in
 * bounded mode positions falling outside the grid are simply skipped. The
 * result may contain duplicate positions when the distance is large
 * compared to the grid dimensions in toric mode, which is intentional.
 */
public class ManhattanNeighborhood implements NeighborhoodStrategy {

    private static final long serialVersionUID = 1L;

    private final int distance;

    /**
     * Creates a new Manhattan neighborhood with the given distance.
     *
     * @param distance the maximum Manhattan distance, in cells; must be
     *                 greater than or equal to {@code 1}
     * @throws IllegalArgumentException if {@code distance < 1}
     */
    public ManhattanNeighborhood(int distance) {
        if (distance < 1) {
            throw new IllegalArgumentException(
                    "distance must be >= 1, got " + distance);
        }
        this.distance = distance;
    }

    /**
     * @return the maximum Manhattan distance of this neighborhood, in cells
     */
    public int getDistance() {
        return distance;
    }

    /**
     * Returns the neighbors of the given position lying within the
     * Manhattan distance.
     *
     * @param grid     the grid on which the lookup is performed
     * @param position the position whose neighbors are requested
     * @return the list of neighbor positions; never {@code null}, possibly
     *         empty if the position has no valid neighbor in bounded mode
     */
    @Override
    public List<Position> getNeighbors(Grid grid, Position position) {
        List<Position> neighbors = new ArrayList<>();
        int height = grid.getHeight();
        int width = grid.getWidth();
        boolean toric = grid.isToricMode();
        for (int dRow = -distance; dRow <= distance; dRow++) {
            for (int dCol = -distance; dCol <= distance; dCol++) {
                if (dRow == 0 && dCol == 0) {
                    continue;
                }
                if (Math.abs(dRow) + Math.abs(dCol) > distance) {
                    continue;
                }
                int rawRow = position.getRow() + dRow;
                int rawCol = position.getColumn() + dCol;
                if (toric) {
                    int row = Math.floorMod(rawRow, height);
                    int column = Math.floorMod(rawCol, width);
                    neighbors.add(new Position(row, column));
                } else if (rawRow >= 0 && rawRow < height
                        && rawCol >= 0 && rawCol < width) {
                    neighbors.add(new Position(rawRow, rawCol));
                }
            }
        }
        return neighbors;
    }

    /**
     * @return the human-readable name of this strategy, including the
     *         distance (for example {@code "Manhattan (d=3)"})
     */
    @Override
    public String getName() {
        return "Manhattan (d=" + distance + ")";
    }
}
