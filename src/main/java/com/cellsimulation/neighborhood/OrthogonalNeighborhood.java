package com.cellsimulation.neighborhood;

import java.util.ArrayList;
import java.util.List;

import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Position;

/**
 * Four-neighbors (von Neumann) implementation of
 * {@link NeighborhoodStrategy}.
 *
 * <p>For a given position the strategy returns the cells immediately above,
 * below, to the left and to the right. Diagonals are not included.
 *
 * <p>The strategy is stateless and safe to share across simulation ticks.
 * In toric mode coordinates wrap around the grid edges using
 * {@link Math#floorMod(int, int)}; in bounded mode positions falling
 * outside the grid are simply skipped. On very small toric grids the result
 * may contain duplicate positions, which is intentional and matches the
 * behavior expected by the simulation engine.
 */
public class OrthogonalNeighborhood implements NeighborhoodStrategy {

    private static final long serialVersionUID = 1L;

    private static final int[][] OFFSETS = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
    };

    /**
     * Returns the four orthogonal neighbors of the given position.
     *
     * @param grid     the grid on which the lookup is performed
     * @param position the position whose neighbors are requested
     * @return the list of neighbor positions; never {@code null}, possibly
     *         empty if the position is isolated in bounded mode
     */
    @Override
    public List<Position> getNeighbors(Grid grid, Position position) {
        List<Position> neighbors = new ArrayList<>(OFFSETS.length);
        int height = grid.getHeight();
        int width = grid.getWidth();
        for (int[] offset : OFFSETS) {
            int rawRow = position.getRow() + offset[0];
            int rawCol = position.getColumn() + offset[1];
            if (grid.isToricMode()) {
                int row = Math.floorMod(rawRow, height);
                int column = Math.floorMod(rawCol, width);
                neighbors.add(new Position(row, column));
            } else if (rawRow >= 0 && rawRow < height
                    && rawCol >= 0 && rawCol < width) {
                neighbors.add(new Position(rawRow, rawCol));
            }
        }
        return neighbors;
    }

    /**
     * @return the human-readable name of this strategy, {@code "Orthogonal"}
     */
    @Override
    public String getName() {
        return "Orthogonal";
    }
}
