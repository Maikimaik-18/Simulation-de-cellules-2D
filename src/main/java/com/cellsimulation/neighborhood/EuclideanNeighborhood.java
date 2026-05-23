package com.cellsimulation.neighborhood;

import java.util.ArrayList;
import java.util.List;

import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Position;

/**
 * Disc-shaped implementation of {@link NeighborhoodStrategy} using the
 * Euclidean distance.
 *
 * <p>For a given position the strategy returns every cell whose Euclidean
 * distance to the reference is less than or equal to a configurable
 * {@code radius}. The reference position itself is excluded.
 *
 * <p>The strategy is stateless apart from the immutable {@code radius} and
 * is safe to share across simulation ticks. In toric mode coordinates wrap
 * around the grid edges using {@link Math#floorMod(int, int)}; in bounded
 * mode positions falling outside the grid are simply skipped. The result
 * may contain duplicate positions when the radius is large compared to the
 * grid dimensions in toric mode, which is intentional.
 */
public class EuclideanNeighborhood implements NeighborhoodStrategy {

    private static final long serialVersionUID = 1L;

    private final int radius;

    /**
     * Creates a new Euclidean neighborhood with the given radius.
     *
     * @param radius the maximum Euclidean distance, in cells; must be
     *               greater than or equal to {@code 1}
     * @throws IllegalArgumentException if {@code radius < 1}
     */
    public EuclideanNeighborhood(int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException(
                    "radius must be >= 1, got " + radius);
        }
        this.radius = radius;
    }

    /**
     * @return the radius of this neighborhood, in cells
     */
    public int getRadius() {
        return radius;
    }

    /**
     * Returns the neighbors of the given position lying within the
     * Euclidean radius.
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
        long radiusSquared = (long) radius * radius;
        for (int dRow = -radius; dRow <= radius; dRow++) {
            for (int dCol = -radius; dCol <= radius; dCol++) {
                if (dRow == 0 && dCol == 0) {
                    continue;
                }
                long distanceSquared = (long) dRow * dRow + (long) dCol * dCol;
                if (distanceSquared > radiusSquared) {
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
     *         radius (for example {@code "Euclidean (r=3)"})
     */
    @Override
    public String getName() {
        return "Euclidean (r=" + radius + ")";
    }
}
