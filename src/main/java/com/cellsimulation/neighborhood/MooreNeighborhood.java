package com.cellsimulation.neighborhood;

import java.util.ArrayList;
import java.util.List;

import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Position;

/**
 * Eight-neighbors (Moore) implementation of {@link NeighborhoodStrategy}.
 *
 * <p>For a given position the strategy returns the eight surrounding cells:
 * the four orthogonal neighbors plus the four diagonals.
 *
 * <p>The strategy is stateless and safe to share across simulation ticks.
 * In toric mode coordinates wrap around the grid edges using
 * {@link Math#floorMod(int, int)}; in bounded mode positions falling
 * outside the grid are simply skipped. On very small toric grids the result
 * may contain duplicate positions, which is intentional.
 */
public class MooreNeighborhood implements NeighborhoodStrategy {

    private static final long serialVersionUID = 1L;

    /**
     * Returns the eight Moore neighbors of the given position.
     *
     * @param grid     the grid on which the lookup is performed
     * @param position the position whose neighbors are requested
     * @return the list of neighbor positions; never {@code null}, possibly
     *         empty if the position is isolated in bounded mode
     */
    @Override
    public List<Position> getNeighbors(Grid grid, Position position) {
        List<Position> neighbors = new ArrayList<>(8);
        int height = grid.getHeight();
        int width = grid.getWidth();
        boolean toric = grid.isToricMode();
        for (int dRow = -1; dRow <= 1; dRow++) {
            for (int dCol = -1; dCol <= 1; dCol++) {
                if (dRow == 0 && dCol == 0) {
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
     * @return the human-readable name of this strategy, {@code "Moore"}
     */
    @Override
    public String getName() {
        return "Moore";
    }
}
